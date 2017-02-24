package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;

/**
 * Exception thrown when a 404 HTTP_NOT_FOUND response is encountered. Indicates that the
 * requested resource could not be located on the server
 */
public class ResourceNotFoundException extends HttpServerException {
    public ResourceNotFoundException(String message, int statusCode, Headers headers) {
        super(message, statusCode, headers);
    }
}
