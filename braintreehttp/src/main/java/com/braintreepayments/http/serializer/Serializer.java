package com.braintreepayments.http.serializer;

import com.braintreepayments.http.exceptions.JsonSerializeException;

import java.io.IOException;

public interface Serializer {
	String contentType();
	String serialize(Object o) throws JsonSerializeException;
	<T> T deserialize(String source, Class<T> cls) throws IOException;
}
