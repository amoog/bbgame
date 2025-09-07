package ee.loki.bbgame.rest;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// httpclient is tricky to test, this interface is for mocking httpclient
public interface IHttpClient {
    <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException;
}
