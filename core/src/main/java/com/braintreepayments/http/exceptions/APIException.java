package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;

@Data
@EqualsAndHashCode(callSuper = true)
public class APIException extends IOException {

	private int statusCode;
	private Headers headers;

	public APIException(String message, int statusCode, Headers headers) {
		super(message);
		this.statusCode = statusCode;
		this.headers = headers;
	}
}
