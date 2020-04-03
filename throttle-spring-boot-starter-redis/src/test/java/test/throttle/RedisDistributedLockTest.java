package test.throttle;

import me.insidezhou.southernquiet.throttle.lock.RedisDistributedLock;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.UUID;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedisDistributedLockTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan({"me.insidezhou.southernquiet.throttle"})
    public static class Config {}

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Test
    public void testLockSingleThread() throws InterruptedException {
        Assert.assertNotNull(redisDistributedLock);

        String key = UUID.randomUUID().toString();

        Duration lockTimeOut = Duration.ofSeconds(1);
        boolean getLock = redisDistributedLock.lock(key, lockTimeOut);
        Assert.assertTrue(getLock);

        getLock = redisDistributedLock.lock(key, lockTimeOut);
        Assert.assertFalse(getLock);

        redisDistributedLock.unlock(key);

        getLock = redisDistributedLock.lock(key, lockTimeOut);
        Assert.assertTrue(getLock);

        Thread.sleep(1000);

        getLock = redisDistributedLock.lock(key, lockTimeOut);
        Assert.assertTrue(getLock);
    }

    private static int getLockNumber = 0;
    @Test
    public void testLockMultipleThreads() throws InterruptedException {
        Assert.assertNotNull(redisDistributedLock);

        String key = UUID.randomUUID().toString();

        int threadNumber = 10;
        Thread[] threads = new Thread[threadNumber];
        for (int i = 0; i < threadNumber; i++) {
            Thread thread = new Thread(new TestLockRunnable(redisDistributedLock, key));
            threads[i] = thread;
        }
        for (Thread thread : threads) {
            thread.start();
            thread.join();
        }

        Assert.assertEquals(1, getLockNumber);

        getLockNumber = 0;
        threads = new Thread[threadNumber];
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        for (int i = 0; i < threadNumber; i++) {
            String threadKey = i % 2 == 0 ? key1 : key2;
            Thread thread = new Thread(new TestLockRunnable(redisDistributedLock, threadKey));
            threads[i] = thread;
        }
        for (Thread thread : threads) {
            thread.start();
            thread.join();
        }

        Assert.assertEquals(2, getLockNumber);
    }

    private static synchronized void getLockNumberAddOne() {
        getLockNumber++;
    }

    private static class TestLockRunnable implements Runnable{
        String key;
        RedisDistributedLock redisDistributedLock;

        public TestLockRunnable(RedisDistributedLock redisDistributedLock,String key) {
            this.redisDistributedLock = redisDistributedLock;
            this.key = key;
        }

        @Override
        public void run() {
            boolean getLock = redisDistributedLock.lock(key, Duration.ofSeconds(1));
            if (getLock) {
                getLockNumberAddOne();
            }
        }

    }

}
