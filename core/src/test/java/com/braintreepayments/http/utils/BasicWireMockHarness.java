package com.braintreepayments.http.utils;

import com.braintreepayments.http.Environment;
import com.braintreepayments.http.Headers;
import com.braintreepayments.http.HttpRequest;
import com.braintreepayments.http.HttpResponse;
import com.braintreepayments.http.internal.JSONFormatter;
import com.braintreepayments.http.testutils.WireMockHarness;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Field;
import java.util.Map;

public class BasicWireMockHarness extends WireMockHarness {

	private Environment mEnvironment;

	@BeforeMethod
	@Override
	public void setup() {
		super.setup();
		mEnvironment = () -> String.format("http://%s:%d", host(), port());
	}

	protected Environment environment() {
		return mEnvironment;
	}

	protected void stub(HttpRequest request, HttpResponse response) {
		String path = request.path();
		String verb = request.verb();
		Map<String, String> headers = translateHeaders(request.headers());
		String requestBody = null;
		if (request.requestBody() != null) {
			requestBody = JSONFormatter.toJSON(request.requestBody());
		}

		String responseBody = null;
		Integer statusCode = null;
		Map<String, String> responseHeaders = null;
		if (response != null) {
			if (response.result() instanceof String) {
				responseBody = (String) response.result();
			} else {
				responseBody = JSONFormatter.toJSON(response.result());
			}
			statusCode = response.statusCode();
			responseHeaders = translateHeaders(response.headers());
		}

		super.stub(path, verb, requestBody, headers, responseBody, statusCode, responseHeaders);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> translateHeaders(Headers headers) {
		try {
			Field mHeaders = Headers.class.getDeclaredField("mHeaders");
			mHeaders.setAccessible(true);

			return (Map<String, String>) mHeaders.get(headers);
		} catch (NoSuchFieldException | IllegalAccessException ignored) {}

		return null;
	}
}
