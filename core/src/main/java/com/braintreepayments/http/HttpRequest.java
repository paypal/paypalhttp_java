package com.braintreepayments.http;

import com.braintreepayments.http.internal.JSONFormatter;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true, fluent = true)
public final class HttpRequest<T> {

	public HttpRequest(String path, String verb, Class<T> responseClass) {
		this.path = path;
		this.verb = verb;
		this.responseClass = responseClass;
	}

	private String baseUrl;
	private String path;
	private String verb;

	@Getter
	@Setter(AccessLevel.NONE)
	private Class<T> responseClass;

	@Getter
	@Setter(AccessLevel.NONE)
	private Headers headers = new Headers();

	private Object requestBody;
	private String oAuthScope;

	public HttpRequest<T> header(String header, String value) {
		headers.header(header, value);
		return this;
	}

	public String url() {
		return baseUrl + path;
	}

	public String serialize() {
		if (requestBody instanceof String) {
			return (String) requestBody;
		}

		return JSONFormatter.toJSON(requestBody);
	}
}
