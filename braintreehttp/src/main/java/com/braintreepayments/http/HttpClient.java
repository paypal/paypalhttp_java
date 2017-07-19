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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class HttpClient {

	private SSLSocketFactory mSSLSocketFactory;
	private String mUserAgent;
	private int mConnectTimeout;
	private int mReadTimeout;
	private Environment mEnvironment;
	private Gson mGson;

    public static final String LINE_FEED = "\r\n";

	List<Injector> mInjectors;

	public HttpClient(Environment environment) {
		mGson = new Gson();
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

	protected abstract String serializeRequest(HttpRequest request) throws IOException;

	protected abstract <T> T deserializeResponse(String responseBody, Class<T> responseClass, Headers headers) throws UnsupportedEncodingException;

    public <T> HttpResponse<T> execute(HttpRequest<T> request) throws IOException {
        for (Injector injector : mInjectors) {
            injector.inject(request);
        }

        HttpURLConnection connection = null;
        try {
            connection = getConnection(request);

            if (request.body() != null || request.file() != null) {
                String data;
                connection.setDoOutput(true);

                if (request.file() == null) { // Only body data
                    if (request.body() instanceof String) {
                        data = (String) request.body();
                    } else {
                        data = serializeRequest(request);
                    }
                } else { // A file, and maybe a body
                    StringWriter writer = new StringWriter();
                    String boundary = "boundary" + System.currentTimeMillis();

                    String json = this.mGson.toJson(request.body());
                    JsonObject obj = this.mGson.fromJson(json, JsonElement.class).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        String key = entry.getKey();
                        String value;
                        value = entry.getValue().toString();
                        addFormField(writer, key, value, boundary);
                    }

                    addFilePart("file", request.file(), writer, boundary);
                    writer.append("--" + boundary + "--").append(LINE_FEED);
                    writer.append(LINE_FEED);

                    data = writer.toString();

                    String contentType = "multipart/form-data; boundary=" + boundary;
                    connection.setRequestProperty("Content-Type", contentType);
                }

                writeOutputStream(connection.getOutputStream(), data);
            }

            return parseResponse(connection, request.responseClass());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void addFormField(StringWriter writer, String key, String value, String boundary) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + key + "\"").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
    }

    private void addFilePart(String fieldName, File uploadFile, StringWriter writer, String boundary)
        throws IOException {
        String filename = uploadFile.getName();

        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"").append(LINE_FEED);
        writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(filename)).append(LINE_FEED);
        writer.append(LINE_FEED);

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            writer.write(new String(buffer));
        }
        inputStream.close();

        writer.append(LINE_FEED);
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
		Writer out = new OutputStreamWriter(outputStream, UTF_8);
		out.write(data, 0, data.length());
		out.flush();
		out.close();
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
			if (responseBody.length() > 0) {
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
