package test.broadcasting;

import me.insidezhou.southernquiet.event.EventPubSub;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static me.insidezhou.southernquiet.event.EventPubSub.CustomApplicationEventChannel;

@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisBroadcastingTest {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(RedisBroadcastingTest.class);

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
    public void channels() {
        Set<String> channels = eventPubSub.getListeningChannels();

        Assert.assertTrue(channels.containsAll(Arrays.asList(CustomApplicationEventChannel, "haha", "TEST.CHANNEL")));
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
            log.message("testListener")
                .context("name", broadcastingDone.getClass().getSimpleName())
                .context("id", broadcastingDone.getId())
                .debug();
        }

        @EventListener
        public void duplicate(BroadcastingDone broadcastingDone) {
            log.message("duplicate")
                .context("name", broadcastingDone.getClass().getSimpleName())
                .context("id", broadcastingDone.getId())
                .debug();
        }

        @EventListener
        public void testCustomChannelListener(BroadcastingCustomChannel broadcastingCustomChannel) {
            log.message("testCustomChannelListener")
                .context("name", broadcastingCustomChannel.getClass().getSimpleName())
                .context("id", broadcastingCustomChannel.getId())
                .debug();

            testCustomChannelListenerMap.merge(broadcastingCustomChannel.getId().toString(), 1, Integer::sum);
        }

        @EventListener
        public void hahaListener(HahaEvent hahaEvent) {
            log.message("haha")
                .context("guid", hahaEvent.getGuid())
                .debug();
        }
    }
}
