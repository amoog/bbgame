package ee.loki.bbgame;

import ee.loki.bbgame.gamemodel.Task;
import ee.loki.bbgame.gamemodel.TaskResult;
import ee.loki.bbgame.rest.HttpClientMock;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BBGameActionsTest {
    private static final String testGameId = "testGameId";
    private static final int testMinimumLives = 9;
    private static final int testTargetScore = 999;

    @Test
    public void startGameSucceeds() throws IOException, InterruptedException {
        var httpClientMock = new HttpClientMock();
        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();
        var testlives = 2;
        var testgold = 3;
        var testlevel = 4;
        var testscore = 5;

        httpClientMock.setMethodToExpect("POST");
        httpClientMock.setHttpStatusCodeToReturn(200);
        httpClientMock.setURLToExpect(BBGameConfig.defaultServerUrl+"/game/start");
        httpClientMock.setBodyToReturn(
                """
                {"gameId": "%s",
                "lives": %d,
                "gold": %d,
                "level": %d,
                "score": %d,
                "highScore": 0,
                "turn": 0}
                """.formatted(testGameId, testlives, testgold, testlevel, testscore)
        );

        var gameContext = ctx.gameActions.startGame(testMinimumLives, testTargetScore);
        assertNotNull(gameContext);
        assertEquals(testGameId, gameContext.gameId);
        assertEquals(testlives, gameContext.lives);
        assertEquals(testgold, gameContext.gold);
        assertEquals(testlevel, gameContext.level);
        assertEquals(testscore, gameContext.score);
        assertEquals(testTargetScore, gameContext.targetScore);
        assertNotNull(gameContext.boughtUpgrades);
        assertTrue(gameContext.boughtUpgrades.isEmpty());
    }

    private final static String ad1 = "ad1";
    private final static String proability1 = "proability1";
    private final static String ad2 = "ad2";
    private final static String proability2 = "proability2";
    private final static String ad3 = "ad3";
    private final static String proability3 = "proability3";

    private Task createDummyTask(String probability) {
        return new Task(ad1, "", 0, 0, probability, 0);
    }

    private TaskResult createDummyResult() {
        return new TaskResult(true, 0, 0, 0, 0, 0, "");
    }

    private HttpClientMock setupNextTaskSelectHttpClientMock() {
        var httpClientMock = new HttpClientMock();
        httpClientMock.setHttpStatusCodeToReturn(200);
        httpClientMock.setMethodToExpect("GET");
        httpClientMock.setURLToExpect(BBGameConfig.defaultServerUrl+"/" + testGameId + "/messages");
        httpClientMock.setBodyToReturn(
                """
                [
                    {
                        "adId": "%s",
                        "message": "irrelevant",
                        "reward": 35,
                        "expiresIn": 6,
                        "encrypted": null,
                        "probability": "%s"
                    },
                    {
                        "adId": "%s",
                        "message": "irrelevant",
                        "reward": 35,
                        "expiresIn": 5,
                        "encrypted": null,
                        "probability": "%s"
                    },
                    {
                        "adId": "%s",
                        "message": "irrelevant",
                        "reward": 40,
                        "expiresIn": 6,
                        "encrypted": null,
                        "probability": "%s"
                    }
                ]
                """.formatted(ad1, proability1, ad2, proability2, ad3, proability3)
        );
        return httpClientMock;
    }

    @Test
    void selectNextTaskSelectsByExpiration() throws IOException, InterruptedException {
        // value of both ads should be 0 here as history is empty
        var httpClientMock = setupNextTaskSelectHttpClientMock();

        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();

        var gameContext = new GameContext(testGameId, testMinimumLives, testTargetScore);

        var nextTask = ctx.gameActions.selectNextTask(gameContext);

        assertNotNull(nextTask);
        assertEquals(ad2, nextTask.adId());
    }

    @Test
    void selectNextTaskSelectsByValue() throws IOException, InterruptedException {
        var httpClientMock = setupNextTaskSelectHttpClientMock();

        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();

        ctx.historyStore.addGameStep(testGameId, createDummyTask(proability1), createDummyResult());
        ctx.historyStore.addGameStep(testGameId, createDummyTask(proability3), createDummyResult());

        var gameContext = new GameContext(testGameId, testMinimumLives, testTargetScore);

        var nextTask = ctx.gameActions.selectNextTask(gameContext);

        assertNotNull(nextTask);
        assertEquals(ad3, nextTask.adId());
    }

    @Test
    void resolveTaskWorksCorrectly() throws IOException, InterruptedException {
        var newlives = 11;
        var newgold = 12;
        var newscore = 13;

        var httpClientMock = new HttpClientMock();
        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();

        httpClientMock.setHttpStatusCodeToReturn(200);
        httpClientMock.setMethodToExpect("POST");
        httpClientMock.setURLToExpect(BBGameConfig.defaultServerUrl+"/" + testGameId + "/solve/" + ad1);
        httpClientMock.setBodyToReturn(
                """
                {
                "success": true,
                "lives": %s,
                "gold": %s,
                "score": %s,
                "highScore": 0,
                "turn": 2,
                "message": "You successfully solved the mission!"
                }
                """.formatted(newlives, newgold, newscore)
        );

        var gameContext = new GameContext(testGameId, testMinimumLives, testTargetScore);

        ctx.gameActions.resolveTask(gameContext, createDummyTask(""));
        assertEquals(newlives, gameContext.lives);
        assertEquals(newgold, gameContext.gold);
        assertEquals(newscore, gameContext.score);
    }
    
    @Test
    void doShoppingBuysLivesCorrectly() throws IOException, InterruptedException {
        var httpClientMock = new HttpClientMock();
        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();

        var healthPotionPrice = 99;
        httpClientMock.addPlannedResponse(new HttpClientMock.PlannedResponse( 200, "GET",
                BBGameConfig.defaultServerUrl + "/" + testGameId + "/shop",
                        """
                [
                    {
                        "id": "%s",
                        "name": "Healing potion",
                        "cost": %d
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
                """.formatted(BBGameActions.healthPotionId, healthPotionPrice)
        ));
        httpClientMock.addPlannedResponse(new HttpClientMock.PlannedResponse( 200, "POST",
                BBGameConfig.defaultServerUrl + "/" + testGameId + "/shop/buy/" + BBGameActions.healthPotionId,
                """
        {
            "shoppingSuccess": true,
            "gold": %d,
            "lives": %d,
            "level": %d,
            "turn": 3
        }
        """.formatted(1,2,3)
        ));

        var gameContext = new GameContext(testGameId, testMinimumLives, testTargetScore);
        gameContext.gold = healthPotionPrice;
        gameContext.lives = gameContext.minimumLives - 1;

        ctx.gameActions.doShopping(gameContext);
        assertEquals(1, gameContext.gold);
        assertEquals(2, gameContext.lives);
        assertEquals(3, gameContext.level);
    }

    @Test
    void doShoppingBuysUpgradesCorrectly() throws IOException, InterruptedException {
        var httpClientMock = new HttpClientMock();
        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();

        var csPrice = 99;
        var gasPrice = csPrice + 1;
        var csId = "cs";

        httpClientMock.addPlannedResponse(new HttpClientMock.PlannedResponse( 200, "GET",
                BBGameConfig.defaultServerUrl + "/" + testGameId + "/shop",
                """
        [
            {
                "id": "%s",
                "name": "Healing potion",
                "cost": %d
            },
            {
                "id": "%s",
                "name": "Claw Sharpening",
                "cost": %d
            },
            {
                "id": "gas",
                "name": "Gasoline",
                "cost": %d
            }
        ]
        """.formatted(BBGameActions.healthPotionId, 1, csId, csPrice, gasPrice)
        ));
        httpClientMock.addPlannedResponse(new HttpClientMock.PlannedResponse( 200, "POST",
                BBGameConfig.defaultServerUrl + "/" + testGameId + "/shop/buy/" + csId,
                """
        {
            "shoppingSuccess": true,
            "gold": %d,
            "lives": %d,
            "level": %d,
            "turn": 3
        }
        """.formatted(3,4,5)
        ));

        var gameContext = new GameContext(testGameId, testMinimumLives, testTargetScore);
        gameContext.gold = csPrice;
        gameContext.lives = gameContext.minimumLives;

        ctx.gameActions.doShopping(gameContext);
        assertEquals(3, gameContext.gold);
        assertEquals(4, gameContext.lives);
        assertEquals(5, gameContext.level);
    }

    @Test
    void doShoppingRespectsAvailableGoldForHealth() throws IOException, InterruptedException {
        var httpClientMock = new HttpClientMock();
        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();

        var healthPotionPrice = 99;
        var csPrice = 99;
        var gasPrice = csPrice + 1;
        var csId = "cs";

        httpClientMock.addPlannedResponse(new HttpClientMock.PlannedResponse( 200, "GET",
                BBGameConfig.defaultServerUrl + "/" + testGameId + "/shop",
                """
        [
            {
                "id": "%s",
                "name": "Healing potion",
                "cost": %d
            },
            {
                "id": "%s",
                "name": "Claw Sharpening",
                "cost": %d
            },
            {
                "id": "gas",
                "name": "Gasoline",
                "cost": %d
            }
        ]
        """.formatted(BBGameActions.healthPotionId, healthPotionPrice, csId, csPrice, gasPrice)
        ));

        var gameContext = new GameContext(testGameId, testMinimumLives, testTargetScore);
        gameContext.gold = healthPotionPrice - 1;
        gameContext.lives = gameContext.minimumLives - 1;

        ctx.gameActions.doShopping(gameContext);

        assertEquals( healthPotionPrice - 1, gameContext.gold);
        assertEquals(gameContext.minimumLives - 1, gameContext.lives);
    }

    @Test
    void doShoppingRespectsAvailableGoldForUpgrades() throws IOException, InterruptedException {
        var httpClientMock = new HttpClientMock();
        var ctx = new AppContext.AppContextBuilder()
                .setHttpClient(httpClientMock)
                .createAppContext();

        var healthPotionPrice = 99;
        var csPrice = 99;
        var gasPrice = csPrice + 1;
        var csId = "cs";

        httpClientMock.addPlannedResponse(new HttpClientMock.PlannedResponse( 200, "GET",
                BBGameConfig.defaultServerUrl + "/" + testGameId + "/shop",
                """
        [
            {
                "id": "%s",
                "name": "Healing potion",
                "cost": %d
            },
            {
                "id": "%s",
                "name": "Claw Sharpening",
                "cost": %d
            },
            {
                "id": "gas",
                "name": "Gasoline",
                "cost": %d
            }
        ]
        """.formatted(BBGameActions.healthPotionId, healthPotionPrice, csId, csPrice, gasPrice)
        ));

        var gameContext = new GameContext(testGameId, testMinimumLives, testTargetScore);
        gameContext.gold = csPrice - 1;
        gameContext.lives = gameContext.minimumLives;

        ctx.gameActions.doShopping(gameContext);

        assertEquals( csPrice - 1, gameContext.gold);
        assertEquals(gameContext.minimumLives, gameContext.lives);
    }

}
