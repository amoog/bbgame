package ee.loki.bbgame;

import java.util.HashSet;
import java.util.Set;

class GameContext {
    final String gameId;
    int level;
    int lives;
    int gold;
    int score;
    final Set<String> boughtUpgrades = new HashSet<>();

    final int minimumLives;
    final int targetScore;


    public GameContext(String gameId, int minimumLives, int targetScore) {
        this.gameId = gameId;
        this.minimumLives = minimumLives;
        this.targetScore = targetScore;
    }
}
