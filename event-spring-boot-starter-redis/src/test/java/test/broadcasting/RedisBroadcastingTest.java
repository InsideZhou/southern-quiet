package test.broadcasting;

import me.insidezhou.southernquiet.event.EventPubSub;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisBroadcastingTest {
    private final static Logger log = LoggerFactory.getLogger(RedisBroadcastingTest.class);

    @Autowired
    private EventPubSub eventPubSub;

    public static Map<String, Integer> testCustomChannelListenerMap = new ConcurrentHashMap<>();

    @Test
    public void sendSuccess() {
        eventPubSub.publish(new BroadcastingDone());
        eventPubSub.publish(new ChildBroadcastingDone());
    }

    @Test(expected = ClassCastException.class)
    public void sendFailure() {
        eventPubSub.publish(new NonSerializableEvent());
    }

    @Test
    public void sendCustomChannel() throws InterruptedException {
        BroadcastingCustomChannel broadcastingCustomChannel = new BroadcastingCustomChannel();
        eventPubSub.publish(broadcastingCustomChannel);
        Thread.sleep(1000);
        Integer count = testCustomChannelListenerMap.get(broadcastingCustomChannel.getId().toString());
        Assert.assertNotNull(count);
        Assert.assertEquals(1, count.intValue());
    }

    public static class Listener {
        @EventListener
        public void testListener(BroadcastingDone broadcastingDone) {
            log.debug("{} {}", broadcastingDone.getClass().getSimpleName(), broadcastingDone.getId());
        }

        @EventListener
        public void testCustomChannelListener(BroadcastingCustomChannel broadcastingCustomChannel) {
            log.debug("{} {}", broadcastingCustomChannel.getClass().getSimpleName(), broadcastingCustomChannel.getId());
            testCustomChannelListenerMap.merge(broadcastingCustomChannel.getId().toString(), 1, Integer::sum);
        }
    }
}
