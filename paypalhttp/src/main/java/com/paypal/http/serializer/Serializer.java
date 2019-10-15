package com.paypal.http.serializer;

import com.paypal.http.HttpRequest;

import java.io.IOException;

public interface Serializer {
	String contentType();
	byte[] encode(HttpRequest request) throws IOException;
	<T> T decode(String source, Class<T> cls) throws IOException;
}
