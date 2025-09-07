package ee.loki.bbgame.gamemodel;

public record TaskResult(
    boolean success,
    int lives,
    int gold,
    int score,
    int highScore,
    int turn,
    String message
) {}
