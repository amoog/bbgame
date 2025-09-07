import ee.loki.bbgame.AppContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppContextTest {
    @Test
    void appContextCanBeCreated() {
        var appContext = new AppContext.AppContextBuilder().setPropsFileName("missing").createAppContext();
        assertNotNull(appContext.config);
        assertNotNull(appContext.objectMapper);
        assertNotNull(appContext.gameClient);
        assertNotNull(appContext.gameActions);
        assertNotNull(appContext.historyStore);
        assertNotNull(appContext.httpClient);
    }
}
