package test.debounce;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.debounce.Debounce;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

@SpringBootTest(classes = {FrameworkAutoConfiguration.class, DebounceTest.Config.class})
@ExtendWith(SpringExtension.class)
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
        Assertions.assertEquals(2.0, Math.round(64 / 30.0), 0.0);

        eventPublisher.publishEvent(new WorkerEvent(1));
        eventPublisher.publishEvent(new WorkerEvent(1));
        eventPublisher.publishEvent(new WorkerEvent(1));
        eventPublisher.publishEvent(new WorkerEvent(1));
        eventPublisher.publishEvent(new WorkerEvent(2));
        eventPublisher.publishEvent(new WorkerEvent(2));
        eventPublisher.publishEvent(new WorkerEvent(2));

        Thread.sleep(3000);
        Assertions.assertEquals(2, WorkerEventListener.counter);
    }

    public static class WorkerEventListener {
        private static volatile int counter = 0;

        @SuppressWarnings("NonAtomicOperationOnVolatileField")
        @EventListener
        @Debounce(waitFor = 1000, name = "#root.getDefaultName() + '_' + #event.getId()", isSpELName = true, executionTimeout = 10)
        public void work(WorkerEvent event) {
            ++counter;
            log.message("被debounce的worker正在工作中").context("worker", event.id).context("uuid", event.uuid).context("counter", counter).info();
        }
    }

    public static class WorkerEvent {
        private UUID uuid = UUID.randomUUID();

        private final int id;

        public WorkerEvent(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }
    }
}
