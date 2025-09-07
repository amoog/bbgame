package ee.loki.bbgame.rest;

import ee.loki.bbgame.BBGameConfig;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClientWrapper implements IHttpClient {
    private final HttpClient httpClient;

    public HttpClientWrapper(BBGameConfig gameConfig) {
        httpClient =HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(gameConfig.connectTimeout)
                .build();
    }

    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        return httpClient.send(request, responseBodyHandler);
    }
}
