package ee.loki.bbgame.rest;

import com.fasterxml.jackson.core.JsonParseException;
import ee.loki.bbgame.BBGameConfig;
import ee.loki.bbgame.BBGameMain;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class BBGameClientTest {
    private static final String testUrl = "http://localhost:666/testapi";
    private static final String testGameId = "testGameId";

    private final HttpClientMock httpClient;
    private final BBGameClient client;

    BBGameClientTest() {
        var objectMapper = BBGameMain.createObjectMapper();
        httpClient = new HttpClientMock();
        var props = new Properties();
        props.setProperty("serverUrl", testUrl);

        var config = new BBGameConfig(props);
        client = new BBGameClient(config, objectMapper, httpClient);
    }

    @Test
    void startNewGameCallCorrect() throws IOException, InterruptedException {
        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setMethodToExpect("POST");
        httpClient.setURLToExpect(testUrl+"/game/start");
        httpClient.setBodyToReturn(
                """
                {"gameId": "%s",
                "lives": 3,
                "gold": 4,
                "level": 5,
                "score": 6,
                "highScore": 0,
                "turn": 7}
                """.formatted(testGameId)
        );
        var result = client.startNewGame();
        assertNotNull(result);
        assertEquals(testGameId, result.gameId());
        assertEquals(3, result.lives());
        assertEquals(4, result.gold());
        assertEquals(5, result.level());
        assertEquals(6, result.score());
        assertEquals(0, result.highScore());
        assertEquals(7, result.turn());
    }

    @Test
    void getReputationCallCorrect() throws IOException, InterruptedException {
        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setMethodToExpect("POST");
        httpClient.setURLToExpect(testUrl+"/" + testGameId + "/investigate/reputation");
        httpClient.setBodyToReturn(
                """
                {
                "people": 1.2,
                "state": 2.1,
                "underworld": 0.1
                }
                """
        );
        var result = client.getReputation(testGameId);
        assertEquals(1.2, result.people());
        assertEquals(2.1, result.state());
        assertEquals(0.1, result.underworld());
    }

    @Test
    void getShopItemsCallCorrect() throws IOException, InterruptedException {
        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setMethodToExpect("GET");
        httpClient.setURLToExpect(testUrl+"/" + testGameId + "/shop");
        httpClient.setBodyToReturn(
                """
                [
                    {
                        "id": "hpot",
                        "name": "Healing potion",
                        "cost": 50
                    },
                    {
                        "id": "cs",
                        "name": "Claw Sharpening",
                        "cost": 100
                    },
                    {
                        "id": "gas",
                        "name": "Gasoline",
                        "cost": 100
                    }
                ]
                """
        );
        var result = client.getShopItems(testGameId);
        assertEquals(3, result.length);
        assertEquals("cs", result[1].id());
        assertEquals(100, result[2].cost());
    }

    @Test
    void tryBuyItemCallCorrect() throws IOException, InterruptedException {
        var testItemId = "testItemId";
        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setMethodToExpect("POST");
        httpClient.setURLToExpect(testUrl+"/" + testGameId + "/shop/buy/" + testItemId);
        httpClient.setBodyToReturn(
                """
                {
                    "shoppingSuccess": true,
                    "gold": 16,
                    "lives": 4,
                    "level": 0,
                    "turn": 3
                }
                """
        );
        var result = client.tryBuyItem(testGameId, testItemId);
        assertTrue(result.shoppingSuccess());
        assertEquals(16, result.gold());
        assertEquals(4, result.lives());
        assertEquals(0, result.level());
        assertEquals(3, result.turn());
    }

    @Test
    void getTasksCallCorrect() throws IOException, InterruptedException {
        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setMethodToExpect("GET");
        httpClient.setURLToExpect(testUrl+"/" + testGameId + "/messages");
        httpClient.setBodyToReturn(
                """
                [
                    {
                        "adId": "CPBAbShn",
                        "message": "Create an advertisement campaign for Hagar Eymor to promote their bucket based business",
                        "reward": 35,
                        "expiresIn": 6,
                        "encrypted": null,
                        "probability": "Hmmm...."
                    },
                    {
                        "adId": "Tv5wXFOY",
                        "message": "Create an advertisement campaign for Otilia Maddison to promote their clothes based business",
                        "reward": 35,
                        "expiresIn": 6,
                        "encrypted": null,
                        "probability": "Quite likely"
                    }
                ]
                """
        );
        var result = client.getTasks(testGameId);
        assertEquals(2, result.size());
        assertEquals("Tv5wXFOY", result.get(1).adId());
    }

    @Test
    void tryResolveTaskCallCorrectAndUrlEncodeWorks() throws IOException, InterruptedException {
        var testGameId = "test/GameId";
        var testGameIDEncoded = "test%2FGameId";
        var testAdId = "test AdId";
        var testAdIdEncoded = "test+AdId";

        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setMethodToExpect("POST");
        httpClient.setURLToExpect(testUrl+"/" + testGameIDEncoded + "/solve/" + testAdIdEncoded);
        httpClient.setBodyToReturn(
                """
                {
                "success": true,
                "lives": 3,
                "gold": 10,
                "score": 10,
                "highScore": 0,
                "turn": 2,
                "message": "You successfully solved the mission!"
                }
                """
        );
        var result = client.tryResolveTask(testGameId, testAdId);
        assertTrue(result.success());
        assertEquals(3, result.lives());
        assertEquals(10, result.gold());
        assertEquals(10, result.score());
        assertEquals(0, result.highScore());
        assertEquals(2, result.turn());
        assertEquals("You successfully solved the mission!", result.message());
    }

    @Test
    void not200StatusCodeThrows() {
        httpClient.setMethodToExpect("POST");
        httpClient.setURLToExpect(testUrl+"/game/start");
        httpClient.setHttpStatusCodeToReturn(400);

        assertThrows(RuntimeException.class, client::startNewGame);
    }

    @Test
    void invalidResponseThrows() {
        httpClient.setMethodToExpect("POST");
        httpClient.setURLToExpect(testUrl+"/game/start");
        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setBodyToReturn("not a json");

        assertThrows(JsonParseException.class, client::startNewGame);
    }

    @Test
    void ioExceptionThrows() {
        httpClient.setMethodToExpect("POST");
        httpClient.setExceptionToThrow(new IOException());
        httpClient.setURLToExpect(testUrl+"/game/start");
        httpClient.setHttpStatusCodeToReturn(200);
        httpClient.setBodyToReturn("not a json");

        assertThrows(IOException.class, client::startNewGame);
    }
}
