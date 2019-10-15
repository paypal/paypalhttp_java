package com.paypal.http;

import java.io.IOException;

public interface Injector {
	void inject(HttpRequest request) throws IOException;
}
