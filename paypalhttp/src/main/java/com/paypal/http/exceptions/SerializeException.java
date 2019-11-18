package com.paypal.http.exceptions;

import java.io.IOException;

public class SerializeException extends IOException {
	public SerializeException(String message) {
		super(message);
	}
}
