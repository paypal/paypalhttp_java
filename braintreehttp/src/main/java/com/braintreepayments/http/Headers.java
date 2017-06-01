package com.braintreepayments.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Headers implements Iterable<String> {

	public static final String CONTENT_TYPE = "Content-Type";
	public static final String AUTHORIZATION = "Authorization";
	public static final String USER_AGENT = "User-Agent";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";

	@Override
	public Iterator<String> iterator() {
		return mHeaders.keySet().iterator();
	}

	protected Map<String, String> mHeaders = new HashMap<String, String>();

	public Headers header(String header, String value) {
		mHeaders.put(header, value);
		return this;
	}

	public Headers headerIfNotPresent(String key, String value) {
		if (header(key) == null) {
			return header(key, value);
		}
		return this;
	}

	public Headers remove(String key) {
		mHeaders.remove(key);
		return this;
	}

	public String header(String key) {
		return mHeaders.get(key);
	}
}
