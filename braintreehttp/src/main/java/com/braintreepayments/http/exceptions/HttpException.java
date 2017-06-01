package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpException extends IOException {

	private int statusCode;
	private Headers headers;

	public HttpException(String message, int statusCode, Headers headers) {
		super(message);
		this.statusCode = statusCode;
		this.headers = headers;
	}
}
