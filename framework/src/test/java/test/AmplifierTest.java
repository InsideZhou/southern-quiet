package test;

import me.insidezhou.southernquiet.util.Amplifier;
import me.insidezhou.southernquiet.util.GoldenRatioAmplifier;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class AmplifierTest {
    private final static Logger log = LoggerFactory.getLogger(AmplifierTest.class);

    @Test
    public void golden() {
        Amplifier amplifier = new GoldenRatioAmplifier(5);

        long index = 0;
        long total = 0;
        long current = 0;
        while (current < Duration.ofDays(1).toMillis()) {
            current = amplifier.amplify(index);
            total += current;
            log.info("amplified\tindex={}, current={}, total={}", index, Duration.ofMillis(current), Duration.ofMillis(total));
            ++index;
        }
    }
}
