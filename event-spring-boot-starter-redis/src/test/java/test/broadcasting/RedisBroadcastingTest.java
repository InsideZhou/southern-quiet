package test.broadcasting;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.event.EventPublisher;
import com.ai.southernquiet.event.RedisEventAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, RedisEventAutoConfiguration.class})
public class RedisBroadcastingTest {
    @Autowired
    private EventPublisher eventPublisher;

    @Test
    public void sendSuccess() {
        eventPublisher.publish(new BroadcastingDone());
    }

    @Test(expected = ClassCastException.class)
    public void sendFailure() {
        eventPublisher.publish(new NonSerializableEvent());
    }
}
