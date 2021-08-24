package com.paypal.http;

import org.testng.annotations.Test;

import java.util.Iterator;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class HeadersTest {

	@Test
	public void testHeaders_setHeader_setsHeaderCorrectly() {
		Headers h = new Headers();
		h.header("key", "val");
		assertEquals(h.header("key"), "val");
	}

	@Test
	public void testHeaders_setHeader_setsContentTypeCorrectly_case_insensitive_value() {
		Headers h = new Headers();
		h.header("content-type", "VAL");
		assertEquals(h.header("content-type"), "val");
	}

	@Test
	public void testHeaders_setHeader_setsContentTypeCorrectly_case_insensitive_key() {
		Headers h = new Headers();
		h.header("Content-Type", "VAL");
		assertEquals(h.header("Content-Type"), "val");
	}

	@Test
	public void testHeaders_removeHeader_removesHeaderCorrectly() {
		Headers h = new Headers();
		h.header("key", "val");
		assertEquals(h.header("key"), "val");
		h.remove("key");
		assertNull(h.header("not_present"));
	}

	@Test
	public void testHeaders_implementsIterable_iteratesAllKeys() {
		Headers h = new Headers()
				.header("key0", "application/test")
				.header("key1", "value1")
				.header("key2", "value2");

		int i = 0;
		Iterator<String> iterator = h.iterator();
		while (iterator.hasNext()) {
			iterator.next();
			i++;
		}
		assertEquals(3, i);
	}
}
