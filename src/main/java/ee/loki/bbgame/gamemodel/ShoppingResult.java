package ee.loki.bbgame.gamemodel;

public record ShoppingResult(
    boolean shoppingSuccess,
    int gold,
    int lives,
    int level,
    int turn
) {}
