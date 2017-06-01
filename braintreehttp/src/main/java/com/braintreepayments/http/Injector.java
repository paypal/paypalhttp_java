package com.braintreepayments.http;

import java.io.IOException;

public interface Injector {
	void inject(HttpRequest request) throws IOException;
}
