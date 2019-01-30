package test;

import com.ai.southernquiet.throttle.FixedFrequencyThrottle;
import com.ai.southernquiet.throttle.ScheduledFixedFrequencyThrottle;
import com.ai.southernquiet.throttle.Throttle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ThrottleTest {
    private Throttle fixed = new FixedFrequencyThrottle(1000);
    private ScheduledFixedFrequencyThrottle scheduledFixed = new ScheduledFixedFrequencyThrottle(1000);

    @Test
    public void fixed() {
        long beginning = System.currentTimeMillis();

        while (System.currentTimeMillis() - beginning < 10000) {
            fixed.run((counter, elapsed) -> {
                System.out.println("被节流的方法在执行中: elapsed=" + elapsed + ", counter=" + counter);
            });
        }
    }

    @Test
    public void scheduledFixed() {
        long beginning = System.currentTimeMillis();

        while (System.currentTimeMillis() - beginning < 10000) {
            scheduledFixed.run((counter, elapsed) -> {
                System.out.println("被节流的方法在执行中: elapsed=" + elapsed + ", counter=" + counter + ", debouncedCounter=" + scheduledFixed.getDebouncedCount());
            });
        }
    }
}
