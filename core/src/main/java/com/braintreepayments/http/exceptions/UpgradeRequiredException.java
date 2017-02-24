package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 426 HTTP_UPGRADE_REQUIRED response is encountered. Indicates that the
 * API used or current SDK version is deprecated and must be updated.
 */
public class UpgradeRequiredException extends HttpServerException {
	public UpgradeRequiredException(String message, int statusCode, Headers headers) {
		super(message, statusCode, headers);
	}
}
