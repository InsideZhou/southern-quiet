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
    private ThrottleManager throttleManager;

    @Before
    public void before() {}

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
}
