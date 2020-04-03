package test.throttle;

import me.insidezhou.southernquiet.throttle.RedisCounterBaseThrottle;
import me.insidezhou.southernquiet.throttle.Throttle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedisCounterBaseThrottleTest {


    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan({"me.insidezhou.southernquiet.throttle"})
    public static class Config {
    }

    @Resource(name = "redisCounterBaseThrottle")
    private Throttle throttle;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testIncrementFunction() throws InterruptedException {
        String key = "" + System.currentTimeMillis();

        Long counterL = stringRedisTemplate.opsForValue().increment(key);

        assert counterL != null;
        Assert.assertEquals(1, counterL.longValue());

        String counterStr = stringRedisTemplate.opsForValue().get(key);
        Assert.assertEquals("1", counterStr);

        //设置key过期
        stringRedisTemplate.expire(key, 100, TimeUnit.MILLISECONDS);

        Thread.sleep(200);

        counterStr = stringRedisTemplate.opsForValue().get(key);
        Assert.assertNull(counterStr);

        stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.opsForValue().increment(key);
        counterL = stringRedisTemplate.opsForValue().increment(key);
        assert counterL != null;
        Assert.assertEquals(4, counterL.longValue());

        stringRedisTemplate.expire(key, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testBySameKey() {
        Assert.assertTrue(throttle instanceof RedisCounterBaseThrottle);

        String key = "" + System.currentTimeMillis();

        int threshold = 1;

        int openTimes = 0;
        for (int i = 0; i < 10; i++) {
            boolean open = throttle.open(key, threshold);
            if (open) {
                openTimes++;
            }
        }
        Assert.assertEquals(5, openTimes);

        threshold = 19;
        openTimes = 0;
        for (int i = 0; i < 100; i++) {
            boolean open = throttle.open(key, threshold);
            if (open) {
                openTimes++;
            }
        }
        Assert.assertEquals(5, openTimes);
    }

    private static int openTimesTestBySameKeyMultipleThread = 0;

    private synchronized static void openTimesTestBySameKeyMultipleThreadAddOne() {
        openTimesTestBySameKeyMultipleThread++;
    }

    @Test
    public void testBySameKeyMultipleThread() throws InterruptedException {
        String key = "" + System.currentTimeMillis();

        int threshold = 1;

        int threadNumber = 10;
        Thread[] threads = new Thread[threadNumber];
        for (int i = 0; i < threadNumber; i++) {
            Thread thread = new Thread(new TestBySameKeyMultipleThreadRunnable(throttle, key, threshold));
            threads[i] = thread;
        }
        for (Thread thread : threads) {
            thread.start();
            thread.join();
        }

        Assert.assertEquals(5, openTimesTestBySameKeyMultipleThread);
    }

    private static class TestBySameKeyMultipleThreadRunnable implements Runnable {
        String key;
        Throttle throttle;
        long threshold;

        public TestBySameKeyMultipleThreadRunnable(Throttle throttle, String key, long threshold) {
            this.throttle = throttle;
            this.key = key;
            this.threshold = threshold;
        }

        @Override
        public void run() {
            boolean open = throttle.open(key, threshold);
            if (open) {
                openTimesTestBySameKeyMultipleThreadAddOne();
            }
        }

    }


    @Test
    public void testByDifferentKeys(){
        int count1 = 0;
        int count2 = 0;
        String orderId1 = UUID.randomUUID().toString();
        String orderId2 = UUID.randomUUID().toString();
        for (int i = 0; i < 10; i++) {
            boolean open1 = throttle.open(orderId1, 2);
            if (open1) {
                count1++;
            }
            boolean open2 = throttle.open(orderId2, 3);
            if (open2) {
                count2++;
            }
        }
        Assert.assertEquals(4, count1);
        Assert.assertEquals(3, count2);
    }

}
