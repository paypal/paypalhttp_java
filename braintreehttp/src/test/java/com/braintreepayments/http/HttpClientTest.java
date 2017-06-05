package com.braintreepayments.http;

import com.braintreepayments.http.exceptions.HttpException;
import com.braintreepayments.http.internal.TLSSocketFactory;
import com.braintreepayments.http.utils.BasicWireMockHarness;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.braintreepayments.http.Headers.CONTENT_TYPE;
import static com.braintreepayments.http.Headers.USER_AGENT;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.*;
import static java.net.HttpURLConnection.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

public class HttpClientTest extends BasicWireMockHarness {

    private HttpClient client = null;
    private Environment httpsEnvironment = () -> "https://localhost";

    private static class JsonHttpClient extends HttpClient {

		public JsonHttpClient(Environment environment) {
			super(environment);
		}

		@Override
		protected String serializeRequest(HttpRequest request) {
			return new Gson().toJson(request.requestBody());
		}

		@Override
		protected <T> T deserializeResponse(String responseBody, Class<T> responseClass, Headers headers) throws UnsupportedEncodingException {
			String contentType = headers.header("Content-Type");
			if (contentType.equals("application/json")) {
				return new Gson().fromJson(responseBody, responseClass);
			} else {
				throw new UnsupportedEncodingException("Unsupported Content-Type: " + contentType);
			}
		}
	}

    @BeforeMethod
    public void setup() {
    	super.setup();
        client = new JsonHttpClient(environment());
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
        client = new JsonHttpClient(httpsEnvironment);
		client.setSSLSocketFactory(null);

        HttpRequest<String> request = simpleRequest();

		client.execute(request);
    }

    @Test
    public void testHttpClient_getConnection_usesSSLSocketFactory() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    	client = new JsonHttpClient(httpsEnvironment);

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
        HttpURLConnection connection = client.getConnection( request);

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
        HttpURLConnection connection = client.getConnection( request);

        assertNull(connection.getRequestProperty(CONTENT_TYPE.toString()));
    }

    @Test
    public void testHttpClient_getConnection_setsDefaultConnectTimeoutWhenNoTimeoutIsSet() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

        assertEquals(30000, connection.getConnectTimeout());
    }

    @Test
    public void testHttpClient_getConnection_setsConnectTimeout() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		client.setConnectTimeout(50000);

        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);
        assertEquals(50000, connection.getConnectTimeout());
    }

    @Test
    public void testHttpClient_getConnection_setsDefaultReadTimeoutWhenNoTimeoutIsSet() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

        assertEquals(30000, connection.getReadTimeout());
    }

    @Test
    public void testHttpClient_getConnection_setsReadTimeoutWhenTimeoutIsSet() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		client.setReadTimeout(50000);

        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

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
        when(connection.getHeaderField(anyInt())).thenReturn("value");
        when(connection.getHeaderFieldKey(anyInt())).thenReturn("key");

        when(connection.getHeaderFields()).thenReturn(new HashMap<String, List<String>>() {{
            put("key", new ArrayList<String>() {{ add("value"); }});
        }});

        Headers actualResponse = client.parseResponseHeaders(connection);
        assertEquals("value", actualResponse.header("key"));
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
		HttpRequest<SampleObject> request = new HttpRequest<>("/", "POST", SampleObject.class);

		SampleObject responsebody = new SampleObject();
		responsebody.name = "Brian Tree";
		responsebody.age = 10;

		HttpResponse<SampleObject> response = HttpResponse.<SampleObject>builder()
				.headers(new Headers().header("Content-Type", "application/json"))
				.statusCode(201)
				.result(responsebody)
				.build();

		stub(request, response);

		HttpResponse<SampleObject> actualResponse = client.execute(request);
		assertEquals(201, actualResponse.statusCode());
		assertEquals("Brian Tree", actualResponse.result().name);
		assertEquals(10, actualResponse.result().age);
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

    private HttpRequest<String> simpleRequest() {
    	return new HttpRequest<>("/", "GET", String.class);
	}

	private class SampleObject {

    	@SerializedName("name")
    	public String name;

		@SerializedName("age")
    	public int age;
	}
}
