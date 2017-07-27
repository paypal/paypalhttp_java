package com.braintreepayments.http;

import com.braintreepayments.http.serializer.Deserializable;
import com.braintreepayments.http.serializer.Json;
import com.braintreepayments.http.serializer.Serializable;
import com.braintreepayments.http.serializer.Serializer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Encoder {

	private Map<String, Serializer> serializers = new HashMap<>();

	public void registerSerializer(Serializer serializer) {
		serializers.put(serializer.contentType(), serializer);
	}

	public Encoder() {
		registerSerializer(new Json());
	}

	public String encode(HttpRequest request) throws IOException {
		Serializer serializer = serializers.get(request.headers().header(Headers.CONTENT_TYPE));
		if (serializer == null) {
			throw new UnsupportedEncodingException(String.format("Unable to serialize request with Content-Type: %s. Supported encodings are: %s", request.headers().header(Headers.CONTENT_TYPE), supportedEncodings()));
		} else if (!(request.body() instanceof Serializable)) {
			throw new UnsupportedEncodingException(String.format("Body class %s must implement Serializable", request.body().getClass().getSimpleName()));
		}

		return serializer.serialize((Serializable) request.body());
	}

	public <T> T decode(String responseBody, Class<T> responseClass, Headers headers) throws IOException {
		Serializer deserializer = serializers.get(headers.header(Headers.CONTENT_TYPE));
		if (deserializer == null) {
			throw new UnsupportedEncodingException(String.format("Unable to deserialize response with Content-Type: %s. Supported decodings are: %s", headers.header(Headers.CONTENT_TYPE), supportedDecodings()));
		} else if (responseClass.isAssignableFrom(Deserializable.class)) {
			throw new UnsupportedEncodingException(String.format("Destination class %s must implement Deserializable", responseClass.getSimpleName()));
		}

		return deserializer.deserialize(responseBody, responseClass);
	}

	private List<String> supportedDecodings() {
		List<String> supportedEncodings = new ArrayList<>();
		for (String encoding : serializers.keySet()) {
			supportedEncodings.add(encoding);
		}

		return supportedEncodings;
	}

	private List<String> supportedEncodings() {
		List<String> supportedEncodings = new ArrayList<>();
		for (String encoding : serializers.keySet()) {
			supportedEncodings.add(encoding);
		}

		return supportedEncodings;
	}
}
