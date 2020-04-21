package test.broadcasting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BroadcastingTestApp {
    public static void main(String[] args) {
        SpringApplication.run(BroadcastingTestApp.class);
    }

    @Bean
    public RedisBroadcastingTest.Listener listener() {
        return new RedisBroadcastingTest.Listener();
    }
}
