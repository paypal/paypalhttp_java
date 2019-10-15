package com.paypal.http.exceptions;

import com.paypal.http.Headers;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HttpExceptionTest {

	@Test
	public void testHttpServerException_withJsonBody_ParsesIntoDetails() {
		String errorResponse = "{\"debug_id\":\"debug\",\"information_link\":\"http://info.com\",\"message\":\"message\",\"name\":\"name\"}";
		HttpException httpException = new HttpException(errorResponse, 400, new Headers());

		assertEquals(httpException.getMessage(), errorResponse);
	}

	@Test
	public void testHttpServerException_withoutJsonBody_doesNotParseIntoDetails() {
		String errorResponse = "<ns1:XMLFault xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\"><ns1:faultstring xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">java.lang.NullPointerException</ns1:faultstring></ns1:XMLFault>";
		HttpException httpException = new HttpException(errorResponse, 400, new Headers());

		assertEquals(httpException.getMessage(), errorResponse);
	}
}
