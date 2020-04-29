package test.broadcasting;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

@SpringBootApplication
public class BroadcastingTestApp {
    public static void main(String[] args) {
        SpringApplication.run(BroadcastingTestApp.class);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ChildBroadcastingDone childBroadcastingDone() {
        return new ChildBroadcastingDone();
    }

    @Bean
    public RedisBroadcastingTest.Listener listener() {
        return new RedisBroadcastingTest.Listener();
    }
}
