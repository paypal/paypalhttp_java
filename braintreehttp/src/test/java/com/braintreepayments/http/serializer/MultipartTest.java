package com.braintreepayments.http.serializer;

import com.braintreepayments.http.Headers;
import com.braintreepayments.http.HttpRequest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class MultipartTest {

	private Multipart multipart = new Multipart();

	@Test
	public void testMultipart_serialize_serializesFileAndBodyDataCorrectly() throws IOException {
		String uploadData = new String(fileData("fileupload_test_text.txt"));

		FileUploadRequest request = simpleFileRequest()
				.file("file_test_text", resource("fileupload_test_text.txt").toFile())
				.formData("some_field_key", "form_field=\"some_field_value\"");

		String serialized = new String(multipart.encode(request));

		assertTrue(request.headers().header(Headers.CONTENT_TYPE).startsWith("multipart/form-data; boundary=boundary"));

		assertTrue(serialized.contains("Content-Disposition: form-data; name=\"file_test_text\"; filename=\"fileupload_test_text.txt\""));
		assertTrue(serialized.contains("Content-Type: text/plain"));
		assertTrue(serialized.contains(uploadData));
		assertTrue(serialized.contains("Content-Disposition: form-data; name=\"some_field_key\""));
		assertTrue(serialized.contains("form_field=\"some_field_value\""));
		assertTrue(serialized.contains("--boundary"));
	}

	@Test
	public void testMultipart_serialize_withBinaryData() throws IOException {
		FileUploadRequest request = simpleFileRequest()
				.file("binary_file", resource("fileupload_test_binary.jpg").toFile());

		byte[] data = multipart.encode(request);
		assertTrue(byteArrayContains(data, "Content-Disposition: form-data; name=\"binary_file\"; filename=\"fileupload_test_binary.jpg\"".getBytes()));
		assertTrue(byteArrayContains(data, "Content-Type: image/jpeg".getBytes()));

		byte[] imageData = fileData("fileupload_test_binary.jpg");
		assertTrue(byteArrayContains(data, imageData));
	}

	@Test
	public void testMultipart_serialize_throwsWhenBodyNotMap() throws IOException {
		FileUploadRequest request = simpleFileRequest();
		request.requestBody(new Object());

		try {
			multipart.encode(request);
			fail("Http client should have thrown for non-Map requestBody");
		} catch (IOException ioe) {
			assertEquals("Request requestBody must be Map<String, Object> when Content-Type is multipart/*", ioe.getMessage());
		}
	}

	@Test
	public void testMultipart_deserialize_throwsWithUnsupportedEncodingException() throws IOException {
		try {
			multipart.decode("", "".getClass());
			fail("Multipart should never decode a response of mulipart/*");
		} catch (IOException ioe) {
			assertEquals("Unable to decode Content-Type: multipart/form-data.", ioe.getMessage());
		}
	}

	private boolean byteArrayContains(byte[] b1, byte[] subba) {
		for (int i = 0; i < b1.length - subba.length; i++ ) {
			if (Arrays.equals(Arrays.copyOfRange(b1, i, i + subba.length), subba)) {
				return true;
			}
		}

		return false;
	}

	private Path resource(String name) {
		return Paths.get("src/test/resources/" + name).toAbsolutePath();
	}

	private byte[] fileData(String name) throws IOException {
		return Files.readAllBytes(resource(name).toAbsolutePath());
	}

	private FileUploadRequest simpleFileRequest() {
		return new FileUploadRequest("/file_upload", "POST", Void.class);
	}

	private class FileUploadRequest extends HttpRequest<Void> {

		public FileUploadRequest(String path, String verb, Class<Void> responseClass) {
			super(path, verb, responseClass);
			header(Headers.CONTENT_TYPE, "multipart/form-data");
			requestBody(new HashMap<String, Object>());
		}

		public FileUploadRequest file(String key, File f) {
			Map<String, Object> existingBody = ((Map<String, Object>) requestBody());
			existingBody.put(key, f);
			requestBody(existingBody);
			return this;
		}

		public FileUploadRequest formData(String key, String value) {
			Map<String, Object> existingBody = ((Map<String, Object>) requestBody());
			existingBody.put(key, value);
			requestBody(existingBody);
			return this;
		}
	}
}
