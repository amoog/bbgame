package ee.loki.bbgame;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class BBGameConfigTest {
    @Test
    void defaultValuesAreSetCorrectly() {
        var config = new BBGameConfig("");
        assertNotNull(config);
        assertEquals(BBGameConfig.defaultServerUrl, config.serverUrl);
        assertEquals(Duration.ofSeconds(Integer.parseInt(BBGameConfig.defaultConnectTimeoutSec)), config.connectTimeout);
        assertEquals(Duration.ofSeconds(Integer.parseInt(BBGameConfig.defaultReadTimeoutSec)), config.readTimeout);
        assertEquals(BBGameConfig.defaultHistoryFile, config.historyFileName);
        assertEquals(Integer.parseInt(BBGameConfig.defaultMinimumLives), config.minimumLives);
        assertEquals(Integer.parseInt(BBGameConfig.defaultTargetScore), config.targetScore);
    }

    @Test
    void valuesAreReadCorrectly() throws IOException {
        var testUrl = "http://localhost/";
        var testConnectTimeout = 33;
        var testReadTimeout = 43;
        var testHistoryFile = "testHistoryFile";
        var testMinimumLives = 9;
        var testTargetScore = 999;

        var testProperties = """
               serverUrl=%s
               connectTimeoutSec=%d
               readTimeoutSec=%d
               historyFile=%s
               minimumLives=%d
               targetScore=%d
               """
              .formatted(testUrl, testConnectTimeout, testReadTimeout, testHistoryFile, testMinimumLives, testTargetScore);

        var tempFile = File.createTempFile("testsettings", ".props");
        try {
            Files.write(tempFile.toPath(), testProperties.getBytes());
            var config = new BBGameConfig(tempFile.getAbsolutePath());
            assertNotNull(config);
            assertEquals(testUrl, config.serverUrl);
            assertEquals(Duration.ofSeconds(testConnectTimeout), config.connectTimeout);
            assertEquals(Duration.ofSeconds(testReadTimeout), config.readTimeout);
            assertEquals(testHistoryFile, config.historyFileName);
            assertEquals(testMinimumLives, config.minimumLives);
            assertEquals(testTargetScore, config.targetScore);
        } finally {
            tempFile.delete();
        }
    }
}
