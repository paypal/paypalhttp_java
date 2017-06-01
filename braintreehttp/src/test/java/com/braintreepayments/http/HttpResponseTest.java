package com.braintreepayments.http;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

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
}
