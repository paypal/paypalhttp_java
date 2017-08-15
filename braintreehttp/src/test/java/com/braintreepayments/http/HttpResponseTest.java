package com.braintreepayments.http;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class HttpResponseTest {

    @Test
    public void testHttpResponse_headerNotNullEvenIfSet() {
        HttpResponse<String> response = new HttpResponse<>(null, 100, "data");
        assertNotNull(response.headers());
    }
}
