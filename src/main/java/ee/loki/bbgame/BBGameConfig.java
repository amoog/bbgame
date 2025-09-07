package ee.loki.bbgame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.Properties;

public class BBGameConfig {
    private static final Logger logger = LoggerFactory.getLogger(BBGameConfig.class);

    public static final String defaultServerUrl = "https://dragonsofmugloar.com/api/v2";
    public static final String defaultConnectTimeoutSec = "10";
    public static final String defaultReadTimeoutSec = "10";
    public static final String defaultHistoryFile = "gamehistory.json";
    public static final String defaultMinimumLives = "5";
    public static final String defaultTargetScore = "1500";

    public final String serverUrl;
    public final Duration connectTimeout;
    public final Duration readTimeout;
    public final String historyFileName;
    public final int minimumLives;
    public final int targetScore;

    public BBGameConfig(String propsFileName) {
        this(loadProperties(propsFileName));
    }

    public BBGameConfig(Properties props) {
        serverUrl = props.getProperty("serverUrl", defaultServerUrl);
        connectTimeout = Duration.ofSeconds(Integer.parseInt(props.getProperty("connectTimeoutSec", defaultConnectTimeoutSec)));
        readTimeout = Duration.ofSeconds(Integer.parseInt(props.getProperty("readTimeoutSec", defaultReadTimeoutSec)));
        historyFileName = props.getProperty("historyFile", defaultHistoryFile);
        minimumLives = Integer.parseInt(props.getProperty("minimumLives", defaultMinimumLives));
        targetScore = Integer.parseInt(props.getProperty("targetScore", defaultTargetScore));
    }


    private static Properties loadProperties(String propsFileName) {
        var result = new Properties();
        logger.info("Loading properties from '{}'", propsFileName);
        try (FileInputStream fis = new FileInputStream(propsFileName)) {
            result.load(fis);
        } catch (Exception e) {
            logger.error("Failed to load properties file '{}'", propsFileName, e);
            return new Properties();
        }
        return result;
    }

}
