package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpServerException extends IOException {

	private int statusCode;
	private Headers headers;

	public HttpServerException(String message, int statusCode, Headers headers) {
		super(message);
		this.statusCode = statusCode;
		this.headers = headers;
	}
}
