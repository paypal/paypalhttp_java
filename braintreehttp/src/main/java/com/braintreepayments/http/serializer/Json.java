package com.braintreepayments.http.serializer;

import com.braintreepayments.http.exceptions.JsonParseException;
import com.braintreepayments.http.exceptions.JsonSerializeException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class Json implements Serializer {

	private static final char OBJECT_TOKEN_OPEN = '{';
	private static final char OBJECT_TOKEN_CLOSE = '}';
	private static final char LIST_TOKEN_OPEN = '[';
	private static final char LIST_TOKEN_CLOSE = ']';
	private static final char KEY_DELIMITER = ':';
	private static final char PAIR_DELIMITER = ',';
	private static final char KEY_BARRIER = '"';

	@Override
	public String contentType() {
		return "application/json";
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T deserialize(String source, Class<T> cls) throws IOException {
		if (cls.isAssignableFrom(Map.class) || cls.isAssignableFrom(List.class)) {
			return (T) deserializeInternal(source);
		} else {
			try {
				Deserializable destination = (Deserializable) cls.newInstance();
				Map<String, Object> deserialized = (Map<String, Object>) deserializeInternal(source);
				destination.deserialize(deserialized);

				return (T) destination;
			} catch (IllegalAccessException | InstantiationException e) {
				throw new UnsupportedEncodingException(e.getMessage());
			} catch (RuntimeException re) {
				throw new JsonParseException("Unable to parse Json " + re.getMessage());
			}
		}
	}

	@Override
	public String serialize(Object o) throws JsonSerializeException {
		if (o instanceof Serializable) {
			Map<String, Object> map = new HashMap<>();
			((Serializable) o).serialize(map);
			return jsonValueStringFor(map);
		} else {
			return jsonValueStringFor(o);
		}
	}

	private String serializeObjectInternal(Map<String, Object> map) throws JsonSerializeException {
		StringBuilder builder = new StringBuilder();

		builder.append(OBJECT_TOKEN_OPEN);

		boolean hasContents = false;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String)) {
				throw new JsonSerializeException("Map key must be of class String");
			}

			builder.append(String.format("\"%s\":", (String) entry.getKey()));
			builder.append(jsonValueStringFor(entry.getValue()));

			builder.append(',');
			hasContents = true;
		}

		if (hasContents) {
			builder.setLength(builder.length() - 1);
		}
		builder.append(OBJECT_TOKEN_CLOSE);

		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	private String jsonValueStringFor(Object obj) throws JsonSerializeException {
		StringBuilder builder = new StringBuilder();
		if (obj == null) {
			builder.append("null");
		} else if (obj instanceof String) {
			builder.append(String.format("\"%s\"", obj.toString()));
		} else if (obj instanceof Number || obj instanceof Boolean) {
			builder.append(obj.toString());
		} else if (obj instanceof Object[] || obj instanceof Collection) {
			builder.append(LIST_TOKEN_OPEN);
			boolean hasContents = false;

			if (obj instanceof Object[]) {
				Object[] contents = (Object[]) obj;
				for (Object o : contents) {
					builder.append(jsonValueStringFor(o));
					builder.append(',');
					hasContents = true;
				}
			} else {
				Collection contents = (Collection) obj;
				for (Object o : contents) {
					builder.append(jsonValueStringFor(o));
					builder.append(PAIR_DELIMITER);
					hasContents = true;
				}
			}

			if (hasContents) {
				builder.setLength(builder.length() - 1);
			}

			builder.append(LIST_TOKEN_CLOSE);
		} else if (obj instanceof Serializable) {
			builder.append(serialize((Serializable) obj));
		} else if (obj instanceof Map) {
			builder.append(serializeObjectInternal((Map<String, Object>) obj));
		} else {
			throw new JsonSerializeException(String.format("Object of class %s could not be JSON-serialized", obj.getClass()));
		}

		return builder.toString();
	}

	private List<Object> deserializeListInternal(String json) throws JsonParseException {
		List<Object> values = new ArrayList<>();

		json = json.substring(1, json.length() - 1).trim();
		char innerDelim = json.charAt(json.length() - 1);

		String[] innerValues = json.split(new String(new char[]{innerDelim, PAIR_DELIMITER}));
		if (innerValues.length > 0) {
			int delimCount = charCount(json, innerDelim);
			if (innerDelim == KEY_BARRIER) {
				delimCount /= 2;
			}
			if (delimCount -1 > charCount(json, PAIR_DELIMITER)) {
				throw new JsonParseException("Missing list delimiter " + json);
			}
		}
		for (String innerValue : innerValues) {
			innerValue = innerValue.trim();
			if (innerValue.charAt(innerValue.length() - 1) != innerDelim) {
				innerValue += innerDelim;
			}
			if (innerDelim == OBJECT_TOKEN_CLOSE || innerDelim == LIST_TOKEN_CLOSE) {
				values.add(deserializeInternal(innerValue));
			} else {
				values.add(deserializeSimple(innerValue));
			}
		}

		return values;
	}

	private Object deserializeInternal(String json) throws JsonParseException {
		json = json.trim();

		if (json.length() == 0) {
			throw new JsonParseException("Cannot parse empty string as json");
		}

		char startingToken = json.charAt(0);

		if (opposingToken(startingToken) != json.charAt(json.length() - 1)) {
			throw new JsonParseException("Invalid end token " + json.charAt(0));
		}

		if (startingToken == OBJECT_TOKEN_OPEN) {
			return deserializeObjectInternal(json);
		} else if (startingToken == LIST_TOKEN_OPEN) {
			return deserializeListInternal(json);
		}

		throw new JsonParseException("Invalid starting token " + startingToken);
	}

    private Map<String, Object> deserializeObjectInternal(String json) throws JsonParseException {
		Map<String, Object> deserialized = new HashMap<>();

		char[] raw = json.toCharArray();
		for(int i = 0; i < raw.length;) {
			while(raw[i] != OBJECT_TOKEN_OPEN && raw[i] != KEY_BARRIER) {
				i++;
			}

			if (raw[i] == OBJECT_TOKEN_OPEN) {
				i = advanceTo(raw, i, KEY_BARRIER);
			}

			SearchResult keyResult = extractKey(raw, i);
			String key = keyResult.token;
			i = keyResult.endIndex;

			switch(raw[i]) {
				case OBJECT_TOKEN_OPEN: {
					SearchResult result = extractNextToken(raw, i);
					i = result.endIndex;
					deserialized.put(key, deserializeInternal(result.token));
					break;
				}
				case LIST_TOKEN_OPEN: {
					SearchResult result = extractNextToken(raw, i);
					deserialized.put(key, deserializeListInternal(result.token));
					i = result.endIndex;

					break;
				}
				default: {
					// Search to end of value
					StringBuilder value = new StringBuilder();
					while (raw[i] != PAIR_DELIMITER && raw[i] != OBJECT_TOKEN_CLOSE) {
						value.append(raw[i]);
						i++;
					}
					deserialized.put(key, deserializeSimple(value.toString().trim()));
					break;
				}
			}

			// Advance to comma
			while(i < raw.length && raw[i] != PAIR_DELIMITER) {
				i++;
			}
		}


		return deserialized;
    }

	private Object deserializeSimple(String s) throws JsonParseException {
		if (s.equals("null")) {
			return null;
		} else if (s.startsWith("\"")) {
			return s.substring(1, s.length() -1);
		} else if (s.contains(".")) {
			return Double.parseDouble(s);
		} else if (s.equals("true") || s.equals("false")) {
			return Boolean.parseBoolean(s);
		} else if (Character.isDigit(s.charAt(0))) {
			return Integer.parseInt(s);
		} else {
			throw new JsonParseException("Invalid value " + s);
		}
	}

	private int charCount(String s, char search) {
		char[] raw = s.toCharArray();
		int count = 0;
		for (char c : raw) {
			if (c == search) {
				count++;
			}
		}

		return count;
	}

	private int advanceTo(char[] raw, int i, char search) {
		while(raw[i] != search) {
			i++;
		}

		return i;
	}

	private int consumeWhitespace(char[] raw, int i) {
		while(Character.isWhitespace(raw[i])) {
			i++;
		}

		return i;
	}

	private SearchResult extractKey(char[] raw, int i) throws JsonParseException {
		i = consumeWhitespace(raw, i);

		if (raw[i] == KEY_BARRIER) {
			i++;
		} else {
			throw new JsonParseException("Malformed json - missing key barrier");
		}

		StringBuilder keyName = new StringBuilder();
		while (raw[i] != KEY_BARRIER) {
			keyName.append(raw[i]);
			i++;
		}

		if (raw[i] == KEY_BARRIER) {
			i++;
		} else {
			throw new JsonParseException("Malformed json - missing key barrier");
		}

		i = consumeWhitespace(raw, i);

		if (raw[i] != KEY_DELIMITER) {
			throw new JsonParseException("Malformed json - missing pair delimiter");
		}
		i++;

		i = consumeWhitespace(raw, i);

		return new SearchResult(i, keyName.toString());
	}

	private SearchResult extractNextToken(char[] s, int starting) {
		char startToken = s[starting];
		char searchToken = opposingToken(s[starting]);
		int innerCount = 0;
		StringBuilder value = new StringBuilder();
		while (true) {
			value.append(s[starting]);
			if (s[starting] == startToken) {
				innerCount++;
			} else if (s[starting] == searchToken) {
				innerCount--;
				if (innerCount == 0) {
					break;
				}
			}

			starting++;
		}

		return new SearchResult(starting, value.toString());
	}

	private char opposingToken(char token) {
		switch (token) {
			case OBJECT_TOKEN_OPEN:
				return OBJECT_TOKEN_CLOSE;
			case OBJECT_TOKEN_CLOSE:
				return OBJECT_TOKEN_OPEN;
			case LIST_TOKEN_OPEN:
				return LIST_TOKEN_CLOSE;
			case LIST_TOKEN_CLOSE:
				return LIST_TOKEN_OPEN;
			case KEY_BARRIER:
				return KEY_BARRIER;
		}

		return ' ';
	}

	private static class SearchResult {
		private int endIndex;
		private String token;

		private SearchResult(int endIndex, String token) {
			this.endIndex = endIndex;
			this.token = token;
		}
	}
}
