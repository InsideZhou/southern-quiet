package test.broadcasting;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.EventPublisher;
import me.insidezhou.southernquiet.event.RedisEventAutoConfiguration;
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
        eventPublisher.publish(new ChildBroadcastingDone());
    }

    @Test(expected = ClassCastException.class)
    public void sendFailure() {
        eventPublisher.publish(new NonSerializableEvent());
    }
}
