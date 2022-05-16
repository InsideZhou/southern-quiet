package test.throttle;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.throttle.Throttle;
import me.insidezhou.southernquiet.throttle.ThrottleAdvice;
import me.insidezhou.southernquiet.throttle.ThrottleManager;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

@SpringBootTest(classes = {FrameworkAutoConfiguration.class, ThrottleTestApp.class})
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThrottleTest {
    @Autowired
    protected ThrottleManager throttleManager;

    @Autowired
    private ThrottleTestApp.ScheduledThrottleBean scheduledThrottleBean;

    @Autowired
    private ThrottleAdvice throttleAdvice;

    @BeforeAll
    public void before() {
        scheduledThrottleBean.scheduledThrottleMethod1();
        scheduledThrottleBean.scheduledThrottleMethod2();
        scheduledThrottleBean.scheduledThrottleMethod3();
    }

    @Test
    public void countDelay() throws Exception {
        Throttle throttle = throttleManager.getTimeBased(RandomString.make(), 3);

        Assertions.assertTrue(throttle.open(999999999));
        Assertions.assertTrue(throttle.open(100000000));
        Assertions.assertTrue(throttle.open(100));
        Assertions.assertFalse(throttle.open(100));


        throttle = throttleManager.getTimeBased(RandomString.make(), 3);

        Assertions.assertTrue(throttle.open(999999999));
        Assertions.assertTrue(throttle.open(100000000));
        Assertions.assertTrue(throttle.open(100));

        Thread.sleep(100);
        Assertions.assertTrue(throttle.open(100));
    }

    @Test
    public void advisingCount() {
        Assertions.assertEquals(1, throttleAdvice.advisingCount());
    }

    @Test
    public void countBased() {
        Throttle throttle = throttleManager.getCountBased(RandomString.make());

        Assertions.assertFalse(throttle.open(1));
        Assertions.assertTrue(throttle.open(1));
        Assertions.assertFalse(throttle.open(2));
        Assertions.assertFalse(throttle.open(2));
        Assertions.assertTrue(throttle.open(2));
    }

    @Test
    public void countBasedForZero() {
        Throttle throttle = throttleManager.getCountBased(RandomString.make());

        Assertions.assertFalse(throttle.open(3));
        Assertions.assertTrue(throttle.open(0));
        Assertions.assertFalse(throttle.open(1));
        Assertions.assertTrue(throttle.open(1));
    }

    @Test
    public void timeBased() throws InterruptedException {

        String throttleName = UUID.randomUUID().toString();

        Throttle throttle = throttleManager.getTimeBased(throttleName);

        int count = 0;
        for (int i = 0; i < 3; i++) {
            boolean open = throttle.open(1000);
            if (open) {
                count++;
            }
            Thread.sleep(600);
        }
        Assertions.assertEquals(1, count);
    }

    private static int timeBasedSameKeysMultipleThreadsCount = 0;

    private static synchronized void timeBasedSameKeysMultipleThreadsCountAdd() {
        timeBasedSameKeysMultipleThreadsCount++;
    }

    private static class TimeBasedSameKeysMultipleThreadsRunnable implements Runnable {
        Throttle throttle;
        long threshold;

        public TimeBasedSameKeysMultipleThreadsRunnable(Throttle throttle, long threshold) {
            this.throttle = throttle;
            this.threshold = threshold;
        }

        @Override
        public void run() {
            boolean open = throttle.open(threshold);
            if (open) {
                timeBasedSameKeysMultipleThreadsCountAdd();
            }
        }
    }

    @Test
    public void timeBasedMultipleThreads() throws InterruptedException {

        String throttleName = UUID.randomUUID().toString();

        Throttle throttle = throttleManager.getTimeBased(throttleName);

        int threadNumber = 10;
        long threshold = 1000;
        Thread[] threads = new Thread[threadNumber];
        for (int i = 0; i < threadNumber; i++) {
            Thread thread = new Thread(new TimeBasedSameKeysMultipleThreadsRunnable(throttle, threshold));
            threads[i] = thread;
        }
        for (Thread thread : threads) {
            thread.start();
            thread.join();
        }
        Assertions.assertEquals(0, timeBasedSameKeysMultipleThreadsCount);
        timeBasedSameKeysMultipleThreadsCount = 0;

        Thread.sleep(1000);

        for (int i = 0; i < threadNumber; i++) {
            Thread thread = new Thread(new TimeBasedSameKeysMultipleThreadsRunnable(throttle, threshold));
            threads[i] = thread;
        }
        for (Thread thread : threads) {
            thread.start();
            thread.join();
        }
        Assertions.assertEquals(1, timeBasedSameKeysMultipleThreadsCount);
        timeBasedSameKeysMultipleThreadsCount = 0;
    }

    @Test
    public void timeBasedDifferentKeys() throws InterruptedException {
        int count1 = 0;
        int count2 = 0;
        String throttleName1 = UUID.randomUUID().toString();
        String throttleName2 = UUID.randomUUID().toString();

        Throttle throttle1 = throttleManager.getTimeBased(throttleName1);
        Throttle throttle2 = throttleManager.getTimeBased(throttleName2);

        for (int i = 0; i < 3; i++) {
            boolean open1 = throttle1.open(1000);
            if (open1) {
                count1++;
            }
            boolean open2 = throttle2.open(1000);
            if (open2) {
                count2++;
            }
            Thread.sleep(600);
        }
        Assertions.assertEquals(1, count1);
        Assertions.assertEquals(1, count2);
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
        Assertions.assertEquals(5, openTimes);

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
        Assertions.assertEquals(5, openTimes);
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
        Assertions.assertEquals(3, count1);
        Assertions.assertEquals(2, count2);
    }

    private static int openTimesCountBaseBySameKeyMultipleThread = 0;

    private synchronized static void openTimesCountBaseBySameKeyMultipleThreadAddOne() {
        openTimesCountBaseBySameKeyMultipleThread++;
    }

    @Test
    public void countBaseMultipleThreads() throws InterruptedException {
        String throttleName = UUID.randomUUID().toString();

        Throttle throttle = throttleManager.getCountBased(throttleName);

        int threshold = 1;

        int threadNumber = 10;
        Thread[] threads = new Thread[threadNumber];
        for (int i = 0; i < threadNumber; i++) {
            Thread thread = new Thread(new CountBaseSameKeyMultipleThreadRunnable(throttle, threshold));
            threads[i] = thread;
        }
        for (Thread thread : threads) {
            thread.start();
            thread.join();
        }

        Assertions.assertEquals(5, openTimesCountBaseBySameKeyMultipleThread);

        openTimesCountBaseBySameKeyMultipleThread = 0;
    }

    private static class CountBaseSameKeyMultipleThreadRunnable implements Runnable {
        Throttle throttle;
        long threshold;

        public CountBaseSameKeyMultipleThreadRunnable(Throttle throttle, long threshold) {
            this.throttle = throttle;
            this.threshold = threshold;
        }

        @Override
        public void run() {
            boolean open = throttle.open(threshold);
            if (open) {
                openTimesCountBaseBySameKeyMultipleThreadAddOne();
            }
        }
    }

    @Test
    public void timeBaseDifferentThresholds() throws InterruptedException {
        String throttleName = UUID.randomUUID().toString();

        Throttle throttle = throttleManager.getTimeBased(throttleName);

        Assertions.assertFalse(throttle.open(1000));

        boolean open = throttle.open(0);
        Assertions.assertTrue(open);

        open = throttle.open(1000);
        Assertions.assertFalse(open);

        open = throttle.open(500);
        Assertions.assertFalse(open);

        Thread.sleep(200);

        open = throttle.open(200);
        Assertions.assertTrue(open);
    }

    @Test
    public void countBaseDifferentThresholds() {
        String throttleName1 = UUID.randomUUID().toString();
        Throttle throttle1 = throttleManager.getCountBased(throttleName1);
        boolean open = throttle1.open(3);
        Assertions.assertFalse(open);
        open = throttle1.open(3);
        Assertions.assertFalse(open);
        open = throttle1.open(3);
        Assertions.assertFalse(open);
        open = throttle1.open(3);
        Assertions.assertTrue(open);

        String throttleName2 = UUID.randomUUID().toString();
        Throttle throttle2 = throttleManager.getCountBased(throttleName2);
        open = throttle2.open(3);
        Assertions.assertFalse(open);
        open = throttle2.open(3);
        Assertions.assertFalse(open);
        open = throttle2.open(2);
        Assertions.assertTrue(open);

        Throttle throttle3 = throttleManager.getCountBased(UUID.randomUUID().toString());
        Assertions.assertFalse(throttle3.open(3));
        Assertions.assertTrue(throttle3.open(0));
        Assertions.assertFalse(throttle3.open(2));
    }
}
