package ee.loki.bbgame;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BBGameMain {
    private static final Logger logger = LoggerFactory.getLogger(BBGameMain.class);

    public static void main(String[] args) {
        logger.info("Starting BBGameMain");
        try {
          var appContext = new AppContext.AppContextBuilder().setPropsFileName("bbgame.properties").createAppContext();
          try {
              runGame(appContext);
          } finally {
              appContext.historyStore.saveGameHistory(appContext.config.historyFileName);
          }
        } catch (Exception e) {
          logger.error("Exception thrown", e);
        }
        logger.info("Finished BBGameMain");
    }

    static void runGame(AppContext appContext) throws IOException, InterruptedException {
        var gameContext = appContext.gameActions.startGame(appContext.config.minimumLives, appContext.config.targetScore);

        while (gameContext.lives > 0 && gameContext.score < appContext.config.targetScore) {
            appContext.gameActions.doShopping(gameContext);
            var nextTask = appContext.gameActions.selectNextTask(gameContext);
            if (nextTask == null) {
                logger.info("Giving up, no more eligible tasks");
                break;
            }
            appContext.gameActions.resolveTask(gameContext, nextTask);
        }
        logger.info("Finished game. Score: {}. Level: {}", gameContext.score, gameContext.level);
    }

    public static ObjectMapper createObjectMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
