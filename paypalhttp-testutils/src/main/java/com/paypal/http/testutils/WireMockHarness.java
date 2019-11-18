package com.paypal.http.testutils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockHarness {

	private final static int PORT = 8089;
	private WireMockServer wireMockServer = null;

	@BeforeMethod
	public void setup() {
		wireMockServer = new WireMockServer(WireMockConfiguration.options()
				.port(8089)
				.notifier(new ConsoleNotifier(true)));
		wireMockServer.start();

		WireMock.configureFor(host(), PORT);
	}

	@AfterMethod
	public void teardown() {
		wireMockServer.stop();
	}

	protected int port() {
		return PORT;
	}

	protected String host() { return "localhost"; }

	protected void stub(String path,
						String verb,
						String requestBody,
						Map<String, String> headers,
						String responseBody,
						Integer statusCode,
						Map<String, String> responseHeaders) {

		MappingBuilder mappingBuilder;

		UrlPathPattern pattern = urlPathEqualTo(path);
		verb = verb.toUpperCase();

		switch (verb) {
			case "GET":
				mappingBuilder = WireMock.get(pattern);
				break;
			case "POST":
				mappingBuilder = WireMock.post(pattern);
				break;
			case "PUT":
				mappingBuilder = WireMock.put(pattern);
				break;
			case "DELETE":
				mappingBuilder = WireMock.delete(pattern);
				break;
			case "PATCH":
				mappingBuilder = WireMock.patch(pattern);
				break;
			default:
				throw new RuntimeException("Invalid or no verb passed in request");
		}

		for (String headerKey : headers.keySet()) {
			mappingBuilder.withHeader(headerKey, WireMock.equalTo(headers.get(headerKey)));
		}

		if (requestBody != null) {
			mappingBuilder.withRequestBody(equalTo(requestBody));
		}

		ResponseDefinitionBuilder responseBuilder = WireMock.aResponse();
		if (responseBody != null) {
			responseBuilder.withBody(responseBody);
		}

		if (statusCode != null) {
			responseBuilder.withStatus(statusCode);
		}

		if (responseHeaders != null) {
			for (String headerKey : responseHeaders.keySet()) {
				responseBuilder.withHeader(headerKey, responseHeaders.get(headerKey));
			}
		}

		mappingBuilder.willReturn(responseBuilder);

		stubFor(mappingBuilder);
	}
}
