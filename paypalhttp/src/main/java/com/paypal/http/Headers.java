package com.paypal.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Headers implements Iterable<String> {

	public static final String CONTENT_TYPE = "content-type";
	public static final String AUTHORIZATION = "authorization";
	public static final String USER_AGENT = "user-agent";
	public static final String ACCEPT_ENCODING = "accept-encoding";

	@Override
	public Iterator<String> iterator() {
		return mHeaders.keySet().iterator();
	}

	protected Map<String, String> mHeaders = new HashMap<String, String>();
	private Map<String, String> keyMapping = new HashMap<String,String >();


	public Headers header(String header, String value) {
		if(value!=null && header!=null)
			value = header.equalsIgnoreCase("content-type") ? value.toLowerCase() : value;
		mHeaders.put(header, value);
		keyMapping.put(header!=null? header.toLowerCase(): null,header);
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
		keyMapping.remove(key.toLowerCase());
		return this;
	}

	public String header(String key) {
		String caseKey = keyMapping.get(key.toLowerCase());
		if(caseKey!= null){
			return mHeaders.get(caseKey);
		}else {
			return null;
		}

	}
}
