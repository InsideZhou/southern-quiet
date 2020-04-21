package test.throttle.annotation;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = {
    FrameworkAutoConfiguration.class,
    ThrottleAnnotationTestAutoConfiguration.class
})
@RunWith(SpringRunner.class)
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

        Integer count = throttleAnnotationTestProcessor.countBaseReturnObj(1);
        Assert.assertNull(count);

        count = throttleAnnotationTestProcessor.countBaseReturnObj(1);
        Assert.assertNotNull(count);
        Assert.assertEquals(1,count.intValue());
    }

    @Test
    public void countBaseVoid() {
        reset();

        throttleAnnotationTestProcessor.countBaseVoid(1);
        Assert.assertEquals(0, throttleAnnotationTestProcessor.getCountVoid());

        throttleAnnotationTestProcessor.countBaseVoid(1);
        Assert.assertEquals(1, throttleAnnotationTestProcessor.getCountVoid());
    }

    @Test
    public void timeBaseReturnObj() throws InterruptedException {
        reset();

        Integer count = throttleAnnotationTestProcessor.timeBaseReturnObj(1);
        Assert.assertNull(count);

        Thread.sleep(1100);

        count = throttleAnnotationTestProcessor.timeBaseReturnObj(1);
        Assert.assertNotNull(count);
        Assert.assertEquals(1,count.intValue());
    }

    @Test
    public void timeBaseVoid() throws InterruptedException {
        reset();

        throttleAnnotationTestProcessor.timeBaseVoid(1);
        Assert.assertEquals(0, throttleAnnotationTestProcessor.getCountVoid());

        Thread.sleep(1100);

        throttleAnnotationTestProcessor.timeBaseVoid(1);
        Assert.assertEquals(1, throttleAnnotationTestProcessor.getCountVoid());
    }

}
