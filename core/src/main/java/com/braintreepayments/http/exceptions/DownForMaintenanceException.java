package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 503 HTTP_UNAVAILABLE response is encountered. Indicates the server is
 * unreachable or the request timed out.
 */
public class DownForMaintenanceException extends HttpServerException {
	public DownForMaintenanceException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
