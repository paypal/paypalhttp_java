package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 403 HTTP_FORBIDDEN response is encountered. Indicates the current
 * authorization does not have permission to make the request.
 */
public class AuthorizationException extends HttpServerException {
	public AuthorizationException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
