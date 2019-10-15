package com.paypal.http;

public class HttpRequest<T> {

	public HttpRequest(String path, String verb, Class<T> responseClass) {
		this.path = path;
		this.verb = verb;
		this.responseClass = responseClass;
	}

	private String path;
	private String verb;
	private Object body;
	private Class<T> responseClass;
	private Headers headers = new Headers();

	public HttpRequest<T> path(String path) {
		this.path = path;
		return this;
	}

	public HttpRequest<T> verb(String verb) {
		this.verb = verb;
		return this;
	}

	public HttpRequest<T> requestBody(Object body) {
		this.body = body;
		return this;
	}

	public String path() {
		return this.path;
	}

	public String verb() {
		return this.verb;
	}

	public Object requestBody() {
		return this.body;
	}

	public Headers headers() {
		return this.headers;
	}

	public Class<T> responseClass() {
		return this.responseClass;
	}

	public HttpRequest<T> header(String header, String value) {
		headers.header(header, value);
		return this;
	}

	public HttpRequest<T> copy() {
		HttpRequest<T> other = new HttpRequest<T>(path, verb, responseClass);
		for (String key: headers) {
			other.header(key, headers.header(key));
		}

		other.body = body;

		return other;
	}
}
