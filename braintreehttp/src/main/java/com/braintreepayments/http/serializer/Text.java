package com.braintreepayments.http.serializer;

import com.braintreepayments.http.HttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Text implements Serializer {

	@Override
	public String contentType() {
		return "^text\\/.*";
	}

	@Override
	public byte[] serialize(HttpRequest request) throws IOException {
		if (request.requestBody() instanceof String) {
			return ((String) request.requestBody()).getBytes();
		} else {
			return request.requestBody().toString().getBytes();
		}
	}

	@Override
	public <T> T deserialize(String source, Class<T> cls) throws IOException {
		if (!cls.isAssignableFrom(String.class)) {
			throw new UnsupportedEncodingException("Text class unable to return types other than String");
		}

		return (T) source;
	}
}
