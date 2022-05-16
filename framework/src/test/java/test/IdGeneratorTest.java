package test;

import me.insidezhou.southernquiet.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class IdGeneratorTest {
    @SpringBootConfiguration
    public static class Config {}

    private final static long EPOCH = 1573430400L;
    private final static int WORKER = 3;
    private final static int SEQUENCE_START_RANGE = 1000;

    @Test
    public void secondsAccuracy() {
        int tickAccuracy = 1000;
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(WORKER, EPOCH, SEQUENCE_START_RANGE, tickAccuracy);
        long id = idGenerator.generate();

        long ticks = idGenerator.getTicksFromId(id);
        long timestamp = idGenerator.getTimestampFromId(id);
        long sequence = idGenerator.getSequenceFromId(id);
        int worker = idGenerator.getWorkerFromId(id);

        Assertions.assertEquals(
            Duration.between(LocalDateTime.ofInstant(Instant.ofEpochSecond(EPOCH), ZoneOffset.UTC.normalized()), LocalDateTime.now()).toDays(),
            Duration.between(Instant.ofEpochSecond(EPOCH), Instant.ofEpochMilli(timestamp)).toDays()
        );

        commonAsserts(id, worker, timestamp, sequence, ticks, tickAccuracy);
    }

    @Test
    public void millisAccuracy() {
        int tickAccuracy = 1;
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(WORKER, 48, 0, 12, 0, EPOCH, -1, null, tickAccuracy);
        long id = idGenerator.generate();

        long ticks = idGenerator.getTicksFromId(id);
        long timestamp = idGenerator.getTimestampFromId(id);
        long sequence = idGenerator.getSequenceFromId(id);
        int worker = idGenerator.getWorkerFromId(id);

        commonAsserts(id, worker, timestamp, sequence, ticks, tickAccuracy);
    }

    @Test
    public void rareAccuracy() {
        int tickAccuracy = 56;
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(WORKER, EPOCH, SEQUENCE_START_RANGE, tickAccuracy);
        long id = idGenerator.generate();

        long ticks = idGenerator.getTicksFromId(id);
        long timestamp = idGenerator.getTimestampFromId(id);
        long sequence = idGenerator.getSequenceFromId(id);
        int worker = idGenerator.getWorkerFromId(id);

        commonAsserts(id, worker, timestamp, sequence, ticks, tickAccuracy);
    }

    private void commonAsserts(long id, int worker, long timestamp, long sequence, long ticks, int tickAccuracy) {
        Assertions.assertTrue(id > 0);
        Assertions.assertEquals(WORKER, worker);
        Assertions.assertTrue(timestamp > EPOCH * 1000);
        Assertions.assertTrue(sequence <= SEQUENCE_START_RANGE);
        Assertions.assertEquals(ticks * tickAccuracy + EPOCH * 1000, timestamp);
    }
}
