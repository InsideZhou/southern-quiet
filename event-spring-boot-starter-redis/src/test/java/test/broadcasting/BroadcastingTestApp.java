package test.broadcasting;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.RedisEventAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, RedisEventAutoConfiguration.class})
public class BroadcastingTestApp {
    private static Logger log = LoggerFactory.getLogger(BroadcastingTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(BroadcastingTestApp.class);
    }

    @EventListener
    public void testListener(BroadcastingDone broadcastingDone) {
        log.debug("{} {}", broadcastingDone.getClass().getSimpleName(), broadcastingDone.getId());
    }

    @EventListener
    public void testCustomChannel(BroadcastingCustomChannel broadcastingCustomChannel) {
        log.info("{} {}", broadcastingCustomChannel.getClass().getSimpleName(), broadcastingCustomChannel.getId());
    }
}
