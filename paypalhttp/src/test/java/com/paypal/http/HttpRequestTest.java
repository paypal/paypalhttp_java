package com.paypal.http;

import org.testng.annotations.Test;

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
}
