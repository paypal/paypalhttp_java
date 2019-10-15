package com.paypal.http.exceptions;

import java.io.IOException;

import com.paypal.http.Headers;

public class HttpException extends IOException {

	private Headers headers;
	private int statusCode;

	public HttpException(String message, int statusCode, Headers headers) {
		super(message);
		this.statusCode = statusCode;
		this.headers = headers;
	}

	public Headers headers() {
		if (headers == null) {
			return new Headers();
		}
		return headers;
	}

	public int statusCode() {
		return statusCode;
	}
}
