package com.braintreepayments.http;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.testng.Assert.*;

public class EncoderTest {

	@Test
	public void testEncoder_encode_throwsForNonSerializableBodyType() throws UnsupportedEncodingException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "application/json");
		request.body(new Object());

		Encoder encoder = new Encoder();

		try {
			encoder.encode(request);
			fail("Expected encode to throw IOException");
		} catch (IOException ioe) {
			assertTrue(ioe instanceof UnsupportedEncodingException);
			assertEquals(ioe.getMessage(), "Body class Object must implement Serializable");
		}
	}

	@Test
	public void testEncoder_encode_throwsForUnsupportedContentType() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "text/html");
		request.body(new Zoo());

		Encoder encoder = new Encoder();

		try {
			encoder.encode(request);
			fail("Expected encode to throw IOException");
		} catch (IOException ioe) {
			assertTrue(ioe instanceof UnsupportedEncodingException);
			assertEquals(ioe.getMessage(), "Unable to serialize request with Content-Type: text/html. Supported encodings are: [application/json]");
		}
	}

	@Test
	public void testEncoder_encode() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "application/json");
		request.body(new Zoo());

		Encoder encoder = new Encoder();

		String s = encoder.encode(request);
		assertNotEquals(s, "");
	}

	@Test
	public void testEncoder_decode_throwsForNonDeserializableBodyType() throws IOException {
		String response = "{\"name\":\"Brian Tree\"}";
		Headers headers = new Headers();
		headers.header("Content-Type", "application/json");

		Encoder encoder = new Encoder();

		try {
			Object o = encoder.decode(response, Object.class, headers);
		} catch (IOException ioe) {
			assertTrue(ioe instanceof UnsupportedEncodingException);
			assertEquals(ioe.getMessage(), "Destination class Object must implement Deserializable");
		}
	}

	@Test
	public void testEncoder_decode_throwsForUnsupportedContentType() throws IOException {
		String response = "<html>\n<body>200</body>\n</html>";
		Headers headers = new Headers();
		headers.header("Content-Type", "text/html");

		Encoder encoder = new Encoder();

		try {
			Zoo z = encoder.decode(response, Zoo.class, headers);
		} catch (IOException ioe) {
			assertTrue(ioe instanceof UnsupportedEncodingException);
			assertEquals(ioe.getMessage(), "Unable to deserialize response with Content-Type: text/html. Supported decodings are: [application/json]");
		}
	}

	@Test
	public void testEncoder_decode() throws IOException {
		String response = "{\"name\":\"Brian Tree\"}";
		Headers headers = new Headers();
		headers.header("Content-Type", "application/json");

		Encoder encoder = new Encoder();

		Zoo s = encoder.decode(response, Zoo.class, headers);

		assertEquals(s.name, "Brian Tree");
	}
}
