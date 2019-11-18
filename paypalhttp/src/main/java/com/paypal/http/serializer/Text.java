package com.paypal.http.serializer;

import com.paypal.http.HttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Text implements Serializer {

	@Override
	public String contentType() {
		return "^text\\/.*";
	}

	@Override
	public byte[] encode(HttpRequest request) throws IOException {
		if (request.requestBody() instanceof String) {
			return ((String) request.requestBody()).getBytes(UTF_8);
		} else {
			return request.requestBody().toString().getBytes(UTF_8);
		}
	}

	@Override
	public <T> T decode(String source, Class<T> cls) throws IOException {
		if (!cls.isAssignableFrom(String.class)) {
			throw new UnsupportedEncodingException("Text class unable to return types other than String");
		}

		return (T) source;
	}
}
