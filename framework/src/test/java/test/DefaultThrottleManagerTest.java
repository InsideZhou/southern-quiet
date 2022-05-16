package test;

import me.insidezhou.southernquiet.throttle.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DefaultThrottleManagerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan({"me.insidezhou.southernquiet.throttle"})
    public static class Config {
    }

    @Autowired
    private ThrottleManager throttleManager;

    @Test
    public void testThrottleManagerForTimeBased() {
        Assertions.assertTrue(throttleManager instanceof DefaultThrottleManager);

        Throttle timeBased = throttleManager.getTimeBased();
        Throttle timeBasedNull = throttleManager.getTimeBased(null);

        Assertions.assertTrue(timeBased instanceof DefaultTimeBasedThrottle);
        Assertions.assertTrue(timeBasedNull instanceof DefaultTimeBasedThrottle);

        Assertions.assertSame(timeBased, timeBasedNull);

        String name1 = UUID.randomUUID().toString();
        String name2 = UUID.randomUUID().toString();

        Throttle t1 = throttleManager.getTimeBased(name1);
        Throttle t1Copy = throttleManager.getTimeBased(name1);

        Assertions.assertSame(t1, t1Copy);

        Throttle t2 = throttleManager.getTimeBased(name2);
        Assertions.assertNotSame(t1, t2);
    }

    @Test
    public void testThrottleManagerForCountBased() {
        Assertions.assertTrue(throttleManager instanceof DefaultThrottleManager);

        Throttle countBased = throttleManager.getCountBased();
        Throttle countBasedNull = throttleManager.getCountBased(null);

        Assertions.assertTrue(countBased instanceof DefaultCountBasedThrottle);
        Assertions.assertTrue(countBasedNull instanceof DefaultCountBasedThrottle);

        Assertions.assertSame(countBased, countBasedNull);

        String name1 = UUID.randomUUID().toString();
        String name2 = UUID.randomUUID().toString();

        Throttle t1 = throttleManager.getCountBased(name1);
        Throttle t1Copy = throttleManager.getCountBased(name1);

        Assertions.assertSame(t1, t1Copy);

        Throttle t2 = throttleManager.getCountBased(name2);
        Assertions.assertNotSame(t1, t2);
    }
}
