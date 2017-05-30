package com.braintreepayments.http;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true, chain = true)
public class HttpResponse<T> {

	private final Headers headers;
	private final int statusCode;

	private final T result;

	public Headers headers() {
		if (headers == null) {
			return new Headers();
		}
		return headers;
	}
}
