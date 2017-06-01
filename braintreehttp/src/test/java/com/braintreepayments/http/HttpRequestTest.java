package com.braintreepayments.http;

import org.testng.annotations.Test;

import java.util.Base64;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class HttpRequestTest {

	@Test
	public void testHttpRequest_addHeader_setsHeaderInHeaders() {
		HttpRequest<String> request = new HttpRequest<>("/", "POST", String.class)
				.header("key", "value");

		assertNotNull(request.headers().header("key"));
		assertEquals("value", request.headers().header("key"));
	}

	@Test
	public void testHttpRequest_basic_authorization_setsHeaderCorrectly() {
		HttpRequest<String> request = new HttpRequest<>("/", "POST", String.class);

		String user = "user";
		String pass = "pass";
		request.basicAuthorization(user, pass);

		String authString = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
		assertEquals("Basic " + authString, request.headers().header("Authorization"));
	}
}
