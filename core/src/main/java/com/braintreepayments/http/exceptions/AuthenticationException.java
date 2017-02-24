package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 401 HTTP_UNAUTHORIZED response is encountered. Indicates authentication
 * has failed in some way.
 */
public class AuthenticationException extends HttpServerException {
	public AuthenticationException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
