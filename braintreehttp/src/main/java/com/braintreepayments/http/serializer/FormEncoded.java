package com.braintreepayments.http.serializer;

import com.braintreepayments.http.HttpRequest;
import com.braintreepayments.http.exceptions.SerializeException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class FormEncoded implements Serializer {

	private static String safeRegex = "[A-Za-z]";

	@Override
	public String contentType() {
		return "^application/x-www-form-urlencoded";
	}

	@Override
	@SuppressWarnings("unchecked")
	public byte[] encode(HttpRequest request) throws IOException {
		if (!(request.requestBody() instanceof Map)) {
			throw new SerializeException("Request requestBody must be Map<String, String> when Content-Type is application/x-www-form-urlencoded");
		}

		Map<String, String> body = (Map<String, String>) request.requestBody();

		List<String> parts = new ArrayList<>(body.size());
		for (String key : body.keySet()) {
			parts.add(key + "=" + urlEscape(body.get(key)));
		}

		return String.join("&", parts).getBytes();
	}

	@Override
	public <T> T decode(String source, Class<T> cls) throws IOException {
		throw new UnsupportedEncodingException("Unable to decode Content-Type: " + contentType());
	}

	public static String urlEscape(String input) {
		StringBuilder res = new StringBuilder();
		char[] charArray = input.toCharArray();

		String currentChar = "";
		for (int i = 0; i < charArray.length; i ++) {
			char c = charArray[i];
			currentChar = (currentChar + c).substring(i > 0 ? 1 : 0);

			if (!currentChar.matches(safeRegex)) {
				res.append('%');
				res.append(toHex(c / 16));
				res.append(toHex(c % 16));
			} else {
				res.append(c);
			}
		}

		return res.toString();
	}

	private static char toHex(int ch) {
		return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
	}
}
