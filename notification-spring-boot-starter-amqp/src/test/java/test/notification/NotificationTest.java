package test.notification;

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
    }
}
