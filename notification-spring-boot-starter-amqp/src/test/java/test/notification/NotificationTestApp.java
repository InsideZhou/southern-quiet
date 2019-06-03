package test.notification;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.notification.AmqpNotificationAutoConfiguration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, AmqpNotificationAutoConfiguration.class})
public class NotificationTestApp {
    public static void main(String[] args) {
        SpringApplication.run(NotificationTestApp.class, args);
    }

    @Bean
    public static RabbitTransactionManager rabbitTransactionManager(ConnectionFactory connectionFactory) {
        RabbitTransactionManager manager = new RabbitTransactionManager();
        manager.setConnectionFactory(connectionFactory);
        return manager;
    }

    @Bean
    public NotificationTest.Listener listener() {
        return new NotificationTest.Listener();
    }
}
