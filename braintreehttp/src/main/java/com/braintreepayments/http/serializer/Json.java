package com.braintreepayments.http.serializer;

import com.braintreepayments.http.HttpRequest;
import com.braintreepayments.http.exceptions.JsonParseException;
import com.braintreepayments.http.exceptions.SerializeException;

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
		return "^application\\/json$";
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
	public byte[] serialize(HttpRequest request) throws SerializeException {
		return serialize(request.requestBody()).getBytes();
	}

	public String serialize(Object o) throws SerializeException {
		if (o instanceof Serializable) {
			Map<String, Object> map = new HashMap<>();
			((Serializable) o).serialize(map);
			return jsonValueStringFor(map);
		} else {
			return jsonValueStringFor(o);
		}
	}

	private String serializeObjectInternal(Map<String, Object> map) throws SerializeException {
		StringBuilder builder = new StringBuilder();

		builder.append(OBJECT_TOKEN_OPEN);

		boolean hasContents = false;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String)) {
				throw new SerializeException("Map key must be of class String");
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
	private String jsonValueStringFor(Object obj) throws SerializeException {
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
			builder.append(serialize(obj));
		} else if (obj instanceof Map) {
			builder.append(serializeObjectInternal((Map<String, Object>) obj));
		} else {
			throw new SerializeException(String.format("Object of class %s could not be JSON-serialized", obj.getClass()));
		}

		return builder.toString();
	}

	private List<Object> deserializeListInternal(String json) throws JsonParseException {
		List<Object> values = new ArrayList<>();
		if (json.length() == 2) {
			return values;
		}

		List<String> innerValues = splitJsonArray(json.trim());
		for (String innerValue : innerValues) {
			char innerDelim = innerValue.charAt(0);
			if (innerDelim == OBJECT_TOKEN_OPEN || innerDelim == LIST_TOKEN_OPEN) {
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
				if (raw[i + 1] == OBJECT_TOKEN_CLOSE) {
					return deserialized;
				} else {
					i = advanceTo(raw, i, KEY_BARRIER);
				}
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
					SearchResult result = extractNextToken(raw, i);
					deserialized.put(key, deserializeSimple(result.token));
					i = result.endIndex;
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
		s = s.trim();

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

	private List<String> splitJsonArray(String s) throws JsonParseException {
		List<String> split = new ArrayList<>();
		char[] chars = s.toCharArray();
		for (int i = 1; i < chars.length; i++) {
			i = consumeWhitespace(chars, i);
			SearchResult result = extractNextToken(chars, i);
			i = result.endIndex;
			split.add(result.token);

			i = consumeWhitespace(chars, i);
			if (chars[i] != LIST_TOKEN_CLOSE && chars[i] != PAIR_DELIMITER) {
				throw new JsonParseException("Invalid json array delimiter " + chars[i]);
			}

			if (chars[i] == LIST_TOKEN_CLOSE) {
				break;
			}
		}

		return split;
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

	private SearchResult extractNextToken(char[] s, int i) {
		switch (s[i]) {
			case OBJECT_TOKEN_OPEN:
			case LIST_TOKEN_OPEN:
				return extractNextObjectToken(s, i);
			case KEY_BARRIER:
				return extractNextStringToken(s, i);
			default:
				return extractNextValueToken(s, i);
		}
	}

	private SearchResult extractNextObjectToken(char[] s, int i) {
		int startIndex = i;

		char startToken = s[i];
		char searchToken = opposingToken(s[i]);
		int innerCount = 0;

		innerCount++;
		i++;

		if (!matchesOpposing(s[i], searchToken)) {
			do {
				i++;
				if (i >= s.length) {
					break;
				} else if (isBoundaryChar(startToken) && s[i] == startToken) {
					innerCount++;
				} else if (matchesOpposing(s[i], searchToken)) {
					innerCount--;
					if (innerCount == 0) {
						i++;
						break;
					}
				}
			} while (i < s.length);
		} else if (matchesOpposing(s[i], searchToken)) {
			i++;
		}

		String val = new String(s, startIndex, i - startIndex);
		return new SearchResult(i, val);
	}

	private SearchResult extractNextStringToken(char[] s, int i) {
		int startIndex = i;

		if (s[i+1] == KEY_BARRIER) {
			i += 2;
		} else {
			do {
				i++;
			} while(s[i] != KEY_BARRIER);
			i++;
		}

		String val = new String(s, startIndex, i - startIndex);
		return new SearchResult(i, val);
	}

	private SearchResult extractNextValueToken(char[] s, int i) {
		int startIndex = i;

		while (s[i] != PAIR_DELIMITER && s[i] != OBJECT_TOKEN_CLOSE && s[i] != LIST_TOKEN_CLOSE) {
			i++;
		}

		String val = new String(s, startIndex, i - startIndex);
		return new SearchResult(i, val);
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

		return PAIR_DELIMITER;
	}

	private boolean isBoundaryChar(char token) {
		switch (token) {
			case OBJECT_TOKEN_CLOSE:
			case OBJECT_TOKEN_OPEN:
			case LIST_TOKEN_CLOSE:
			case LIST_TOKEN_OPEN:
			case PAIR_DELIMITER:
			case KEY_BARRIER:
			case KEY_DELIMITER:
				return true;
		}
		return false;
	}

	private boolean matchesOpposing(char search, char token) {
		if (search == token) {
			return true;
		} else if (token == PAIR_DELIMITER) {
			return search == OBJECT_TOKEN_CLOSE || search == LIST_TOKEN_CLOSE || search == PAIR_DELIMITER;
		}

		return false;
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
