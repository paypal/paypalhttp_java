package com.paypal.http.multipart;

import java.io.IOException;
import java.io.OutputStream;

public abstract class FormData {

	private String key;

	public FormData(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	public String header() {
		return String.format("Content-Disposition: form-data; name=\"%s\"", key());
	}

	public abstract void writeData(OutputStream os) throws IOException;
}
