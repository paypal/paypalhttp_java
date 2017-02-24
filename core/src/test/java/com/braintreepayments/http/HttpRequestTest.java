package com.braintreepayments.http;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class HttpRequestTest {

	@Test
	public void testHttpRequest_addHeader_setsHeaderInHeaders() {
		HttpRequest<String> request = new HttpRequest<String>("/", "POST", String.class)
				.header("key", "value");

		assertNotNull(request.headers().header("key"));
		assertEquals("value", request.headers().header("key"));
	}

	@Test
	public void testHttpRequest_serialize_stringBodyRemainsUnchanged() {
		HttpRequest<Void> req = new HttpRequest<Void>("", "", Void.class)
				.requestBody("{\"some_preformatted_json\":true}");

		assertEquals(req.serialize(), "{\"some_preformatted_json\":true}");
	}
}
