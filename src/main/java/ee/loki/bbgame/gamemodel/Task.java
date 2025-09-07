package ee.loki.bbgame.gamemodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public record Task(
    String adId,
    String message,
    int reward,
    int expiresIn,
    String probability,
    int encrypted
){
    private static final Logger logger = LoggerFactory.getLogger(Task.class);

    public Task {
        if (encryptionUnknown()) {
            logger.info("Received task with unknown encryption {}: {}", encrypted, this);
        }

        if (encrypted == 1) {
            adId = decode(adId);
            message = decode(message);
            probability = decode(probability);
            encrypted = 0;
        }
    }

    private String decode(String input) {
        var decoded =  Base64.getDecoder().decode (input);
        return new String(decoded);
    }

    public boolean encryptionUnknown() {
        return encrypted != 0 && encrypted != 1;
    }
}
