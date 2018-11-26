package test.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NotificationTestApp {
    public static void main(String[] args) {
        SpringApplication.run(NotificationTestApp.class, args);
    }

    @Bean
    public NotificationTest.Listener listener() {
        return new NotificationTest.Listener();
    }
}
