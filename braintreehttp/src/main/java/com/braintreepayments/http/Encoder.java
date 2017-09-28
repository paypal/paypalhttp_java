package com.braintreepayments.http;

import com.braintreepayments.http.serializer.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Encoder {

	private List<Serializer> serializers = new ArrayList<>();

	public void registerSerializer(Serializer serializer) {
		serializers.add(serializer);
	}

	public Encoder() {
		registerSerializer(new Json());
		registerSerializer(new Text());
		registerSerializer(new Multipart());
	}

	public byte[] serializeRequest(HttpRequest request) throws IOException {
		String contentType = request.headers().header(Headers.CONTENT_TYPE);
		if (contentType != null) {
			Serializer serializer = serializer(contentType);
			if (serializer == null) {
				throw new UnsupportedEncodingException(String.format("Unable to serialize request with Content-Type: %s. Supported encodings are: %s", request.headers().header(Headers.CONTENT_TYPE), supportedEncodings()));
			} else if (request.requestBody() instanceof Serializable ||
					request.requestBody() instanceof List ||
					request.requestBody() instanceof Map ||
					request.requestBody() instanceof String) {
				return serializer.serialize(request);
			} else {
				throw new UnsupportedEncodingException(String.format("Body class %s must implement Serializable, Map, List, or String", request.responseClass().getSimpleName()));
			}
		} else {
			throw new UnsupportedEncodingException("HttpRequest does not have Content-Type header set");
		}
	}

	public <T> T deserializeResponse(String responseBody, Class<T> responseClass, Headers headers) throws IOException {
		String contentType = headers.header(Headers.CONTENT_TYPE);
		if (contentType != null) {
			Serializer serializer = serializer(contentType);
			if (serializer == null) {
				throw new UnsupportedEncodingException(String.format("Unable to deserialize response with Content-Type: %s. Supported decodings are: %s", headers.header(Headers.CONTENT_TYPE), supportedEncodings()));
			} else if (Deserializable.class.isAssignableFrom(responseClass) ||
					Map.class.isAssignableFrom(responseClass) ||
					List.class.isAssignableFrom(responseClass) ||
					String.class.isAssignableFrom(responseClass)) {
				return serializer.deserialize(responseBody, responseClass);
			} else {
				throw new UnsupportedEncodingException(String.format("Destination class %s must implement Deserializable, Map, List, or String", responseClass.getSimpleName()));
			}
		} else {
			throw new UnsupportedEncodingException("HttpResponse does not have Content-Type header set");
		}
	}

	private List<String> supportedEncodings() {
		List<String> supportedEncodings = new ArrayList<>();
		for (Serializer serializer : serializers) {
			supportedEncodings.add(serializer.contentType());
		}

		return supportedEncodings;
	}

	private Serializer serializer(String contentType) {
		for (Serializer s : serializers) {
			if (contentType.matches(s.contentType())) {
				return s;
			}
		}

		return null;
	}
}
