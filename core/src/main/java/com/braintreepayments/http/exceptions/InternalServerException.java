package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 500 HTTP_INTERNAL_ERROR response is encountered. Indicates an unexpected
 * error from the server.
 */
public class InternalServerException extends HttpServerException {
	public InternalServerException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
