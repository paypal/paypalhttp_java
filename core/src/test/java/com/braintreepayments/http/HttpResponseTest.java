package com.braintreepayments.http;

import com.braintreepayments.http.utils.TestSerializer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class HttpResponseTest {

    @Test
    public void testHttpResponse_headerNotNullEvenIfSet() {
        HttpResponse<String> response = HttpResponse.<String>builder()
                .result("data")
                .statusCode(100)
                .headers(new Headers())
                .build();
        assertNotNull(response.headers());
    }

    @Test
	public void testHttpResponse_deserialize() {
    	String testResponse = "{\"name\":\"test\",\"value\":100, \"is_good\":true}";
    	HttpResponse<TestSerializer> response = HttpResponse.deserialize(new Headers().header("Content-Type", "application/json"),
				200, testResponse, TestSerializer.class);

    	assertEquals(response.result().getName(), "test");
		assertEquals(response.result().getValue(), 100);
		assertTrue(response.result().isGood());
	}

	@Test
	public void testHttpResponse_deserialize_stringResponseUntransformed() {
		String testResponse = "{\"name\":\"test\",\"value\":100, \"is_good\":true}";
		HttpResponse<String> response = HttpResponse.deserialize(new Headers(),
				200, testResponse, String.class);

		assertEquals(response.result(), testResponse);
	}
}
