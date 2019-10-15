package com.paypal.http.multipart;

import java.io.IOException;
import java.io.OutputStream;

import static com.paypal.http.serializer.StreamUtils.writeOutputStream;

public class FormPart extends FormData {

	private String value;

	public FormPart(String key, String value) {
		super(key);
		this.value = value;
	}

	@Override
	public void writeData(OutputStream os) throws IOException {
		writeOutputStream(os, value);
	}
}
