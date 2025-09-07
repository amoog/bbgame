package ee.loki.bbgame.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.loki.bbgame.gamemodel.Task;
import ee.loki.bbgame.gamemodel.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

// this class IS thread safe
public class HistoryStore {
    private static final Logger logger = LoggerFactory.getLogger(HistoryStore.class);

    private final ObjectMapper objectMapper;

    private final Map<String, ResolveStatistic> probabilityStats = new HashMap<>();
    private List<GameStep> history = new ArrayList<>();

    public HistoryStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void loadGameHistory(String historyFileName) {
        try {
            history = objectMapper.readValue(new File(historyFileName), new TypeReference<List<GameStep>>() {});
        } catch (IOException e) {
            logger.error("Failed to load history file.", e);
            history = new ArrayList<>();
        }
        calculateStatistics();
    }

    private void calculateStatistics() {
        for (GameStep gameStep : history) {
            calculateStatistic(gameStep);
        }
    }

    private void calculateStatistic(GameStep gameStep) {
        var statistic = probabilityStats.computeIfAbsent(gameStep.attemptedTask().probability(), s -> new ResolveStatistic());
        if (gameStep.attemptResult().success()) {
            statistic.succeeded++;
        } else  {
            statistic.failed++;
        }
    }

    public synchronized double getSuccessRate(String probability) {
        var statistic = probabilityStats.get(probability);
        if (statistic==null) {
            return 0.0;
        }
        return (double) statistic.succeeded / (statistic.failed + statistic.succeeded);
    }

    public synchronized void saveGameHistory(String historyFileName) {
        try {
            objectMapper.writeValue(new File(historyFileName), history);
        } catch (IOException e) {
            logger.error("Failed to save history file.", e);
        }
    }

    public synchronized void addGameStep(String gameId, Task nextTask, TaskResult taskResult) {
        var gameStep = new GameStep(gameId, nextTask, taskResult);
        history.add(gameStep);
        calculateStatistic(gameStep);
    }

    synchronized List<GameStep> getHistory() {
        return List.of(history.toArray(new GameStep[0]));
    }
}
