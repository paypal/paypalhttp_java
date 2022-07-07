package com.paypal.http.exceptions;

import java.io.IOException;

public class MalformedJsonException extends IOException {
	public MalformedJsonException(String message) {
		super(message);
	}
}
