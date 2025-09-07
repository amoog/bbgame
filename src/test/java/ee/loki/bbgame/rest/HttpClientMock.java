package ee.loki.bbgame.rest;

import org.junit.jupiter.api.AssertionFailureBuilder;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HttpClientMock  implements IHttpClient {
    private final List<PlannedResponse> plannedResponses = new ArrayList<>();

    public static class PlannedResponse {
        private String bodyToReturn = null;
        private int httpStatusCodeToReturn = 200;
        private String urlToExpect = "";
        private String methodToExpect = "GET";
        private IOException exceptionToThrow = null;

        public PlannedResponse(int httpStatusCodeToReturn, String methodToExpect, String urlToExpect, String bodyToReturn) {
            this.httpStatusCodeToReturn = httpStatusCodeToReturn;
            this.urlToExpect = urlToExpect;
            this.methodToExpect = methodToExpect;
            this.bodyToReturn = bodyToReturn;
        }

        private PlannedResponse() {}
    }

    private PlannedResponse singleCall() {
        if (plannedResponses.isEmpty()) {
            var plannedResponse = new PlannedResponse();
            plannedResponses.add(plannedResponse);
            return plannedResponse;
        } else {
            return plannedResponses.getFirst();
        }
    }

    public void addPlannedResponse(PlannedResponse plannedResponse) {
        plannedResponses.add(plannedResponse);
    }

    public void clearPlannedResponses() {
        plannedResponses.clear();
    }

    public void setBodyToReturn(String bodyToReturn) {
        singleCall().bodyToReturn = bodyToReturn;
    }

    public void setHttpStatusCodeToReturn(int httpStatusCodeToReturn) {
        singleCall().httpStatusCodeToReturn = httpStatusCodeToReturn;
    }

    public void setExceptionToThrow(IOException exceptionToThrow) {
        singleCall().exceptionToThrow = exceptionToThrow;
    }

    public void setURLToExpect(String url) {
        singleCall().urlToExpect = url;
    }

    public void setMethodToExpect(String method) {
        singleCall().methodToExpect = method;
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
        if (plannedResponses.isEmpty()) {
            throw new IllegalStateException("No planned responses");
        }
        var nextResponse = plannedResponses.removeFirst();
        if (nextResponse.exceptionToThrow != null) {
            throw nextResponse.exceptionToThrow;
        }

        if (!nextResponse.methodToExpect.equals(request.method())) {
            AssertionFailureBuilder.assertionFailure()
                    .message("Method " + nextResponse.methodToExpect + " does not match request method " + request.method())
                    .buildAndThrow();
        }

        if (!nextResponse.urlToExpect.equals(request.uri().toString())) {
            AssertionFailureBuilder.assertionFailure()
                    .message("URL " + nextResponse.urlToExpect + " does not match request URL " + request.uri())
                    .buildAndThrow();
        }

        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return nextResponse.httpStatusCodeToReturn;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<T>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T body() {
                return (T) nextResponse.bodyToReturn;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return request.uri();
            }

            @Override
            public HttpClient.Version version() {
                return request.version().isPresent() ? request.version().get() : null;
            }
        };
    }
}
