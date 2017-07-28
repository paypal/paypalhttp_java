package com.braintreepayments.http;

import com.braintreepayments.http.exceptions.HttpException;
import com.braintreepayments.http.internal.TLSSocketFactory;
import com.braintreepayments.http.serializer.Deserializable;
import com.braintreepayments.http.serializer.Serializable;
import com.braintreepayments.http.utils.BasicWireMockHarness;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.braintreepayments.http.Headers.CONTENT_TYPE;
import static com.braintreepayments.http.Headers.USER_AGENT;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.*;
import static java.net.HttpURLConnection.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

public class HttpClientTest extends BasicWireMockHarness {

	private HttpClient client = null;
	private Environment httpsEnvironment = () -> "https://localhost";

	@BeforeMethod
	public void setup() {
		super.setup();
		client = new HttpClient(environment());
	}

	@Test(dataProvider = "getErrorCodesWithException")
	public void testHttpClient_execute_throwsProperException(final int statusCode, final Class expectedExceptionClass) throws IOException, IllegalAccessException, InstantiationException {
		HttpRequest<String> request = simpleRequest();
		HttpResponse expectedResponse = HttpResponse.builder().statusCode(statusCode).build();

		stub(request, expectedResponse);

		try {
			client.execute(request);
			fail("We should always be throwing an exception");
		} catch (Exception ex) {
			assertEquals(expectedExceptionClass.getSimpleName(), ex.getClass().getSimpleName());
		}
	}

	@Test(dataProvider = "getSuccessCode")
	public void testHttpClient_execute_returnsSuccess(final int statusCode) throws IOException {
		HttpRequest<String> request = simpleRequest();
		HttpResponse<String> expectedResponse = HttpResponse.<String>builder()
						.statusCode(statusCode)
						.build();

		stub(request, expectedResponse);

		HttpResponse actualResponse = client.execute(request);
		assertEquals(expectedResponse.statusCode(), actualResponse.statusCode());
	}


	@Test(dataProvider = "getVerbs")
	public void testHttpClient_execute_setsVerbFromRequest(RequestMethod requestMethod) throws IOException {
		HttpRequest<String> request = simpleRequest()
						.verb(requestMethod.getName());
		stub(request, null);

		client.execute(request);
		verify(new RequestPatternBuilder(requestMethod, urlEqualTo("/")));
	}

	@Test
	public void testHttpClient_new_setsDefaultValues() throws IOException {
		HttpRequest<String> request = simpleRequest();
		stub(request, null);

		client.execute(request);

		verify(getRequestedFor(urlEqualTo("/"))
						.withHeader("User-Agent", equalTo("Java HTTP/1.1")));
	}

	@Test
	public void testHttpClient_execute_setsPathFromRequest() throws IOException {
		HttpRequest<String> request = simpleRequest()
						.path("/somepath/to/a/rest/resource");
		stub(request, null);

		client.execute(request);
		verify(getRequestedFor(urlEqualTo("/somepath/to/a/rest/resource")));
	}

	@Test
	public void testHttpClient_setUserAgent_setsUserAgentInRequest() throws IOException {
		client.setUserAgent("Test User Agent");
		HttpRequest<String> request = simpleRequest();
		stub(request, null);

		client.execute(request);

		verify(getRequestedFor(urlEqualTo("/"))
						.withHeader("User-Agent", equalTo("Test User Agent")));
	}

	@Test
	public void testHttpClient_usesDefaultSSLSocketFactoryWhenNoFactoryIsSet()
					throws IOException, NoSuchFieldException, IllegalAccessException {

		assertTrue(client.getSSLSocketFactory() instanceof TLSSocketFactory);
	}

