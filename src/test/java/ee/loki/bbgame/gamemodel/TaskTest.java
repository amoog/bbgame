package ee.loki.bbgame.gamemodel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

public class TaskTest
{
    @Test
    void canDecryptType_1() {
        var unencryptedTask = new Task("adId", "message", 10, 3, "probability", 0);
        var encryptedTask = new Task(enc_1(unencryptedTask.adId()), enc_1(unencryptedTask.message()), 10, 3,
                enc_1(unencryptedTask.probability()), 1);

        Assertions.assertEquals(unencryptedTask, encryptedTask);
    }

    private String enc_1(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    @Test
    void canDetectUnknownEncryption() {
        var task = new Task("garbage", "", 1, 1, "irrelevant", 2);
        Assertions.assertTrue(task.encryptionUnknown());
    }
}
