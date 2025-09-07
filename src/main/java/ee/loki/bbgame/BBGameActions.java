package ee.loki.bbgame;

import ee.loki.bbgame.gamemodel.*;
import ee.loki.bbgame.history.HistoryStore;
import ee.loki.bbgame.rest.BBGameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

// this class IS thread safe
public class BBGameActions {
    private static final Logger logger = LoggerFactory.getLogger(BBGameActions.class);
    public static final String healthPotionId = "hpot";

    private final BBGameClient gameClient;
    private final HistoryStore historyStore;


    public BBGameActions(BBGameClient gameClient, HistoryStore historyStore) {
        this.gameClient = gameClient;
        this.historyStore = historyStore;
    }

    GameContext startGame(int minimumLives, int targetScore) throws IOException, InterruptedException {
        var game = gameClient.startNewGame();
        var result = new GameContext(game.gameId(), minimumLives, targetScore);
        result.gold = game.gold();
        result.lives = game.lives();
        result.level = game.level();
        result.score = game.score();
        return result;
    }

    // select next task based on statistical probability * reward value, preferring tasks that expire sooner
    Task selectNextTask(GameContext context) throws IOException, InterruptedException {
        var tasks = gameClient.getTasks(context.gameId);
        double bestValue = -1;
        int bestExpire = Integer.MAX_VALUE;
        Task bestTask = null;
        for (Task task : tasks) {
            var currentValue = historyStore.getSuccessRate(task.probability()) * task.reward();
            if (currentValue >= bestValue) {
                if (currentValue > bestValue || task.expiresIn() < bestExpire) {
                    bestValue = currentValue;
                    bestExpire = task.expiresIn();
                    bestTask = task;
                }
            }
        }
        return bestTask;
    }

    void resolveTask(GameContext context, Task nextTask) throws IOException, InterruptedException {
        var taskResult = gameClient.tryResolveTask(context.gameId, nextTask.adId());
        context.lives = taskResult.lives();
        context.gold = taskResult.gold();
        context.score = taskResult.score();

        logger.info("Task result: {}", taskResult);

        historyStore.addGameStep(context.gameId, nextTask, taskResult);
    }


    // if lives are below minimumLives try to buy health potion
    // otherwise try to buy an upgrade
    void doShopping(GameContext context) throws IOException, InterruptedException {
        var items = gameClient.getShopItems(context.gameId);
        if (context.lives < context.minimumLives) {
            var healthPrice = findHealthPrice(items);
            if (healthPrice > 0 && context.gold >= healthPrice) {
                buyItem(context, healthPotionId);
            }
        } else {
            for (var shopItem : items) {
                if (!healthPotionId.equals(shopItem.id()) && !context.boughtUpgrades.contains(shopItem.id())) {
                    if (shopItem.cost() <= context.gold) {
                        if (buyItem(context, shopItem.id())) {
                            context.boughtUpgrades.add(shopItem.id());
                            break;
                        }
                    }
                }
            }
        }
    }

    private int findHealthPrice(ShopItem[] items) {
        return Arrays.stream(items)
                .filter(item -> healthPotionId.equals(item.id()))
                .findFirst()
                .map(ShopItem::cost)
                .orElse(0);
    }

    private boolean buyItem(GameContext context, String itemId) throws IOException, InterruptedException {
        var result = gameClient.tryBuyItem(context.gameId, itemId);
        if (!result.shoppingSuccess()) {
            logger.error("Failed to buy item {}: {}", itemId, result);
        }
        context.level = result.level();
        context.lives = result.lives();
        context.gold = result.gold();
        return  result.shoppingSuccess();
    }
}
