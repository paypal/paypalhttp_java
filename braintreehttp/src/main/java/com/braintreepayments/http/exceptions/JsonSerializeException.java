package com.braintreepayments.http.exceptions;

import java.io.IOException;

public class JsonSerializeException extends IOException {
	public JsonSerializeException(String message) {
		super(message);
	}
}
