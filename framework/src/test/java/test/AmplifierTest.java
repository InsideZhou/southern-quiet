package test;

import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.util.Amplifier;
import me.insidezhou.southernquiet.util.GoldenRatioAmplifier;
import org.junit.Test;

import java.time.Duration;

public class AmplifierTest {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AmplifierTest.class);

    @Test
    public void golden() {
        Amplifier amplifier = new GoldenRatioAmplifier(5);

        long index = 0;
        long total = 0;
        long current = 0;
        while (current < Duration.ofDays(1).toMillis()) {
            current = amplifier.amplify(index);
            total += current;
            log.message("amplified").context("index", index).context("current", Duration.ofMillis(current)).context("total", Duration.ofMillis(total)).info();
            ++index;
        }
    }
}
