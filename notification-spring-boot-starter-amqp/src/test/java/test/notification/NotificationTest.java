package test.notification;

import me.insidezhou.southernquiet.amqp.rabbit.DelayedMessage;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.NotificationListener;
import me.insidezhou.southernquiet.notification.NotificationPublisher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationTest {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(NotificationTest.class);

    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class Config {}

    @Autowired
    private NotificationPublisher<Serializable> notificationPublisher;

    @Test
    public void dummy() {
        notificationPublisher.publish(new StandardNotification());
    }

    @Test
    public void delay() {
        notificationPublisher.publish(new DelayedNotification());
    }

    public static class Listener {
        @NotificationListener(notification = StandardNotification.class, name = "a")
        @NotificationListener(notification = StandardNotification.class, name = "b")
        @NotificationListener(notification = StandardNotification.class, name = "e")
        public void standard(StandardNotification notification, NotificationListener listener) {
            log.message("使用监听器接到通知")
                .context("listenerName", listener.name())
                .context("id", notification.getId())
                .info();
        }

        @NotificationListener(notification = StandardNotification.class, name = "e")
        public void exception(StandardNotification notification, NotificationListener listener) {
            throw new RuntimeException("在通知中抛出异常通知：listener=" + listener.name() + ", notification=" + notification.getId());
        }

        @NotificationListener(notification = DelayedNotification.class)
        public void delay(DelayedNotification notification, NotificationListener listener, DelayedMessage delayedAnnotation) {
            log.message("使用监听器接到延迟通知")
                .context("listenerName", listener.name())
                .context("delay", delayedAnnotation)
                .context("duration", Duration.between(notification.publishedAt, Instant.now()))
                .info();
        }
    }

    public static class StandardNotification implements Serializable {
        private UUID id = UUID.randomUUID();

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }
    }

    @DelayedMessage(3000)
    public static class DelayedNotification implements Serializable {
        private Instant publishedAt = Instant.now();

        public Instant getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
        }
    }
}
