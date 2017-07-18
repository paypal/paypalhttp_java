package com.braintreepayments.http;

import java.io.File;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;

@Data()
@NonFinal
@Accessors(chain=true, fluent = true)
public class HttpRequest<T> {

	public HttpRequest(String path, String verb, Class<T> responseClass) {
		this.path = path;
		this.verb = verb;
		this.responseClass = responseClass;
	}

	private String path;
	private String verb;
	private Object body;
    private File file;

	@Getter
	@Setter(AccessLevel.NONE)
	private Class<T> responseClass;

	@Getter
	@Setter(AccessLevel.NONE)
	private Headers headers = new Headers();


	public HttpRequest<T> header(String header, String value) {
		headers.header(header, value);
		return this;
	}
}
