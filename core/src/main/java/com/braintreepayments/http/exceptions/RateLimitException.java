package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 429 HTTP_TOO_MANY_REQUESTS response is encountered. Indicates the client has hit a request
 * limit and should wait a period of time and try again.
 */
public class RateLimitException extends HttpServerException {
	public RateLimitException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
