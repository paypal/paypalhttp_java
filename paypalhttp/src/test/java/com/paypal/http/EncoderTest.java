package com.paypal.http;

import com.paypal.http.multipart.FormPart;
import com.paypal.http.multipart.MultipartBody;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.testng.AssertJUnit.*;

public class EncoderTest {

	@Test
	public void testEncoder_encode_throwsForUnsupportedContentType() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "not application/json");
		request.requestBody(new Zoo());

		Encoder encoder = new Encoder();

		try {
			encoder.serializeRequest(request);
			fail("Expected serializeRequest to throw IOException");
		} catch (IOException ioe) {
			assertTrue(ioe instanceof UnsupportedEncodingException);
			assertTrue(ioe.getMessage().contains("Unable to encode request with content-type: not application/json. Supported encodings are: "));
		}
	}

	@Test
	public void testEncoder_decode_throwsForNonDeserializableBodyType() throws IOException {
		String response = "{\"name\":\"Brian Tree\"}";
		Headers headers = new Headers();
		headers.header("Content-Type", "application/json");

		Encoder encoder = new Encoder();

		try {
			Object o = encoder.deserializeResponse(new ByteArrayInputStream(response.getBytes()), Object.class, headers);
		} catch (IOException ioe) {
			assertTrue(ioe instanceof UnsupportedEncodingException);
			assertEquals(ioe.getMessage(), "Destination class Object must implement Deserializable, Map, List, or String");
		}
	}

	@Test
	public void testEncoder_decode_throwsForUnsupportedContentType() throws IOException {
		String response = "<html>\n<requestBody>200</requestBody>\n</html>";
		Headers headers = new Headers();
		headers.header("Content-Type", "not application/json");

		Encoder encoder = new Encoder();

		try {
			Zoo z = encoder.deserializeResponse(new ByteArrayInputStream(response.getBytes()), Zoo.class, headers);
		} catch (IOException ioe) {
			assertTrue(ioe instanceof UnsupportedEncodingException);
			assertTrue(ioe.getMessage().contains("Unable to decode response with content-type: not application/json. Supported decodings are: "));
		}
	}

	@Test
	public void testEncoder_encode_json() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "application/json");
		request.requestBody(new Zoo());

		Encoder encoder = new Encoder();

		String s = new String(encoder.serializeRequest(request));
		assertNotSame(s, "");
	}

	@Test
	public void testEncoder_decode_json() throws IOException {
		String response = "{\"name\":\"Brian Tree\"}";
		Headers headers = new Headers();
		headers.header("Content-Type", "application/json");

		Encoder encoder = new Encoder();

		Zoo s = encoder.deserializeResponse(new ByteArrayInputStream(response.getBytes()), Zoo.class, headers);

		assertEquals(s.name, "Brian Tree");
	}

	@Test
	public void testEncoder_decode_json_case_insensitive() throws IOException {
		String response = "{\"name\":\"Brian Tree\"}";
		Headers headers = new Headers();
		headers.header("Content-Type", "application/JSON");

		Encoder encoder = new Encoder();

		Zoo s = encoder.deserializeResponse(new ByteArrayInputStream(response.getBytes()), Zoo.class, headers);

		assertEquals(s.name, "Brian Tree");
	}

	@Test
	public void testEncoder_decode_json_withCharset() throws IOException {
		String response = "{\"name\":\"Brian Tree\"}";
		Headers headers = new Headers();
		headers.header("Content-Type", "application/json; charset=utf-8");

		Encoder encoder = new Encoder();

		Zoo s = encoder.deserializeResponse(new ByteArrayInputStream(response.getBytes()), Zoo.class, headers);

		assertEquals(s.name, "Brian Tree");
	}

	@Test
	public void testEncoder_encode_withGzip() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "text/plain");
		request.header("Content-Encoding", "gzip");
		request.requestBody("Some plain text");

		Encoder encoder = new Encoder();

		byte[] encoded = encoder.serializeRequest(request);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(bos);

		gzos.write("Some plain text".getBytes());
		gzos.close();
		bos.close();

		assertArrayEquals(bos.toByteArray(), encoded);
	}

	@Test
	public void testEncoder_decode_withGzip() throws IOException {
		String rawData = "some plain text";
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(bos);

		gzos.write(rawData.getBytes());
		gzos.close();
		bos.close();

		Headers headers = new Headers();

		headers.header("Content-Type", "text/plain");
		headers.header("Content-Encoding", "gzip");

		String decoded = new Encoder().deserializeResponse(new ByteArrayInputStream(bos.toByteArray()), String.class, headers);

		assertEquals(rawData, decoded);
	}

	@Test
	public void testEncoder_encode_list() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "application/json");
		ArrayList<Zoo> body = new ArrayList();
		body.add(new Zoo("name", 1, null));
		request.requestBody(body);

		Encoder encoder = new Encoder();

		String s = new String(encoder.serializeRequest(request));
		assertNotSame("", s);
	}

	@Test
	public void testEncoder_encode_map() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "application/json");
		Map<String, Object> body = new HashMap<>();
		body.put("one", "two");
		request.requestBody(body);

		Encoder encoder = new Encoder();

		String s = new String(encoder.serializeRequest(request));
		assertEquals(s, "{\"one\":\"two\"}");
	}

	@Test
	public void testEncoder_encode_text() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "text/html; charset=utf8");
		request.requestBody("some text");

		Encoder encoder = new Encoder();

		String s = new String(encoder.serializeRequest(request));
		assertEquals(s, "some text");
	}

	@Test
	public void testEncoder_decode_text() throws IOException {
		String response = "<h1>Hello!</h1>";
		Headers headers = new Headers();
		headers.header("Content-Type", "text/html; charset=utf8");

		Encoder encoder = new Encoder();

		String s = encoder.deserializeResponse(new ByteArrayInputStream(response.getBytes()), String.class, headers);

		assertEquals(s, response);
	}

	@Test
	public void testEncoder_encode_multipart() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "multipart/form-data; charset=utf8");

		MultipartBody body = new MultipartBody(new FormPart("Key", "Value"));

		request.requestBody(body);

		Encoder encoder = new Encoder();
		String s = new String(encoder.serializeRequest(request));

		assertNotSame(s, "");
	}

	@Test
	public void testEncoder_encode_formEncoded() throws IOException {
		HttpRequest<Void> request = new HttpRequest("/", "POST", Void.class);
		request.header("Content-Type", "application/x-www-form-urlencoded; charset=utf8");

		Map<String, Object> body = new HashMap<>();
		body.put("Key", "Value");
		body.put("Key-2", "Value-2");

		request.requestBody(body);

		Encoder encoder = new Encoder();
		String s = new String(encoder.serializeRequest(request));

		assertNotSame("", s);
	}
}
