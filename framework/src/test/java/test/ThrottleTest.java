package test;

import com.ai.southernquiet.throttle.DebouncedThrottle;
import com.ai.southernquiet.throttle.FixedWaitingThrottle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ThrottleTest {

    @Test
    public void fixedWaiting() throws InterruptedException {
        FixedWaitingThrottle fixedWaiting = new FixedWaitingThrottle(1000);

        Thread.sleep(300);

        boolean opened = fixedWaiting.open();
        Assert.assertTrue(opened);
        Assert.assertEquals(1, fixedWaiting.counter());

        opened = fixedWaiting.open();
        Assert.assertFalse(opened);
    }

    @Test
    public void debouncedThrottle() throws InterruptedException {
        DebouncedThrottle debouncedThrottle = new DebouncedThrottle(300, 1000L);

        boolean opened = debouncedThrottle.open();
        Assert.assertFalse(opened);

        Thread.sleep(1000);

        opened = debouncedThrottle.open();
        Assert.assertTrue(opened);
        Assert.assertEquals(1L, debouncedThrottle.counter());
    }

    @Test
    public void debouncedCount() {
        DebouncedThrottle debouncedThrottle = new DebouncedThrottle(300, 3000L);
        long begin = System.currentTimeMillis();

        boolean opened = debouncedThrottle.open();

        while (System.currentTimeMillis() - begin < 2000) {
            opened = debouncedThrottle.open();
        }

        Assert.assertFalse(opened);
    }
}
