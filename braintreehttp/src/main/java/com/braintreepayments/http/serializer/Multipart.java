package com.braintreepayments.http.serializer;


import com.braintreepayments.http.Headers;
import com.braintreepayments.http.HttpRequest;
import com.braintreepayments.http.exceptions.SerializeException;

import java.io.*;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.util.Map;

import static com.braintreepayments.http.serializer.StreamUtils.writeOutputStream;

public class Multipart implements Serializer {

	protected static final String CRLF = "\r\n";

	@Override
	public String contentType() {
		return "^multipart\\/.*";
	}

	@Override
	public byte[] serialize(HttpRequest request) throws IOException {
		if (!(request.requestBody() instanceof Map)) {
			throw new SerializeException("Request requestBody must be Map<String, Object> when Content-Type is multipart/*");
		} else {
			String contentType = request.headers().header(Headers.CONTENT_TYPE);
			String boundary = "boundary" + System.currentTimeMillis();
			contentType = contentType + "; boundary=" + boundary;
			request.header(Headers.CONTENT_TYPE, contentType); // Rewrite header with boundary

			Map<String, Object> body = (Map<String, Object>) request.requestBody();

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			for (String key : body.keySet()) {
				Object value = body.get(key);
				if (value instanceof File) {
					addFilePart(os, key, (File) value, boundary);
				} else {
					addFormPart(os, key, String.valueOf(value), boundary);
				}
			}

			writeOutputStream(os, "--" + boundary + "--");
			writeOutputStream(os, CRLF);
			writeOutputStream(os, CRLF);

			return os.toByteArray();
		}
	}

	@Override
	public <T> T deserialize(String source, Class<T> cls) throws IOException {
		throw new UnsupportedEncodingException("Unable to deserialize Content-Type: multipart/form-data.");
	}

	private void writePartHeader(OutputStream writer, String name, String filename, String boundary) throws IOException {
		writeOutputStream(writer,"--" + boundary);
		writeOutputStream(writer, CRLF);
		writeOutputStream(writer, "Content-Disposition: form-data; name=\"" + name + "\"");
		if (filename != null) {
			writeOutputStream(writer, "; filename=\"" + filename + "\"");
			writeOutputStream(writer, CRLF);
			writeOutputStream(writer, "Content-Type: " + URLConnection.guessContentTypeFromName(filename));
		}
		writeOutputStream(writer, CRLF);
		writeOutputStream(writer, CRLF);
	}

	private void addFormPart(OutputStream writer, String key, String value, String boundary) throws IOException {
		writePartHeader(writer, key, null, boundary);

		writeOutputStream(writer, value);
		writeOutputStream(writer, CRLF);
	}

	private void addFilePart(OutputStream writer, String key, File uploadFile, String boundary)
		throws IOException {
		String filename = uploadFile.getName();
		writePartHeader(writer, key, filename, boundary);

		new FileInputStream(uploadFile).getChannel()
				.transferTo(0, uploadFile.length(), Channels.newChannel(writer));

		writeOutputStream(writer, CRLF);
	}
}