	@Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "SSLSocketFactory was not set or failed to initialize")
	public void testHttpClient_execute_postsErrorForHttpsRequestsWhenSSLSocketFactoryIsNull()
					throws InterruptedException, IOException {
		client = new HttpClient(httpsEnvironment);
		client.setSSLSocketFactory(null);

		HttpRequest<String> request = simpleRequest();

		client.execute(request);
	}

	@Test
	public void testHttpClient_getConnection_usesSSLSocketFactory() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		client = new HttpClient(httpsEnvironment);

		SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
		client.setSSLSocketFactory(sslSocketFactory);

		HttpRequest<String> request = simpleRequest();
		HttpURLConnection connection = client.getConnection(request);

		assertTrue(connection instanceof HttpsURLConnection);
		assertEquals(sslSocketFactory, ((HttpsURLConnection) connection).getSSLSocketFactory());
	}

	@Test
	public void testHttpClient_getConnection_doesNotUseSSLSocketFactoryForNonHttpsRequests() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		HttpRequest<String> request = simpleRequest();
		HttpURLConnection connection = client.getConnection(request);

		assertFalse(connection instanceof HttpsURLConnection);
	}

	@Test
	public void testHttpClient_getConnection_usesRequestHeaderValueOverItsOwn() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
		client.setUserAgent("Client User Agent");

		HttpRequest<String> request = simpleRequest();
		request.headers().header(USER_AGENT, "Request User Agent");

		HttpURLConnection connection = client.getConnection(request);

		assertEquals("Request User Agent", connection.getRequestProperty("User-Agent"));
	}

	@Test
	public void testHttpClient_getConnection_returnsNullIfNoContentTypeSet() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		HttpRequest<String> request = simpleRequest();
		HttpURLConnection connection = client.getConnection(request);

		assertNull(connection.getRequestProperty(CONTENT_TYPE.toString()));
	}

	@Test
	public void testHttpClient_getConnection_setsDefaultConnectTimeoutWhenNoTimeoutIsSet() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		HttpRequest<String> request = simpleRequest();
		HttpURLConnection connection = client.getConnection(request);

		assertEquals(30000, connection.getConnectTimeout());
	}

	@Test
	public void testHttpClient_getConnection_setsConnectTimeout() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		client.setConnectTimeout(50000);

		HttpRequest<String> request = simpleRequest();
		HttpURLConnection connection = client.getConnection(request);
		assertEquals(50000, connection.getConnectTimeout());
	}

	@Test
	public void testHttpClient_getConnection_setsDefaultReadTimeoutWhenNoTimeoutIsSet() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		HttpRequest<String> request = simpleRequest();
		HttpURLConnection connection = client.getConnection(request);

		assertEquals(30000, connection.getReadTimeout());
	}

	@Test
	public void testHttpClient_getConnection_setsReadTimeoutWhenTimeoutIsSet() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		client.setReadTimeout(50000);

		HttpRequest<String> request = simpleRequest();
		HttpURLConnection connection = client.getConnection(request);

		assertEquals(50000, connection.getReadTimeout());
	}

	@Test
	public void testHttpClient_execute_setsHeadersFromRequest() throws IOException {
		HttpRequest<String> request = simpleRequest()
						.header("Key1", "Value1")
						.header(CONTENT_TYPE, "application/xml");

		stub(request, null);

		client.execute(request);
		verify(getRequestedFor(urlEqualTo("/"))
						.withHeader("Key1", equalTo("Value1"))
						.withHeader("Content-Type", equalTo("application/xml")));
	}

	@Test
	public void testHttpClient_execute_writesDataFromRequestIfPresent() throws IOException {
		HttpRequest<String> request = simpleRequest()
						.verb("POST")
						.requestBody("some data");

		stub(request, null);

		client.execute(request);
		verify(postRequestedFor(urlEqualTo("/"))
						.withRequestBody(containing("some data")));
	}

	@Test
	public void testHttpClient_execute_doesNotwriteDataFromRequestIfNotPresent() throws IOException {
		HttpRequest<String> request = simpleRequest()
						.verb("POST");
		stub(request, null);

		client.execute(request);
		verify(postRequestedFor(urlEqualTo("/"))
						.withRequestBody(equalTo("")));
	}

	@Test
	public void testHttpClient_execute_setsCommonHeaders() throws IOException {
		HttpRequest<String> request = simpleRequest();
		stub(request, null);

		client.execute(request);
		assertEquals(request.headers().header(USER_AGENT), "Java HTTP/1.1");
	}

	@Test
	public void testHttpClient_execute_doesNotSetCommonHeadersIfPresent() throws IOException {
		HttpRequest<String> request = simpleRequest();
		request.header(USER_AGENT, "Custom User Agent");

		stub(request, null);

		client.execute(request);

		assertEquals(request.headers().header(USER_AGENT), "Custom User Agent");
	}

	@Test
	public void testHttpClient_addInjector_usesCustomInjectors() throws IOException {
		client.addInjector(request -> {
			request.header("Idempotency-Id", "abcd-uuid");
		});

		HttpRequest<String> request = simpleRequest();
		stub(request, null);

		client.execute(request);

		assertEquals(request.headers().header("Idempotency-Id"), "abcd-uuid");
	}

	@Test
	public void testHttpClient_addInjector_withNull_doestNotAddNullInjector() {
		client.addInjector(null);
		assertEquals(1, client.mInjectors.size());
	}

	@Test
	public void testHttpClient_applyHeadersFromRequest_SetsHeaders() throws IOException {
		HttpRequest<String> request = simpleRequest();
		request.header(USER_AGENT, "Custom User Agent");
		HttpURLConnection connection = (HttpURLConnection) new URL(environment().baseUrl()).openConnection();

		client.applyHeadersFromRequest(request, connection);
		assertEquals(connection.getRequestProperty(USER_AGENT.toString()), "Custom User Agent");
	}

	@Test
	public void testHttpClient_parseResponseHeaders_returnsParsedHeaders() throws IOException {
		HttpURLConnection connection = mock(HttpURLConnection.class);

		Map<String, List<String>> responseHeaders = new HashMap<>();
		responseHeaders.put("key", Arrays.asList("value"));
		responseHeaders.put("another-key", Arrays.asList("another-value"));

		Map<String, List<String>> headerFields = mock(Map.class);
		when(headerFields.keySet()).thenReturn(new HashSet<>(Arrays.asList("key", "another-key")));
		when(connection.getHeaderFields()).thenReturn(headerFields);

		when(connection.getHeaderField(eq("key"))).thenReturn("value");
		when(connection.getHeaderField(eq("another-key"))).thenReturn("another-value");

		Headers actualResponse = client.parseResponseHeaders(connection);
		assertEquals("value", actualResponse.header("key"));
		assertEquals("another-value", actualResponse.header("another-key"));
	}

	@Test
	public void testHttpClient_doesNotManuallyDeserializeIfResponseTypeIsString() throws IOException {
		HttpRequest<String> request = simpleRequest();
		HttpResponse<String> response = HttpResponse.<String>builder()
						.statusCode(200)
						.result("Here's the response")
						.build();

		stub(request, response);

		HttpResponse<String> actualResponse = client.execute(request);
		assertEquals(200, actualResponse.statusCode());
		assertEquals("Here's the response", actualResponse.result());
	}

	@Test
	public void testHttpClient_callsDeserializeIfResponseTypeIsNotString() throws IOException {
		HttpRequest<Zoo> request = new HttpRequest<>("/", "POST", Zoo.class);

		Zoo zoo = new Zoo();
		zoo.numberOfAnimals = 10;
		zoo.name = "Brian Tree";

		HttpResponse<Zoo> response = HttpResponse.<Zoo>builder()
						.headers(new Headers().header("Content-Type", "application/json"))
						.statusCode(201)
						.result(zoo)
						.build();

		stub(request, response);

		HttpResponse<Zoo> actualResponse = client.execute(request);
		assertEquals(201, actualResponse.statusCode());
		assertEquals("Brian Tree", actualResponse.result().name);
		assertEquals(10, actualResponse.result().numberOfAnimals.intValue());
	}

	@Test
	public void testHttpClient_HttpClientSupportPatchVerb() throws IOException {
		HttpRequest<String> request = simpleRequest();
		request.verb("PATCH");

		HttpResponse<String> response = HttpResponse.<String>builder()
						.headers(new Headers().header("Content-Type", "application/json"))
						.statusCode(200)
						.build();

		stub(request, response);

		HttpResponse<String> actual = client.execute(request);
		assertEquals(200, actual.statusCode());
	}

	@Test
	public void testHttpClient_HttpClientWithHttpsURLConnectionSupportPatchVerb() throws IOException {
		client = new HttpClient(() -> "https://google.com");

		HttpRequest<String> request = simpleRequest();
		request.verb("PATCH");

		try {
			client.execute(request);
			fail("client.execute() with patch request did not throw, when we expected it to");
		} catch(HttpException exception) {
			assertTrue(exception.statusCode() >= 300);
		}
	}

	@Test
	public void testHttpClient_HttpClientLoadsFileDataWithoutBodyPresent() throws IOException {
		String uploadData = new String(fileData("fileupload_test_text.txt"));

		FileUploadRequest request = simpleFileRequest()
				.file("file_test_text", resource("fileupload_test_text.txt").toFile());

		stubFor(post(urlEqualTo("/file_upload")));

		client.execute(request);

		verify(postRequestedFor(urlEqualTo("/file_upload"))
				.withHeader("Content-Type", containing("multipart/form-data; boundary=boundary"))
				.withRequestBody(containing("Content-Disposition: form-data; name=\"file_test_text\"; filename=\"fileupload_test_text.txt\""))
				.withRequestBody(containing("Content-Type: text/plain"))
				.withRequestBody(containing(uploadData))
				.withRequestBody(containing("--boundary")));
	}

	@Test
	public void testHttpClient_HttpClientLoadsFileDataWithTextBodyPresent() throws IOException {
		String uploadData = new String(fileData("fileupload_test_text.txt"));

		FileUploadRequest request = simpleFileRequest()
				.file("file_test_text", resource("fileupload_test_text.txt").toFile())
				.formData("some_field_key", "form_field=\"some_field_value\"");

		stubFor(post(urlEqualTo("/file_upload")));

		client.execute(request);

		verify(postRequestedFor(urlEqualTo("/file_upload"))
				.withHeader("Content-Type", containing("multipart/form-data; boundary=boundary"))
				.withRequestBody(containing("Content-Disposition: form-data; name=\"file_test_text\"; filename=\"fileupload_test_text.txt\""))
				.withRequestBody(containing("Content-Type: text/plain"))
				.withRequestBody(containing(uploadData))
				.withRequestBody(containing("--boundary"))
				.withRequestBody(containing("Content-Disposition: form-data; name=\"some_field_key\""))
				.withRequestBody(containing("form_field=\"some_field_value\"")));
    }

    @Test
	public void testHttpClient_HttpClientLoadsFileDataWithBinaryBodyPresnt() throws IOException {
		FileUploadRequest request = simpleFileRequest()
				.file("binary_file", resource("fileupload_test_binary.jpg").toFile());

		stubFor(post(urlEqualTo("/file_upload")));

		client.execute(request);

		verify(postRequestedFor(urlEqualTo("/file_upload"))
				.withHeader("Content-Type", containing("multipart/form-data; boundary=boundary"))
				.withRequestBody(containing("Content-Disposition: form-data; name=\"binary_file\"; filename=\"fileupload_test_binary.jpg\""))
				.withRequestBody(containing("Content-Type: image/jpeg")));

		LoggedRequest loggedRequest = WireMock.getAllServeEvents().get(0).getRequest();
		byte[] imageData = fileData("fileupload_test_binary.jpg");
		assertTrue(byteArrayContains(loggedRequest.getBody(), imageData));
	}

	@Test
	public void testHttpClient_HttpClientThrowsExceptionWithNonMapBody() throws IOException {
		FileUploadRequest request = simpleFileRequest();
		request.requestBody(new Object());

		stub(request, null);

		try {
			client.execute(request);
			fail("Http client should have thrown for non-Map requestBody");
		} catch (IOException ioe) {
			assertEquals("Request requestBody must be Map<String, Object> when Content-Type is multipart/*", ioe.getMessage());
		}
	}

	@DataProvider(name = "getVerbs")
	public Object[][] getVerbs() {
		return new Object[][]{
						{GET},
						{POST},
						{PUT},
						{DELETE}
		};
	}

	@DataProvider(name = "getSuccessCode")
	public Object[][] getSuccessCode() {
		return new Object[][]{
						{HTTP_OK},
						{HTTP_CREATED},
						{HTTP_ACCEPTED},
						{HTTP_RESET},
						{HTTP_NO_CONTENT},
		};
	}

	@DataProvider(name = "getErrorCodesWithException")
	public Object[][] getErrorCodesWithException() {
		return new Object[][]{
						{HTTP_UNAUTHORIZED, HttpException.class},
		};
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

	private HttpRequest<String> simpleRequest() {
		return new HttpRequest<>("/", "GET", String.class);
	}

	private class SampleObject implements Serializable, Deserializable {

		public SampleObject() {}

		public String name;
		public Integer age;

		@Override
		public void deserialize(Map<String, Object> fields) {
			if (fields.containsKey("name")) {
				name = (String) fields.get("name");
			}

			if (fields.containsKey("age")) {
				age = (Integer) fields.get("age");
			}
		}

		@Override
		public void serialize(Map<String, Object> serialized) {
			if (age != null) {
				serialized.put("age", age);
			}

			if (name != null) {
				serialized.put("name", name);
			}
		}
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
