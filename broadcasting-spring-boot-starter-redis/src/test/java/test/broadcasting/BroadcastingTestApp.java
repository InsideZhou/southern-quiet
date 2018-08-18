package test.broadcasting;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.broadcasting.CustomApplicationEventRedisRelay;
import com.ai.southernquiet.broadcasting.RedisBroadcastingAutoConfiguration;
import com.ai.southernquiet.broadcasting.RedisTemplateBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@SuppressWarnings({"unchecked", "ConstantConditions"})
@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, RedisBroadcastingAutoConfiguration.class})
public class BroadcastingTestApp {
    private static Log log = LogFactory.getLog(BroadcastingTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(BroadcastingTestApp.class);
    }

    @Bean
    public static CustomApplicationEventRedisRelay customApplicationEventRelay(RedisTemplateBuilder builder, RedisMessageListenerContainer container) {
        return new CustomApplicationEventRedisRelay(builder, container);
    }

    @EventListener
    public void testListener(BroadcastingDone broadcastingDone) {
        log.debug(broadcastingDone.getId());
    }
}
