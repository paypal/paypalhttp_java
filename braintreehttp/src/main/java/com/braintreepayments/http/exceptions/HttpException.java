package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;

@Data
@Accessors(fluent = true)
public class HttpException extends IOException {

	@Setter(AccessLevel.NONE)
	private int statusCode;

	@Setter(AccessLevel.NONE)
	private Headers headers;

	public HttpException(String message, int statusCode, Headers headers) {
		super(message);
		this.statusCode = statusCode;
		this.headers = headers;
	}
}