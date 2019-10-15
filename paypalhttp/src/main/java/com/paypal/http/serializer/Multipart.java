package com.paypal.http.serializer;


import com.paypal.http.Headers;
import com.paypal.http.HttpRequest;
import com.paypal.http.exceptions.SerializeException;
import com.paypal.http.multipart.FormData;
import com.paypal.http.multipart.MultipartBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import static com.paypal.http.serializer.StreamUtils.writeOutputStream;

public class Multipart implements Serializer {

	public static final String CRLF = "\r\n";

	@Override
	public String contentType() {
		return "^multipart\\/.*";
	}

	@Override
	@SuppressWarnings("unchecked")
	public byte[] encode(HttpRequest request) throws IOException {
		if (!(request.requestBody() instanceof MultipartBody)) {
			throw new SerializeException("Request requestBody must be MultipartBody when Content-Type is multipart/*");
		} else {
			String contentType = request.headers().header(Headers.CONTENT_TYPE);
			String boundary = "boundary" + System.currentTimeMillis();
			contentType = contentType + "; boundary=" + boundary;
			request.header(Headers.CONTENT_TYPE, contentType); // Rewrite header with boundary

			MultipartBody body = (MultipartBody) request.requestBody();
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			for (FormData formData : body) {
				writePart(os, formData, boundary);
			}

			writeOutputStream(os, "--" + boundary + "--");
			writeOutputStream(os, CRLF);
			writeOutputStream(os, CRLF);

			return os.toByteArray();
		}
	}

	@Override
	public <T> T decode(String source, Class<T> cls) throws IOException {
		throw new UnsupportedEncodingException("Unable to decode Content-Type: multipart/form-data.");
	}

	private void writePart(OutputStream writer, FormData part, String boundary) throws IOException {
		writeOutputStream(writer,"--" + boundary);
		writeOutputStream(writer, CRLF);
		writeOutputStream(writer, part.header());
		writeOutputStream(writer, CRLF);
		writeOutputStream(writer, CRLF);

		part.writeData(writer);
		writeOutputStream(writer, CRLF);
	}
}
