package com.paypal.http;
import com.paypal.http.serializer.*;
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
				String message = String.format("Unable to encode request with content-type: %s. Supported encodings are: %s",request.headers().header(Headers.CONTENT_TYPE), supportedEncodings());
				System.out.println(message);
				throw new UnsupportedEncodingException(message);
			}

			byte[] encoded = serializer.encode(request);

			if ("gzip".equals(request.headers().header("content-encoding"))) {
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
			String message = "HttpRequest does not have content-type header set";
			System.out.println(message);
			throw new UnsupportedEncodingException(message);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T deserializeResponse(InputStream stream, Class<T> responseClass, Headers headers) throws IOException {
		String contentType = headers.header(Headers.CONTENT_TYPE);
		String contentEncoding = headers.header("content-encoding");

		String responseBody = StreamUtils.readStream(stream, contentEncoding);

		stream.close();

		if (responseClass.isAssignableFrom(String.class)) {
			return (T) responseBody;
		}

		if (responseBody.isEmpty()) {
			return null;
		}

		if (contentType == null) {
			String message = "HttpResponse does not have content-type header set" ;
			System.out.println(message);
			throw new UnsupportedEncodingException(message);
		}
		// Setting to lowercase
		contentType = contentType.toLowerCase();

		Serializer serializer = serializer(contentType);

		if (serializer == null) {
			String message = String.format("Unable to decode response with content-type: %s. Supported decodings are: %s", headers.header(Headers.CONTENT_TYPE), supportedEncodings());
			System.out.println(message);
			throw new UnsupportedEncodingException(message);
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
