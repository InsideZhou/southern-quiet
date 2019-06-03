package test;

import me.insidezhou.southernquiet.util.SnowflakeIdGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.Random;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IdGeneratorTest {
    @SpringBootConfiguration
    public static class Config {}

    private SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(3, new Random(), 1000);

    @Test
    public void getInfoFromId() {
        long id = idGenerator.generate();

        System.out.println("ts: " + Instant.ofEpochSecond(idGenerator.getTimestampFromId(id)));

        Assert.assertEquals(3, idGenerator.getWorkerFromId(id));
        System.out.println("worker: " + idGenerator.getWorkerFromId(id));

        System.out.println("sequence: " + idGenerator.getSequenceFromId(id));
    }
}
