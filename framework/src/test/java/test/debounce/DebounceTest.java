package test.debounce;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.debounce.Debounce;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@SpringBootTest(classes = {FrameworkAutoConfiguration.class, DebounceTest.Config.class})
@RunWith(SpringRunner.class)
public class DebounceTest {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(DebounceTest.class);

    @Configuration
    public static class Config {
        @Bean
        public WorkerEventListener workerEventListener() {
            return new WorkerEventListener();
        }
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    public void debounce() throws Exception {
        eventPublisher.publishEvent(new WorkerEvent());
        eventPublisher.publishEvent(new WorkerEvent());
        eventPublisher.publishEvent(new WorkerEvent());
        eventPublisher.publishEvent(new WorkerEvent());
        eventPublisher.publishEvent(new WorkerEvent());
        eventPublisher.publishEvent(new WorkerEvent());
        eventPublisher.publishEvent(new WorkerEvent());

        Thread.sleep(2000);
        Assert.assertEquals(1, WorkerEventListener.counter);
    }

    public static class WorkerEventListener {
        private static int counter = 0;

        @EventListener
        @Debounce(waitFor = 1000)
        public void work(WorkerEvent event) {
            ++counter;
            log.message("被debounce的worker正在工作中").context("uuid", event.uuid).context("counter", counter).info();
        }
    }

    public static class WorkerEvent {
        private UUID uuid = UUID.randomUUID();

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }
    }
}
