## PayPal HttpClient

PaypalHttp is a generic HTTP Client.

In it's simplest form, an [`HttpClient`](./paypalhttp/src/main/java/com/paypal/http/HttpClient.java) exposes an `#execute` method which takes an `HttpRequest`, executes it against the domain described in an `Environment`, and returns an `HttpResponse`. It throws an `IOException` if anything goes wrong during the course of execution.

### Environment

An [`Environment`](./paypalhttp/src/main/java/com/paypal/http/Environment.java) describes a domain that hosts a REST API, against which an `HttpClient` will make requests. `Environment` is a simple interface that wraps one method, `#baseUrl`.

```java
Environment env = () -> "https://example.com";
```

### Requests

[`HttpRequest`](./paypalhttp/src/main/java/com/paypal/http/HttpRequest.java)s contain all the information needed to make an HTTP request against the REST API. Specifically, one request describes a path, a verb, any path/query/form parameters, headers, attached files for upload, and body data. This class also holds a reference to the type of the response for deserializtion, if a structured response is expected.

### Responses

[`HttpResponse`](./paypalhttp/src/main/java/com/paypal/http/HttpResponse.java)s contain information returned by a server in response to a request as described above. They contain a status code, headers, and any data returned by the server, deserialized in accordance with the type in the `HttpRequest` from which this reponse originated.

```java
HttpRequest<MyResponsePojo> req = new HttpRequest("/path/to/resource", "GET", MyResponsePojo.class);

HttpResponse<MyResponsePojo> resp = client.execute(req);

Integer statusCode = resp.statusCode();
Headers headers = resp.headers();
MyResponsePojo responseData = resp.result();
```

### Injectors

[`Injector`](./paypalhttp/src/main/java/com/paypal/http/Injector.java)s wrap closures that can be used for executing arbitrary pre-flight logic, such as modifying a request or logging data. `Injector`s are attached to an `HttpClient` using the `#addInjector` method.

The HttpClient executes its `Injector`s in a first-in, first-out order, before each request.

```java
HttpClient client = new HttpClient(env);

client.addInjector(req -> {
  log.log(req);
});

client.addInjector(req -> {
  req.headers().header("Request-Id", "abcd");
});

...
```

### Error Handling

`HttpClient#execute` may throw an `IOException` if something goes wrong during the course of execution. If the server returns a non-200 response, this execption will be an instance of [`HttpException`](./paypalhttp/src/main/java/com/paypal/http/exceptions/HttpException.java) that will contain a status code and headers that you can use for debugging. 

```java
try {
  HttpResponse<MyResponsePojo> resp = client.execute(req);
} catch(IOException ioe) {
  if (ioe instanceof HttpException) {
    // Inspect this exception for details
    HttpException he = (HttpException) ioe;
    int statusCode = ioe.statusCode();
  } else {
    // Something else went wrong
  }
}
```

### Serializer
(De)Serialization of request and response data is performed by implementing the [`Serializer`](./paypalhttp/src/main/java/com/paypal/http/serializer/Serializer.java) interface. PaypalHttp currently supports `json` encoding out of the box.

### SSL

By default, PaypalHttp will use the built-in `TLSSoccketFactory` when connecting to URLs that use `https` as their scheme. If you'd like to do cert-pinning, or use a different SSL implementation, you can provide your own `SSLSocketFactory` via `HttpClient#setSSLSocketFactory()`.

## License

PaypalHttp-Java is open source and available under the MIT license. See the [LICENSE](./LICENSE) file for more info.


## Contributions
Pull requests and new issues are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for details.
