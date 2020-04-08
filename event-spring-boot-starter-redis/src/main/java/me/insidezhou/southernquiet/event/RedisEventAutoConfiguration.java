package me.insidezhou.southernquiet.event;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.driver.FstSerializationRedisSerializer;
import me.insidezhou.southernquiet.event.driver.RedisEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "rawtypes"})
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisEventAutoConfiguration {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public RedisEventPublisher redisEventPublisher(RedisTemplateBuilder builder, FrameworkAutoConfiguration.EventProperties properties) {
        return new RedisEventPublisher<>(builder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FstSerializationRedisSerializer fstSerializationRedisSerializer() {
        return new FstSerializationRedisSerializer<>();
    }

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplateBuilder redisTemplateBuilder(FstSerializationRedisSerializer eventSerializer, RedisConnectionFactory connectionFactory) {
        return new RedisTemplateBuilder<>(eventSerializer, connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomApplicationEventRedisRelay customApplicationEventRelay(RedisTemplateBuilder builder, RedisMessageListenerContainer redisMessageListenerContainer, ApplicationContext applicationContext) {
        return new CustomApplicationEventRedisRelay(builder, redisMessageListenerContainer, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }
}
