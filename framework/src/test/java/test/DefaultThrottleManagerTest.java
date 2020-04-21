package test;

import me.insidezhou.southernquiet.throttle.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@SpringBootTest
@RunWith(SpringRunner.class)
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
        Assert.assertTrue(throttleManager instanceof DefaultThrottleManager);

        Throttle timeBased = throttleManager.getTimeBased();
        Throttle timeBasedNull = throttleManager.getTimeBased(null);

        Assert.assertTrue(timeBased instanceof DefaultTimeBasedThrottle);
        Assert.assertTrue(timeBasedNull instanceof DefaultTimeBasedThrottle);

        Assert.assertSame(timeBased, timeBasedNull);

        String name1 = UUID.randomUUID().toString();
        String name2 = UUID.randomUUID().toString();

        Throttle t1 = throttleManager.getTimeBased(name1);
        Throttle t1Copy = throttleManager.getTimeBased(name1);

        Assert.assertSame(t1, t1Copy);

        Throttle t2 = throttleManager.getTimeBased(name2);
        Assert.assertNotSame(t1, t2);
    }

    @Test
    public void testThrottleManagerForCountBased() {
        Assert.assertTrue(throttleManager instanceof DefaultThrottleManager);

        Throttle countBased = throttleManager.getCountBased();
        Throttle countBasedNull = throttleManager.getCountBased(null);

        Assert.assertTrue(countBased instanceof DefaultCountBasedThrottle);
        Assert.assertTrue(countBasedNull instanceof DefaultCountBasedThrottle);

        Assert.assertSame(countBased, countBasedNull);

        String name1 = UUID.randomUUID().toString();
        String name2 = UUID.randomUUID().toString();

        Throttle t1 = throttleManager.getCountBased(name1);
        Throttle t1Copy = throttleManager.getCountBased(name1);

        Assert.assertSame(t1, t1Copy);

        Throttle t2 = throttleManager.getCountBased(name2);
        Assert.assertNotSame(t1, t2);
    }

}
