package test.throttle;

import me.insidezhou.southernquiet.throttle.Throttle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DefaultThrottleTest extends ThrottleTest {

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan({"me.insidezhou.southernquiet.throttle"})
    public static class Config {}

    @Test
    public void timeBaseDifferentThresholds() throws InterruptedException {
        String throttleName = UUID.randomUUID().toString();

        Throttle throttle = throttleManager.getTimeBased(throttleName);

        boolean open = throttle.open(1000);
        Assert.assertTrue(open);

        open = throttle.open(500);
        Assert.assertFalse(open);

        Thread.sleep(200);

        open = throttle.open(200);
        Assert.assertTrue(open);
    }

    @Test
    public void countBaseDifferentThresholds() {
        String throttleName1 = UUID.randomUUID().toString();
        Throttle throttle1 = throttleManager.getCountBased(throttleName1);
        boolean open = throttle1.open(3);
        Assert.assertTrue(open);
        open = throttle1.open(3);
        Assert.assertFalse(open);
        open = throttle1.open(3);
        Assert.assertFalse(open);
        open = throttle1.open(3);
        Assert.assertFalse(open);


        String throttleName2 = UUID.randomUUID().toString();
        Throttle throttle2 = throttleManager.getCountBased(throttleName2);
        open = throttle2.open(3);
        Assert.assertTrue(open);
        open = throttle2.open(3);
        Assert.assertFalse(open);
        open = throttle2.open(3);
        Assert.assertFalse(open);
        open = throttle2.open(2);
        Assert.assertTrue(open);
    }

}
