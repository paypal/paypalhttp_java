package com.paypal.http.serializer;

import com.paypal.http.HttpRequest;
import com.paypal.http.exceptions.SerializeException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FormEncoded implements Serializer {

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

		return String.join("&", parts).getBytes(UTF_8);
	}

	@Override
	public <T> T decode(String source, Class<T> cls) throws IOException {
		throw new UnsupportedEncodingException("Unable to decode Content-Type: " + contentType());
	}

	public static String urlEscape(String input) {
		try {
			return URLEncoder.encode(input, "UTF8");
		} catch (UnsupportedEncodingException ignored) {}

		return null;
	}
}
