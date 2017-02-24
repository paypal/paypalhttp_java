package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 422 HTTP_UNPROCESSABLE_ENTITY response is encountered. Indicates the
 * request was invalid in some way.
 */
public class UnprocessableEntityException extends HttpServerException {
	public UnprocessableEntityException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
