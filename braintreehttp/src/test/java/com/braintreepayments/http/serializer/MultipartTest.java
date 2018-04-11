package com.braintreepayments.http.serializer;

import com.braintreepayments.http.Headers;
import com.braintreepayments.http.HttpRequest;
import com.braintreepayments.http.Zoo;
import com.braintreepayments.http.multipart.FilePart;
import com.braintreepayments.http.multipart.FormPart;
import com.braintreepayments.http.multipart.JsonPart;
import com.braintreepayments.http.multipart.MultipartBody;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.testng.Assert.*;

public class MultipartTest {

	private Multipart multipart = new Multipart();

	@Test
	public void testMultipart_serialize_serializesFileAndBodyDataCorrectly() throws IOException {
		String uploadData = new String(fileData("fileupload_test_text.txt"));

		FileUploadRequest request = simpleFileRequest()
				.file("file_test_text", resource("fileupload_test_text.txt").toFile())
				.formData("some_field_key", "form_field=\"some_field_value\"");

		Zoo.Animal mixedPart = new Zoo.Animal();
		mixedPart.age = 1;
		mixedPart.carnivorous = true;

		request.mixedData("some_mixed_part", mixedPart);

		String serialized = new String(multipart.encode(request));

		assertTrue(request.headers().header(Headers.CONTENT_TYPE).startsWith("multipart/form-data; boundary=boundary"));
		assertTrue(serialized.contains("Content-Disposition: form-data; name=\"file_test_text\"; filename=\"fileupload_test_text.txt\""));
		assertTrue(serialized.contains("Content-Type: text/plain"));
		assertTrue(serialized.contains(uploadData));
		assertTrue(serialized.contains("Content-Disposition: form-data; name=\"some_field_key\""));
		assertTrue(serialized.contains("form_field=\"some_field_value\""));
		assertTrue(serialized.contains("--boundary"));
		assertTrue(serialized.contains("Content-Disposition: form-data; name=\"some_mixed_part\"; filename=\"some_mixed_part.json\"\r\nContent-Type: application/json"));
		assertTrue(serialized.contains(new Json().serialize(mixedPart)));
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
		HttpRequest request = new HttpRequest("/", "POST", Void.class);
		request.header(Headers.CONTENT_TYPE, "multipart/form-data");
		request.requestBody(new Object());

		try {
			multipart.encode(request);
			fail("Http client should have thrown for MultipartBody requestBody");
		} catch (IOException ioe) {
			assertEquals("Request requestBody must be MultipartBody when Content-Type is multipart/*", ioe.getMessage());
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

	@Test
	public void testMultipart_serialize_binaryWithJsonPart() throws IOException {
		FileUploadRequest request = new FileUploadRequest("/multipart", "POST", Void.class)
		    .file("file_test_image", resource("fileupload_test_binary.jpg").toFile());

		Zoo.Animal mixedPart = new Zoo.Animal();
		mixedPart.age = 1;
		mixedPart.carnivorous = true;
		request.mixedData("some_mixed_part", mixedPart);

		byte[] data = multipart.encode(request);

		assertTrue(request.headers().header(Headers.CONTENT_TYPE).startsWith("multipart/form-data; boundary=boundary"));

		assertTrue(byteArrayContains(data, "Content-Disposition: form-data; name=\"file_test_image\"; filename=\"fileupload_test_binary.jpg\"".getBytes()));
		assertTrue(byteArrayContains(data, "Content-Type: image/jpeg".getBytes()));
		byte[] imageData = fileData("fileupload_test_binary.jpg");
		assertTrue(byteArrayContains(data, imageData));

		assertTrue(byteArrayContains(data, "--boundary".getBytes()));
		assertTrue(byteArrayContains(data, "Content-Disposition: form-data; name=\"some_mixed_part\"; filename=\"some_mixed_part.json\"\r\nContent-Type: application/json".getBytes()));
		assertTrue(byteArrayContains(data, new Json().serialize(mixedPart).getBytes()));
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

		private MultipartBody multipartBody;

		public FileUploadRequest(String path, String verb, Class<Void> responseClass) {
			super(path, verb, responseClass);
			header(Headers.CONTENT_TYPE, "multipart/form-data");
			multipartBody = new MultipartBody();
		}

		@SuppressWarnings("unchecked")
		public FileUploadRequest file(String key, File f) {
			multipartBody.addPart(new FilePart(key, f));
			return this;
		}

		@SuppressWarnings("unchecked")
		public FileUploadRequest formData(String key, String value) {
			multipartBody.addPart(new FormPart(key, value));
			return this;
		}

		@SuppressWarnings("unchecked")
		public FileUploadRequest mixedData(String key, Object data) {
			multipartBody.addPart(new JsonPart(key, data));
			return this;
		}

		@Override
		public Object requestBody() {
			return multipartBody;
		}
	}
}
