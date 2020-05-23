package test.throttle;

import me.insidezhou.southernquiet.throttle.RedisThrottleAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportAutoConfiguration({RedisThrottleAutoConfiguration.class})
public class RedisThrottleTestApp {
    public static void main(String[] args) {
        SpringApplication.run(RedisThrottleTestApp.class, args);
    }
}
