package test.throttle;

import me.insidezhou.southernquiet.throttle.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedisThrottleManagerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan({"me.insidezhou.southernquiet.throttle"})
    public static class Config {}

    @Autowired
    private ThrottleManager throttleManager;

    @Test
    public void testGetThrottle() {
        Assert.assertTrue(throttleManager instanceof RedisThrottleManager);

        Throttle throttle = throttleManager.getThrottle();
        Assert.assertTrue(throttle instanceof RedisTimeBaseThrottle);

        try {
            throttleManager.getThrottle("whatever");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NoSuchBeanDefinitionException);
        }

        throttle = throttleManager.getThrottle("redisTimeBaseThrottle");
        Assert.assertTrue(throttle instanceof RedisTimeBaseThrottle);

        throttle = throttleManager.getThrottle("redisCounterBaseThrottle");
        Assert.assertTrue(throttle instanceof RedisCounterBaseThrottle);

    }

}
