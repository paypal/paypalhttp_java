package com.braintreepayments.http.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class JSONFormatter {

	/**
	 * Gson
	 */
	public static Gson GSON = new GsonBuilder()
			.create();
	/**
	 * Converts a Raw Type to JSON String
	 *
	 * @param <T>
	 *            Type to be converted
	 * @param t
	 *            Object of the type
	 * @return JSON representation
	 */
	public static <T> String toJSON(T t) {
		return GSON.toJson(t);
	}

	/**
	 * Converts a JSON String to object representation
	 *
	 * @param <T>
	 *            Type to be converted
	 * @param responseString
	 *            JSON representation
	 * @param clazz
	 *            Target class
	 * @return Object of the target type
	 */
	public static <T> T fromJSON(String responseString, Class<T> clazz) {
		T t;
		if (clazz.isAssignableFrom(responseString.getClass())) {
			t = clazz.cast(responseString);
		} else {
			t = GSON.fromJson(responseString, clazz);
		}
		return t;
	}
}
