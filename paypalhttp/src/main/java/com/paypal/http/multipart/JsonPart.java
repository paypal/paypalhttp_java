package com.paypal.http.multipart;

import com.paypal.http.Encoder;
import com.paypal.http.Headers;
import com.paypal.http.HttpRequest;
import com.paypal.http.serializer.StreamUtils;
import com.paypal.http.serializer.Multipart;

import java.io.IOException;
import java.io.OutputStream;

public class JsonPart extends FormData {

	private Object value;
	private String contentType;

	public JsonPart(String key, Object value) {
		super(key);
		this.value = value;
		this.contentType = "application/json";
	}

	@Override
	public String header() {
        return super.header() + String.format("; filename=\"%s.json\"%sContent-Type: %s", key(), Multipart.CRLF, contentType);
	}

	@Override
	public void writeData(OutputStream os) throws IOException {
		HttpRequest fakeReq = new HttpRequest("/", "GET", Void.class)
				.requestBody(value)
				.header(Headers.CONTENT_TYPE, contentType);

		StreamUtils.writeOutputStream(os, new Encoder().serializeRequest(fakeReq));
	}
}
