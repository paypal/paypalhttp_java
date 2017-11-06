package com.braintreepayments.http.serializer;

import com.braintreepayments.http.HttpRequest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class FormEncodedTest {

	@Test
	public void testFormEncoded_encode_throwsWhenBodyNotMap() {
		HttpRequest request = new HttpRequest("/", "GET", Void.class);
		request.requestBody(new Object());

		FormEncoded formEncoded = new FormEncoded();

		try {
			formEncoded.encode(request);
			fail("Http client should have thrown for non-Map requestBody");
		} catch (IOException ioe) {
			assertEquals("Request requestBody must be Map<String, Object> when Content-Type is multipart/*", ioe.getMessage());
		}
	}

	@Test
	public void testFormEncoded_encode_encodesData() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "GET", Void.class);

		Map<String, String> data = new HashMap<>();
		data.put("key", "value");
		data.put("anotherkey", "anothervalue");

		request.requestBody(data);

		FormEncoded formEncoded = new FormEncoded();

		String encoded = new String(formEncoded.encode(request));

		// Order is non-deterministic
		String regex = "(key=value&anotherkey=anothervalue|anotherkey=anothervalue&key=value)";
		assertTrue(encoded.matches(regex));
	}

	@Test
	public void testFormEncoded_encode_escapesValues() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "GET", Void.class);

		Map<String, String> data = new HashMap<>();
		data.put("key", "value with dashes and spaces");

		request.requestBody(data);

		FormEncoded formEncoded = new FormEncoded();

		String encoded = new String(formEncoded.encode(request));

		assertEquals("key=value+with+dashes+and+spaces", encoded);
	}
}
