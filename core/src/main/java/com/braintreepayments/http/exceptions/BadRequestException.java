package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 400 BAD_REQUEST response is encountered. Indicates that
 * there was a problem with the request
 */
public class BadRequestException extends HttpServerException {
	public BadRequestException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
