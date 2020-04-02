package test.throttle;

import me.insidezhou.southernquiet.throttle.RedisTimeBaseThrottle;
import me.insidezhou.southernquiet.throttle.Throttle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedisTimeBaseThrottleTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan({"me.insidezhou.southernquiet.throttle"})
    public static class Config {}

    @Resource(name="redisTimeBaseThrottle")
    private Throttle throttle;

    @Test
    public void testBySameKey() throws InterruptedException {

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
            boolean open = throttle.open(orderId, 100);
            if (!open) {
                continue;
            }
            System.out.println(i);
            count++;
        }
        Assert.assertEquals(2, count);
    }

    @Test
    public void testByDifferentKeys() throws InterruptedException {
        int count1 = 0;
        int count2 = 0;
        String orderId1 = "orderId1";
        String orderId2 = "orderId2";
        for (int i = 0; i < 3; i++) {
            boolean open1 = throttle.open(orderId1, 100);
            if (open1) {
                count1++;
                System.out.println(orderId1 + "  " + i);
            }
            boolean open2 = throttle.open(orderId2, 100);
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
