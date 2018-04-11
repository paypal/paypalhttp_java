package com.braintreepayments.http.multipart;

import com.braintreepayments.http.Encoder;
import com.braintreepayments.http.Headers;
import com.braintreepayments.http.HttpRequest;
import com.braintreepayments.http.serializer.StreamUtils;

import java.io.IOException;
import java.io.OutputStream;

import static com.braintreepayments.http.serializer.Multipart.CRLF;

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
        return super.header() + String.format("; filename=\"%s.json\"%sContent-Type: %s", key(), CRLF, contentType);
	}

	@Override
	public void writeData(OutputStream os) throws IOException {
		HttpRequest fakeReq = new HttpRequest("/", "GET", Void.class)
				.requestBody(value)
				.header(Headers.CONTENT_TYPE, contentType);

		StreamUtils.writeOutputStream(os, new Encoder().serializeRequest(fakeReq));
	}
}
