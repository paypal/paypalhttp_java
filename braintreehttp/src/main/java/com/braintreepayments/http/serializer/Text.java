package com.braintreepayments.http.serializer;

import com.braintreepayments.http.exceptions.JsonSerializeException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Text implements Serializer {

	@Override
	public String contentType() {
		return "^text\\/.*";
	}

	@Override
	public String serialize(Object o) throws JsonSerializeException {
		return o.toString();
	}

	@Override
	public <T> T deserialize(String source, Class<T> cls) throws IOException {
		if (!cls.isAssignableFrom(String.class)) {
			throw new UnsupportedEncodingException("Text class unable to return types other than String");
		}

		return (T) source;
	}
}
