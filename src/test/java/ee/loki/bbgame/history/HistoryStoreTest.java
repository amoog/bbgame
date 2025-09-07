package ee.loki.bbgame.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.loki.bbgame.BBGameMain;
import ee.loki.bbgame.gamemodel.Task;
import ee.loki.bbgame.gamemodel.TaskResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryStoreTest {
    private static final ObjectMapper objectMapper = BBGameMain.createObjectMapper();

    private static final String testProbability = "testprobability";

    private static final Task testTask = new Task("test", "testmessage", 10, 1, testProbability, 0);
    private static final TaskResult successResult = new TaskResult( true,1, 1, 1, 0, 1, "what");
    private static final TaskResult failureResult = new TaskResult( false,1, 1, 1, 0, 1, "what");

    @Test
    void canSaveAndLoadHistory() throws IOException {
        var gameId = createTestGameId();
        var tempFile = File.createTempFile("history", ".json");
        try {
            var sourceStore = createTestStore(2, gameId);
            sourceStore.saveGameHistory(tempFile.getAbsolutePath());

            var resultStore = new HistoryStore(objectMapper);
            resultStore.loadGameHistory(tempFile.getAbsolutePath());

            Assertions.assertArrayEquals(
                    sourceStore.getHistory().toArray(new GameStep[0]),
                    resultStore.getHistory().toArray(new GameStep[0]));
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void missingProbabilitySuccessRateIsZero() {
        var gameId = createTestGameId();
        var sourceStore = createTestStore(2, gameId);
        assertEquals(0.0, sourceStore.getSuccessRate("unknown"));
    }

    @Test
    void probabilityRateIsCorrect() {
        var gameId = createTestGameId();
        var sourceStore = createTestStore(2, gameId);

        sourceStore.addGameStep(gameId, testTask, successResult);
        sourceStore.addGameStep(gameId, testTask, successResult);
        sourceStore.addGameStep(gameId, testTask, failureResult);

        assertEquals(2.0/3.0, sourceStore.getSuccessRate(testProbability));
    }

    @Test
    void statisticsChangeDynamically() {
        var gameId = createTestGameId();
        var sourceStore = createTestStore(3, gameId);

        sourceStore.addGameStep(gameId, testTask, successResult);
        sourceStore.addGameStep(gameId, testTask, successResult);

        assertEquals( 1.0, sourceStore.getSuccessRate(testProbability));

        sourceStore.addGameStep(gameId, testTask, failureResult);
        assertEquals( 2.0 / 3.0, sourceStore.getSuccessRate(testProbability));
    }

    private HistoryStore createTestStore(int steps, String gameId) {
        var result = new HistoryStore(objectMapper);
        for (int i = 0; i < steps; i++) {
            result.addGameStep(gameId,
                    new Task("adid" + i, "message" + i, 10*i, i + 1, "p" + i, 0),
                    new TaskResult(i % 3 == 0,5+i, i*15, i*20, 0, i, "what" + i ));
       }
       return result;
    }

    private String createTestGameId() {
        return UUID.randomUUID().toString();
    }
}
