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
public class ThrottleTest {

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

    @Test
    public void testTimeBaseBySameKey() throws InterruptedException {

        String throttleName = UUID.randomUUID().toString();

        Throttle orderThrottle = throttleManager.getTimeBased(throttleName);

        int count = 0;
        for (int i = 0; i < 3; i++) {
            boolean open = orderThrottle.open(100);
            if (open) {
                System.out.println(i);
                count++;
            }
            Thread.sleep(60);
        }
        Assert.assertEquals(2, count);
    }

    @Test
    public void testByDifferentKeys() throws InterruptedException {
        String throttleName1 = UUID.randomUUID().toString();
        String throttleName2 = UUID.randomUUID().toString();

        Throttle throttle1 = throttleManager.getTimeBased(throttleName1);
        Throttle throttle2 = throttleManager.getTimeBased(throttleName2);

        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < 3; i++) {
            boolean open1 = throttle1.open(100);
            if (open1) {
                count1++;
            }
            boolean open2 = throttle2.open(100);
            if (open2) {
                count2++;
            }
            Thread.sleep(60);
        }
        Assert.assertEquals(2, count1);
        Assert.assertEquals(2, count2);
    }

    @Test
    public void testCountBaseBySameKey() {

        String throttleName = UUID.randomUUID().toString();

        Throttle throttle = throttleManager.getCountBased(throttleName);

        int threshold = 1;

        int openTimes = 0;
        for (int i = 0; i < 10; i++) {
            boolean open = throttle.open(threshold);
            if (open) {
                openTimes++;
            }
        }
        Assert.assertEquals(5, openTimes);

        throttleName = UUID.randomUUID().toString();
        throttle = throttleManager.getCountBased(throttleName);
        threshold = 19;
        openTimes = 0;
        for (int i = 0; i < 100; i++) {
            boolean open = throttle.open(threshold);
            if (open) {
                openTimes++;
            }
        }
        Assert.assertEquals(5, openTimes);
    }

    @Test
    public void testCountBaseByDifferentKeys() {

        String throttleName1 = UUID.randomUUID().toString();
        String throttleName2 = UUID.randomUUID().toString();

        Throttle throttle1 = throttleManager.getCountBased(throttleName1);
        Throttle throttle2 = throttleManager.getCountBased(throttleName2);

        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < 10; i++) {
            boolean open1 = throttle1.open(2);
            if (open1) {
                count1++;
            }
            boolean open2 = throttle2.open(3);
            if (open2) {
                count2++;
            }
        }
        Assert.assertEquals(4, count1);
        Assert.assertEquals(3, count2);
    }
}
