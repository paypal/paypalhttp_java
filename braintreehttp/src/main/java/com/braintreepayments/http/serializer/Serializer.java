package com.braintreepayments.http.serializer;

import com.braintreepayments.http.HttpRequest;

import java.io.IOException;

public interface Serializer {
	String contentType();
	byte[] encode(HttpRequest request) throws IOException;
	<T> T decode(String source, Class<T> cls) throws IOException;
}
