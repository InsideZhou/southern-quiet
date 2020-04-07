package test.throttle;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.throttle.Throttle;
import me.insidezhou.southernquiet.throttle.ThrottleManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@SpringBootTest(classes = FrameworkAutoConfiguration.class)
@RunWith(SpringRunner.class)
public class ThrottleTest {

    @Autowired
    protected ThrottleManager throttleManager;

    @Before
    public void before() {
    }

    @Test
    public void timeBasedSameKeys() throws InterruptedException {

        String throttleName = UUID.randomUUID().toString();

        Throttle throttle = throttleManager.getTimeBased(throttleName);

        int count = 0;
        for (int i = 0; i < 3; i++) {
            Thread.sleep(60);
            boolean open = throttle.open(100);
            if (!open) {
                continue;
            }
            count++;
        }
        Assert.assertEquals(2, count);
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

    private static int openTimesCountBaseBySameKeyMultipleThread = 0;

    private synchronized static void openTimesCoutBaseBySameKeyMultipleThreadAddOne() {
        openTimesCountBaseBySameKeyMultipleThread++;
    }

    @Test
    public void countBaseSameKeyMultipleThreads() throws InterruptedException {
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

        Assert.assertEquals(5, openTimesCountBaseBySameKeyMultipleThread);
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
                openTimesCoutBaseBySameKeyMultipleThreadAddOne();
            }
        }
    }


}
