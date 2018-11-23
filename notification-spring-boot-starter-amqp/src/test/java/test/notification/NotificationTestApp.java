package test.notification;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackageClasses = {FrameworkAutoConfiguration.class})
public class NotificationTestApp {
    public static void main(String[] args) {
        SpringApplication.run(NotificationTestApp.class, args);
    }

    @Bean
    public NotificationTest.Listener listener() {
        return new NotificationTest.Listener();
    }
}
