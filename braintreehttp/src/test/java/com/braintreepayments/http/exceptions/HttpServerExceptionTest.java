package com.braintreepayments.http.exceptions;

import com.braintreepayments.http.Headers;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HttpServerExceptionTest {

	@Test
	public void testHttpServerException_withJsonBody_ParsesIntoDetails() {
		String errorResponse = "{\"debug_id\":\"debug\",\"information_link\":\"http://info.com\",\"message\":\"message\",\"name\":\"name\"}";
		APIException serverException = new APIException(errorResponse, 400, new Headers());

		assertEquals(serverException.getMessage(), errorResponse);
	}

	@Test
	public void testHttpServerException_withoutJsonBody_doesNotParseIntoDetails() {
		String errorResponse = "<ns1:XMLFault xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\"><ns1:faultstring xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">java.lang.NullPointerException</ns1:faultstring></ns1:XMLFault>";
		APIException serverException = new APIException(errorResponse, 400, new Headers());

		assertEquals(serverException.getMessage(), errorResponse);
	}
}
