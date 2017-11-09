package com.braintreepayments.http.utils;

import com.braintreepayments.http.Environment;
import com.braintreepayments.http.Headers;
import com.braintreepayments.http.HttpRequest;
import com.braintreepayments.http.HttpResponse;
import com.braintreepayments.http.exceptions.SerializeException;
import com.braintreepayments.http.serializer.Json;
import com.braintreepayments.http.testutils.WireMockHarness;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.util.HashMap;
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
		String body = null;
		if (request.requestBody() != null) {
			if (request.requestBody() instanceof String) {
				body = (String) request.requestBody();
			} else {
				try {
					body = new String(new Json().encode(request));
				} catch (IOException ignored) {}
			}
		}

		String responseBody = null;
		Integer statusCode = null;
		Map<String, String> responseHeaders = null;
		if (response != null) {
			if (response.result() != null) {
				if (response.result() instanceof String) {
					responseBody = (String) response.result();
				} else {
					try {
						responseBody = new Json().serialize(response.result());
					} catch (SerializeException ignored) {}
				}
			}

			statusCode = response.statusCode();
			responseHeaders = translateHeaders(response.headers());
		}

		super.stub(path, verb, body, headers, responseBody, statusCode, responseHeaders);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> translateHeaders(Headers headers) {
		Map<String, String> headerMap = new HashMap<>();

		for (String key : headers) {
			headerMap.put(key, headers.header(key));
		}

		return headerMap;
	}
}
