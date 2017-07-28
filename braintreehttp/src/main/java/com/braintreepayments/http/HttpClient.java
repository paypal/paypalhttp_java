package com.braintreepayments.http;

import com.braintreepayments.http.exceptions.HttpException;
import com.braintreepayments.http.internal.TLSSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpClient {

	private SSLSocketFactory mSSLSocketFactory;
	private String mUserAgent;
	private int mConnectTimeout;
	private int mReadTimeout;
	private Environment mEnvironment;
	private Encoder encoder;

	public static final String CRLF = "\r\n";

	List<Injector> mInjectors;

	public HttpClient(Environment environment) {
		mReadTimeout =  (int) TimeUnit.SECONDS.toMillis(30);
		mConnectTimeout = mReadTimeout;
		mUserAgent = "Java HTTP/1.1"; // TODO: add version string to build.gradle
		mInjectors = new ArrayList<>();
		mEnvironment = environment;
		encoder = new Encoder();
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

	protected String serializeRequest(HttpRequest request) throws IOException {
		return encoder.encode(request);
	}

	protected <T> T deserializeResponse(String responseBody, Class<T> responseClass, Headers headers) throws IOException {
		return encoder.decode(responseBody, responseClass, headers);
	}

	public <T> HttpResponse<T> execute(HttpRequest<T> request) throws IOException {
		for (Injector injector : mInjectors) {
			injector.inject(request);
		}

		HttpURLConnection connection = null;
		try {
			connection = getConnection(request);

			if (request.requestBody() != null) {
				connection.setDoOutput(true);

				String contentType = request.headers().header(Headers.CONTENT_TYPE);
				if (contentType != null && contentType.startsWith("multipart/")) {
					if (!(request.requestBody() instanceof Map)) {
						throw new IOException("Request requestBody must be Map<String, Object> when Content-Type is multipart/*");
					} else {
						String boundary = "boundary" + System.currentTimeMillis();
						contentType = contentType + "; boundary=" + boundary;
						connection.setRequestProperty(Headers.CONTENT_TYPE, contentType); // Rewrite header with boundary

						Map<String, Object> body = (Map<String, Object>) request.requestBody();
						for (String key : body.keySet()) {
							Object value = body.get(key);
							if (value instanceof File) {
								addFilePart(connection.getOutputStream(), key, (File) value, boundary);
							} else {
								addFormPart(connection.getOutputStream(), key, String.valueOf(value), boundary);
							}
						}

						writeOutputStream(connection.getOutputStream(), "--" + boundary + "--");
						writeOutputStream(connection.getOutputStream(), CRLF);
						writeOutputStream(connection.getOutputStream(), CRLF);
					}
				} else {
					String data;
					if (request.requestBody() instanceof String) {
						data = (String) request.requestBody();
					} else {
						data = serializeRequest(request);
					}

					writeOutputStream(connection.getOutputStream(), data);
				}
			}
			return parseResponse(connection, request.responseClass());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private void writePartHeader(OutputStream writer, String name, String filename, String boundary) throws IOException {
		writeOutputStream(writer,"--" + boundary);
		writeOutputStream(writer, CRLF);
		writeOutputStream(writer, "Content-Disposition: form-data; name=\"" + name + "\"");
		if (filename != null) {
			writeOutputStream(writer, "; filename=\"" + filename + "\"");
			writeOutputStream(writer, CRLF);
			writeOutputStream(writer, "Content-Type: " + URLConnection.guessContentTypeFromName(filename));
		}
		writeOutputStream(writer, CRLF);
		writeOutputStream(writer, CRLF);
	}

	private void addFormPart(OutputStream writer, String key, String value, String boundary) throws IOException {
		writePartHeader(writer, key, null, boundary);

		writeOutputStream(writer, value);
		writeOutputStream(writer, CRLF);
	}

	private void addFilePart(OutputStream writer, String key, File uploadFile, String boundary)
			throws IOException {
		String filename = uploadFile.getName();
		writePartHeader(writer, key, filename, boundary);

		new FileInputStream(uploadFile).getChannel()
				.transferTo(0, uploadFile.length(), Channels.newChannel(writer));

		writeOutputStream(writer, CRLF);
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

		setRequestVerb(request.verb(), connection);

		return connection;
	}

	/**
	 * Workaround for a bug in {@code HttpURLConnection.setRequestMethod(String)}
	 * The implementation of Sun/Oracle is throwing a {@code ProtocolException}
	 * when the method is other than the HTTP/1.1 default methods. So to use {@code PATCH}
	 * and others, we must apply this workaround.
	 *
	 * See issue https://bugs.openjdk.java.net/browse/JDK-7016595
	 */
	private void setRequestVerb(String verb, HttpURLConnection connection) {
		try {
			connection.setRequestMethod(verb.toUpperCase());
		} catch (ProtocolException ignored) {
			try {
				Field delegateField = connection.getClass().getDeclaredField("delegate");
				delegateField.setAccessible(true);
				HttpURLConnection delegateConnection = (HttpURLConnection) delegateField.get(connection);

				setRequestVerb(verb, delegateConnection);
			} catch (NoSuchFieldException e) {
				Field methodField = null;
				Class connectionClass = connection.getClass();
				while (methodField == null) {
					try {
						methodField = connectionClass.getDeclaredField("method");
						methodField.setAccessible(true);
						methodField.set(connection, "PATCH");
					} catch (IllegalAccessException | NoSuchFieldException _ignored) {
						connectionClass = connectionClass.getSuperclass();
					}
				}
			} catch (IllegalAccessException ignoredIllegalAccess) {}
		}
	}

	private void writeOutputStream(OutputStream outputStream, String data) throws IOException {
		writeOutputStream(outputStream, data.getBytes());
	}

	private void writeOutputStream(OutputStream outputStream, byte[] data) throws IOException {
		outputStream.write(data);
	}

	Headers parseResponseHeaders(URLConnection connection) {
		Headers headers = new Headers();
		for (String key : connection.getHeaderFields().keySet()) {
			headers.header(key, connection.getHeaderField(key));
		}

		return headers;
	}

	private <T> HttpResponse<T> parseResponse(HttpURLConnection connection, Class<T> responseClass) throws IOException {
		boolean gzip = "gzip".equals(connection.getContentEncoding());

		Headers responseHeaders = parseResponseHeaders(connection);
		String responseBody;
		int statusCode;
		statusCode = connection.getResponseCode();
		if (statusCode >= HTTP_OK && statusCode <= HTTP_PARTIAL) {
			responseBody = readStream(connection.getInputStream(), gzip);

			T deserializedResponse = null;
			if (responseBody.length() > 0 && !Void.class.isAssignableFrom(responseClass)) {
				if (responseClass.isAssignableFrom(responseBody.getClass())) {
					deserializedResponse = (T) responseBody;
				} else {
					deserializedResponse = deserializeResponse(responseBody, responseClass, responseHeaders);
				}
			}

			return HttpResponse.<T>builder()
					.headers(responseHeaders)
					.statusCode(statusCode)
					.result(deserializedResponse)
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
