package com.braintreepayments.http;

import com.braintreepayments.http.exceptions.HttpException;
import com.braintreepayments.http.internal.TLSSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class HttpClient {

	private SSLSocketFactory mSSLSocketFactory;
	private String mUserAgent;
	private int mConnectTimeout;
	private int mReadTimeout;
	private Environment mEnvironment;

	List<Injector> mInjectors;

	public HttpClient(Environment environment) {
		mReadTimeout =  (int) TimeUnit.SECONDS.toMillis(30);
		mConnectTimeout = mReadTimeout;
		mUserAgent = "Java HTTP/1.1"; // TODO: add version string to build.gradle
		mInjectors = new ArrayList<>();
		mEnvironment = environment;
		addInjector(this::injectStandardHeaders);

		try {
			mSSLSocketFactory = new TLSSocketFactory();
		} catch (SSLException e) {
			mSSLSocketFactory = null;
		}
	}

	/**
	 * Override this method in a custom subclass to use a custom connect timeout value.
	 */
	protected int getConnectTimeout() { return mConnectTimeout; }

	/**
	 * Override this method in a custom subclass to use a custom read timeout value.
	 */
	protected int getReadTimeout() { return mReadTimeout; }

	/**
	 * Override this method in a custom subclass to use a SSLSocketFactory.
	 */
	protected SSLSocketFactory getSSLSocketFactory() { return mSSLSocketFactory; }

	/**
	 * Override this method in a custom subclass to use a User Agent.
	 */
	protected String getUserAgent() { return mUserAgent; }

	protected Environment getEnvironment() { return mEnvironment; }

	public void setSSLSocketFactory(SSLSocketFactory factory) { mSSLSocketFactory = factory; }

	public void setUserAgent(String userAgent) { mUserAgent = userAgent; }

	public void setConnectTimeout(int connectTimeout) { mConnectTimeout = connectTimeout; }

	public void setReadTimeout(int readTimeout) { mReadTimeout = readTimeout; }

	public synchronized void addInjector(Injector injector) {
		if (injector != null) {
			mInjectors.add(injector);
		}
	}

	protected abstract String serializeRequestBody(HttpRequest request);

	protected abstract <T> T parseResponseBody(String responseBody, Class<T> responseClass);

	public <T> HttpResponse<T> execute(HttpRequest<T> request) throws IOException {
		for (Injector injector : mInjectors) {
			injector.inject(request);
		}

		HttpURLConnection connection = null;
		try {
			connection = getConnection(request);
			if (request.requestBody() != null) {
				connection.setDoOutput(true);
				writeOutputStream(connection.getOutputStream(), serializeRequestBody(request));
			}
			return parseResponse(connection, request.responseClass());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	void applyHeadersFromRequest(HttpRequest request, URLConnection connection) {
		for (String key: request.headers()) {
			connection.setRequestProperty(key, request.headers().header(key));
		}
	}

	HttpURLConnection getConnection(HttpRequest request) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(mEnvironment.baseUrl() + request.path()).openConnection();
		applyHeadersFromRequest(request, connection);

		if (connection instanceof HttpsURLConnection) {
			if (mSSLSocketFactory == null) {
				throw new SSLException("SSLSocketFactory was not set or failed to initialize");
			}

			((HttpsURLConnection) connection).setSSLSocketFactory(getSSLSocketFactory());
		}

		connection.setReadTimeout(getReadTimeout());
		connection.setConnectTimeout(getConnectTimeout());
		connection.setRequestMethod(request.verb().toUpperCase());

		return connection;
	}

	private void writeOutputStream(OutputStream outputStream, String data) throws IOException {
		Writer out = new OutputStreamWriter(outputStream, UTF_8);
		out.write(data, 0, data.length());
		out.flush();
		out.close();
	}

	Headers parseResponseHeaders(URLConnection connection) {
		Headers headers = new Headers();
		for (int i = 0; i < connection.getHeaderFields().size(); i++) {
			headers.header(connection.getHeaderFieldKey(i), connection.getHeaderField(i));
		}

		return headers;
	}

	<T> HttpResponse<T> parseResponse(HttpURLConnection connection, Class<T> responseClass) throws IOException {
		boolean gzip = "gzip".equals(connection.getContentEncoding());

		Headers responseHeaders = parseResponseHeaders(connection);
		String responseBody;
		int statusCode;
		statusCode = connection.getResponseCode();
		if (statusCode >= HTTP_OK && statusCode <= HTTP_PARTIAL) {
			responseBody = readStream(connection.getInputStream(), gzip);
			return HttpResponse.<T>builder()
					.headers(responseHeaders)
					.statusCode(statusCode)
					.result(parseResponseBody(responseBody, responseClass))
					.build();
		} else {
			responseBody = readStream(connection.getErrorStream(), gzip);
			throw new HttpException(responseBody, statusCode, responseHeaders);
		}
	}

	private String readStream(InputStream in, boolean gzip) throws IOException {
		if (in == null) {
			return null;
		}

		try {
			if (gzip) {
				in = new GZIPInputStream(in);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int count; (count = in.read(buffer)) != -1; ) {
				out.write(buffer, 0, count);
			}

			return new String(out.toByteArray(), UTF_8);
		} finally {
			try {
				in.close();
			} catch (IOException ignored) {}
		}
	}

	private void injectStandardHeaders(HttpRequest request) throws IOException {
		request.headers()
				.headerIfNotPresent(Headers.USER_AGENT, getUserAgent());
	}
}
