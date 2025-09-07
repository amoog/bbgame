package ee.loki.bbgame.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.loki.bbgame.BBGameConfig;
import ee.loki.bbgame.gamemodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BBGameClient {
    private static final Logger logger = LoggerFactory.getLogger(BBGameClient.class);
    private final BBGameConfig gameConfig;

    private final IHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private enum Method {
        GET,
        POST,
    }

    public BBGameClient(BBGameConfig gameConfig, ObjectMapper objectMapper, IHttpClient httpClient) {
        this.gameConfig = gameConfig;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    private <T> T runRequest(Method method, Class<T> valueType, String path, String... args) throws IOException, InterruptedException {
        urlEncodeArgs(args);
        var requestPath = String.format(path, (Object[]) args);
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(gameConfig.serverUrl + requestPath))
                .header("Accept", "application/json")
                .timeout(gameConfig.readTimeout);

        if (method == Method.GET) {
            requestBuilder.GET();
        } else {
            requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
        }

        var request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("Received raw response {} from server: {}", response.statusCode(), response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Response status code: " + response.statusCode() + "-" + response.body());
        } else {
            var result = objectMapper.readValue(response.body(), valueType);
            logger.debug("Received response: {}", result);
            return result;
        }
    }

    private void urlEncodeArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                args[i] = URLEncoder.encode(args[i], StandardCharsets.UTF_8);
            }
        }
    }

    public NewGame startNewGame() throws IOException, InterruptedException {
        var result = runRequest(Method.POST, NewGame.class,"/game/start");
        logger.info("Started new game: {}", result.gameId());
        return result;
    }

    public Reputation getReputation(String gameId) throws IOException, InterruptedException {
        return runRequest(Method.POST, Reputation.class, "/%s/investigate/reputation", gameId);
    }

    public ShopItem[] getShopItems(String gameId) throws IOException, InterruptedException {
        return runRequest(Method.GET, ShopItem[].class, "/%s/shop", gameId);
    }

    public ShoppingResult tryBuyItem(String gameId, String itemId) throws IOException, InterruptedException {
        return runRequest(Method.POST, ShoppingResult.class, "/%s/shop/buy/%s", gameId, itemId);
    }

    public List<Task> getTasks(String gameId) throws IOException, InterruptedException {
        var tasks= new ArrayList<Task>(Arrays.asList(runRequest(Method.GET, Task[].class, "/%s/messages", gameId)));
        tasks.removeIf(Task::encryptionUnknown);
        return tasks;
    }

    public TaskResult tryResolveTask(String gameId, String adId) throws IOException, InterruptedException {
        return runRequest(Method.POST, TaskResult.class, "/%s/solve/%s", gameId, adId);
    }
}
