package test.broadcasting;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.broadcasting.Publisher;
import com.ai.southernquiet.broadcasting.RedisBroadcastingAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, RedisBroadcastingAutoConfiguration.class})
public class RedisBroadcastingTest {
    @Autowired
    private Publisher publisher;

    @Test
    public void sendSuccess() {
        publisher.publish(new BroadcastingDone());
    }

    @Test(expected = ClassCastException.class)
    public void sendFailure() {
        publisher.publish(new NonSerializableEvent());
    }
}
