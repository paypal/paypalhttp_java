package com.braintreepayments.http;

import com.braintreepayments.http.serializer.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Encoder {

	private List<Serializer> serializers = new ArrayList<>();

	public void registerSerializer(Serializer serializer) {
		serializers.add(serializer);
	}

	public Encoder() {
		registerSerializer(new Json());
		registerSerializer(new Text());
		registerSerializer(new Multipart());
		registerSerializer(new FormEncoded());
	}

	public byte[] serializeRequest(HttpRequest request) throws IOException {
		String contentType = request.headers().header(Headers.CONTENT_TYPE);

		if (contentType != null) {
			Serializer serializer = serializer(contentType);

			if (serializer == null) {
				throw new UnsupportedEncodingException(String.format("Unable to encode request with Content-Type: %s. Supported encodings are: %s", request.headers().header(Headers.CONTENT_TYPE), supportedEncodings()));
			}

			byte[] encoded = serializer.encode(request);

			if ("gzip".equals(request.headers().header("Content-Encoding"))) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				GZIPOutputStream gzos = new GZIPOutputStream(bos);

				try {
					gzos.write(encoded);
				} finally {
					bos.close();
					gzos.close();
				}

				return bos.toByteArray();
			}

			return encoded;
		} else {
			throw new UnsupportedEncodingException("HttpRequest does not have Content-Type header set");
		}
	}

	public <T> T deserializeResponse(InputStream stream, Class<T> responseClass, Headers headers) throws IOException {
		String contentType = headers.header(Headers.CONTENT_TYPE);
		String contentEncoding = headers.header("Content-Encoding");

		String responseBody = StreamUtils.readStream(stream, contentEncoding);

		stream.close();

		if (responseBody.isEmpty()) {
			return null;
		}

		if (contentType == null) {
			throw new UnsupportedEncodingException("HttpResponse does not have Content-Type header set");
		}

		Serializer serializer = serializer(contentType);

		if (serializer == null) {
			throw new UnsupportedEncodingException(String.format("Unable to decode response with Content-Type: %s. Supported decodings are: %s", headers.header(Headers.CONTENT_TYPE), supportedEncodings()));
		}

		if (responseBody.length() > 0) {
			return serializer.decode(responseBody, responseClass);
		}

		return null;
	}

	private List<String> supportedEncodings() {
		List<String> supportedEncodings = new ArrayList<>();

		for (Serializer serializer : serializers) {
			supportedEncodings.add(serializer.contentType());
		}

		return supportedEncodings;
	}

	private Serializer serializer(String contentType) {
		if (contentType.contains(";")) {
			contentType = contentType.split(";")[0];
		}

		for (Serializer s : serializers) {
			if (contentType.matches(s.contentType())) {
				return s;
			}
		}

		return null;
	}
}
