package ee.loki.bbgame;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.loki.bbgame.history.HistoryStore;
import ee.loki.bbgame.rest.BBGameClient;
import ee.loki.bbgame.rest.HttpClientWrapper;
import ee.loki.bbgame.rest.IHttpClient;

import java.util.Properties;

public class AppContext {
    public final ObjectMapper objectMapper;
    public final BBGameConfig config;
    public final IHttpClient httpClient;
    public final BBGameClient gameClient;
    public final HistoryStore historyStore;
    public final BBGameActions gameActions;

    private AppContext(ObjectMapper objectMapper, BBGameConfig config, IHttpClient httpClient, BBGameClient gameClient,
                       HistoryStore historyStore, BBGameActions gameActions) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.httpClient = httpClient;
        this.gameClient = gameClient;
        this.historyStore = historyStore;
        this.gameActions = gameActions;
    }

    public static class AppContextBuilder {
        private String propsFileName;
        private ObjectMapper objectMapper;
        private BBGameConfig config;
        private IHttpClient httpClient;
        private BBGameClient gameClient;
        private HistoryStore historyStore;
        private BBGameActions gameRunner;

        public AppContextBuilder setPropsFileName(String propsFileName) {
            this.propsFileName = propsFileName;
            return this;
        }

        public AppContextBuilder setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public AppContextBuilder setConfig(BBGameConfig config) {
            this.config = config;
            return this;
        }

        public AppContextBuilder setHttpClient(IHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public AppContextBuilder setGameClient(BBGameClient gameClient) {
            this.gameClient = gameClient;
            return this;
        }

        public AppContextBuilder setHistoryStore(HistoryStore historyStore) {
            this.historyStore = historyStore;
            return this;
        }

        public AppContextBuilder setGameRunner(BBGameActions gameRunner) {
            this.gameRunner = gameRunner;
            return this;
        }

        public AppContext createAppContext() {
            if (objectMapper == null) {
                objectMapper = BBGameMain.createObjectMapper();
            }
            if (config == null) {
                if (propsFileName == null) {
                    config = new BBGameConfig(new Properties());
                } else {
                    config = new BBGameConfig(propsFileName);
                }
            }
            if (httpClient == null) {
                httpClient = new HttpClientWrapper(config);
            }
            if (gameClient == null) {
                gameClient = new BBGameClient(config, objectMapper, httpClient);
            }
            if (historyStore == null) {
                historyStore = new HistoryStore(objectMapper);
            }
            if (gameRunner == null) {
                gameRunner = new BBGameActions(gameClient, historyStore);
            }
            return new AppContext(objectMapper, config, httpClient, gameClient, historyStore, gameRunner);
        }
    }
}
