package test;

import me.insidezhou.southernquiet.throttle.DefaultThrottle;
import me.insidezhou.southernquiet.throttle.DefaultThrottleManager;
import me.insidezhou.southernquiet.throttle.Throttle;
import me.insidezhou.southernquiet.throttle.ThrottleManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.zip.CheckedOutputStream;

@RunWith(SpringRunner.class)
public class ThrottleTest {

    @Test
    public void textBySameOrderId() throws InterruptedException {
        ThrottleManager throttleManager = new DefaultThrottleManager();
        Throttle orderThrottle = throttleManager.getThrottle("orderThrottle");

        String orderId = "" + System.currentTimeMillis();

        int count = 0;
        for (int i = 0; i < 3; i++) {
            System.out.println(i);
            count++;
        }
        Assert.assertEquals(3, count);

        count = 0;
        for (int i = 0; i < 3; i++) {
            Thread.sleep(60);
            boolean open = orderThrottle.open(orderId, 100);
            if (!open) {
                continue;
            }
            System.out.println(i);
            count++;
        }
        Assert.assertEquals(2, count);
    }

    @Test
    public void textByDifferentOrderId() throws InterruptedException {
        ThrottleManager throttleManager = new DefaultThrottleManager();
        Throttle orderThrottle = throttleManager.getThrottle();
        int count1 = 0;
        int count2 = 0;
        String orderId1 = "orderId1";
        String orderId2 = "orderId2";
        for (int i = 0; i < 3; i++) {
            boolean open1 = orderThrottle.open(orderId1, 100);
            if (open1) {
                count1++;
                System.out.println(orderId1 + "  " + i);
            }
            boolean open2 = orderThrottle.open(orderId2, 100);
            if (open2) {
                count2++;
                System.out.println(orderId2 + "  " + i);
            }
            Thread.sleep(60);
        }
        Assert.assertEquals(2, count1);
        Assert.assertEquals(2, count2);
    }

}
