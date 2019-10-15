package com.paypal.http;

import com.paypal.http.exceptions.HttpException;
import com.paypal.http.internal.TLSSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.paypal.http.serializer.StreamUtils.writeOutputStream;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;

public class HttpClient {

	private SSLSocketFactory sslSocketFactory;
	private String userAgent;
	private int connectTimeout;
	private int readTimeout;
	private Environment environment;
	private Encoder encoder;

	List<Injector> mInjectors;

	public HttpClient(Environment environment) {
		this.readTimeout =  (int) TimeUnit.SECONDS.toMillis(30);
		this.connectTimeout = readTimeout;
		this.userAgent = "Java HTTP/1.1"; // TODO: add version string to build.gradle
		this.mInjectors = new ArrayList<>();
		this.environment = environment;
		this.encoder = new Encoder();

		addInjector(this::injectStandardHeaders);

		try {
			sslSocketFactory = new TLSSocketFactory();
		} catch (SSLException e) {
			sslSocketFactory = null;
		}
	}

	/**
	 * Override this method in a custom subclass to use a custom connect timeout value.
	 */
	protected int getConnectTimeout() { return connectTimeout; }

	/**
	 * Override this method in a custom subclass to use a custom read timeout value.
	 */
	protected int getReadTimeout() { return readTimeout; }

	/**
	 * Override this method in a custom subclass to use a SSLSocketFactory.
	 */
	protected SSLSocketFactory getSSLSocketFactory() { return sslSocketFactory; }

	/**
	 * Override this method in a custom subclass to use a User Agent.
	 */
	protected String getUserAgent() { return userAgent; }

	public Encoder getEncoder() { return encoder; }

	protected Environment getEnvironment() { return environment; }

	public void setSSLSocketFactory(SSLSocketFactory factory) { sslSocketFactory = factory; }

	public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

	public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }

	public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }

	public synchronized void addInjector(Injector injector) {
		if (injector != null) {
			mInjectors.add(injector);
		}
	}

	public <T> HttpResponse<T> execute(HttpRequest<T> request) throws IOException {
		HttpRequest<T> requestCopy = request.copy();

		for (Injector injector : mInjectors) {
			injector.inject(requestCopy);
		}

		HttpURLConnection connection = getConnection(requestCopy);
		try {
			return parseResponse(connection, requestCopy.responseClass());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private void applyHeadersFromRequest(HttpURLConnection connection, HttpRequest request) {
		for (String key: request.headers()) {
			connection.setRequestProperty(key, request.headers().header(key));
		}
	}

	HttpURLConnection getConnection(HttpRequest request) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(environment.baseUrl() + request.path()).openConnection();

		if (connection instanceof HttpsURLConnection) {
			if (sslSocketFactory == null) {
				String message = "SSLSocketFactory was not set or failed to initialize";
				System.out.println(message);
				throw new SSLException(message);
			}

			((HttpsURLConnection) connection).setSSLSocketFactory(getSSLSocketFactory());
		}

		connection.setReadTimeout(getReadTimeout());
		connection.setConnectTimeout(getConnectTimeout());

		setRequestVerb(request.verb(), connection);
		if (request.requestBody() != null) {
			connection.setDoOutput(true);
			byte[] data = encoder.serializeRequest(request);

			applyHeadersFromRequest(connection, request);
			writeOutputStream(connection.getOutputStream(), data);
		} else {
			applyHeadersFromRequest(connection, request);
		}

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

	Headers parseResponseHeaders(URLConnection connection) {
		Headers headers = new Headers();
		for (String key : connection.getHeaderFields().keySet()) {
			headers.header(key, connection.getHeaderField(key));
		}

		return headers;
	}

	private <T> HttpResponse<T> parseResponse(HttpURLConnection connection, Class<T> responseClass) throws IOException {
		Headers responseHeaders = parseResponseHeaders(connection);
		String responseBody;
		int statusCode;
		statusCode = connection.getResponseCode();
		if (statusCode >= HTTP_OK && statusCode <= HTTP_PARTIAL) {
			T deserializedResponse = null;

			if (!Void.class.isAssignableFrom(responseClass)) {
				deserializedResponse = encoder.deserializeResponse(connection.getInputStream(), responseClass, responseHeaders);
			}

			return new HttpResponse<>(responseHeaders, statusCode, deserializedResponse);
		} else {
			responseBody = encoder.deserializeResponse(connection.getErrorStream(), String.class, responseHeaders);
			throw new HttpException(responseBody, statusCode, responseHeaders);
		}
	}

	private void injectStandardHeaders(HttpRequest request) throws IOException {
		request.headers()
				.headerIfNotPresent(Headers.USER_AGENT, getUserAgent());
	}
}
