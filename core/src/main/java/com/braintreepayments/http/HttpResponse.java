package com.braintreepayments.http;

import com.braintreepayments.http.internal.JSONFormatter;
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

	public static <T> HttpResponse<T> deserialize(Headers headers, int statusCode, String httpBody, Class<T> responseClass) {
		HttpResponseBuilder<T> response = HttpResponse.<T>builder()
				.headers(headers)
				.statusCode(statusCode)
				.result(JSONFormatter.fromJSON(httpBody, responseClass));

		return response.build();
	}
}
