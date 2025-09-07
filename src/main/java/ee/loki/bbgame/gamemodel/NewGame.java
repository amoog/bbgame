package ee.loki.bbgame.gamemodel;

public record NewGame(
    String gameId,
    int lives,
    int gold,
    int level,
    int score,
    int highScore,
    int turn
) {}
