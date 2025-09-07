package ee.loki.bbgame.history;

import ee.loki.bbgame.gamemodel.Task;
import ee.loki.bbgame.gamemodel.TaskResult;

public record GameStep(
    String gameId,
    Task attemptedTask,
    TaskResult attemptResult
)
{}
