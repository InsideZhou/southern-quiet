package test;

import me.insidezhou.southernquiet.throttle.DefaultThrottle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ThrottleTest {

    @Test
    public void fixedWaiting() throws InterruptedException {
        DefaultThrottle defaultThrottle = new DefaultThrottle();
    }
}
