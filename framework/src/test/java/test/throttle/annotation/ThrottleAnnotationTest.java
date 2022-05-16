package test.throttle.annotation;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {
    FrameworkAutoConfiguration.class,
    ThrottleAnnotationTestAutoConfiguration.class
})
@ExtendWith(SpringExtension.class)
@Service
public class ThrottleAnnotationTest {

    @Autowired
    ThrottleAnnotationTestProcessor throttleAnnotationTestProcessor;

    private void reset() {
        throttleAnnotationTestProcessor.setCountReturnObj(0);
        throttleAnnotationTestProcessor.setCountVoid(0);
    }

    @Test
    public void countBaseReturnObj() {
        reset();

        Integer count = null;
        try {
            count = throttleAnnotationTestProcessor.countBaseReturnObj(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertNull(count);

        try {
            count = throttleAnnotationTestProcessor.countBaseReturnObj(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertNotNull(count);
        Assertions.assertEquals(1, count.intValue());
    }

    @Test
    public void countBaseVoid() {
        reset();

        try {
            throttleAnnotationTestProcessor.countBaseVoid(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(0, throttleAnnotationTestProcessor.getCountVoid());

        try {
            throttleAnnotationTestProcessor.countBaseVoid(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(1, throttleAnnotationTestProcessor.getCountVoid());
    }

    @Test
    public void timeBaseReturnObj() throws InterruptedException {
        reset();

        Integer count = null;
        try {
            count = throttleAnnotationTestProcessor.timeBaseReturnObj(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertNull(count);

        Thread.sleep(1100);

        try {
            count = throttleAnnotationTestProcessor.timeBaseReturnObj(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertNotNull(count);
        Assertions.assertEquals(1, count.intValue());
    }

    @Test
    public void timeBaseVoid() throws InterruptedException {
        reset();

        try {
            throttleAnnotationTestProcessor.timeBaseVoid(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(0, throttleAnnotationTestProcessor.getCountVoid());

        Thread.sleep(1100);

        try {
            throttleAnnotationTestProcessor.timeBaseVoid(1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(1, throttleAnnotationTestProcessor.getCountVoid());
    }

}
