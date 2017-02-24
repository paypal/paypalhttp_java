package com.braintreepayments.http;

import java.io.IOException;

public interface HttpClient {
    <T> HttpResponse<T> execute(HttpRequest<T> request) throws IOException;
}
