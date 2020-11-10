package test.notification;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import me.insidezhou.southernquiet.notification.NotificationListener;
import me.insidezhou.southernquiet.notification.NotificationPublisher;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, AmqpNotificationAutoConfiguration.class})
@RestController
public class NotificationTestApp {
    public static void main(String[] args) {
        SpringApplication.run(NotificationTestApp.class, args);
    }

    @ConditionalOnMissingBean
    @Bean
    public static RabbitTransactionManager rabbitTransactionManager(ConnectionFactory connectionFactory) {
        RabbitTransactionManager manager = new RabbitTransactionManager();
        manager.setConnectionFactory(connectionFactory);
        return manager;
    }

    @Autowired
    private NotificationPublisher<Serializable> notificationPublisher;
    @Resource
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @RequestMapping("pushMessage")
    public void pushMessage(Integer count) {

        Collection<MessageListenerContainer> listenerContainers = rabbitListenerEndpointRegistry.getListenerContainers();

        if (count == null) {
            count = 1;
        }
        for (int i = 0; i < count; i++) {
            ConcurrentNotification concurrentNotification = new ConcurrentNotification();
            notificationPublisher.publish(concurrentNotification);
        }
    }

    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(NotificationTestApp.class);

    @NotificationListener(notification = ConcurrentNotification.class, concurrency = 2)
    public void concurrent(ConcurrentNotification notification, NotificationListener listener) {

        log.message("使用并发监听器接到通知")
            .context("listenerName", listener.name())
            .context("listenerConcurrent", listener.concurrency())
            .context("notificationId", notification.getId())
            .info();
    }

    public static class ConcurrentNotification implements Serializable {
        private UUID id = UUID.randomUUID();

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }
    }

}
