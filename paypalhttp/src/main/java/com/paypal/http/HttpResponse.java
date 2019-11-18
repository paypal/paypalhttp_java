package com.paypal.http;

public class HttpResponse<T> {

	private final Headers headers;
	private final int statusCode;
	private final T result;

	protected HttpResponse(Headers headers, int statusCode, T result) {
		this.headers = headers;
		this.statusCode = statusCode;
		this.result = result;
	}

	public Headers headers() {
		if (headers == null) {
			return new Headers();
		}
		return headers;
	}

	public int statusCode() {
		return statusCode;
	}

	public T result() {
		return result;
	}
}
