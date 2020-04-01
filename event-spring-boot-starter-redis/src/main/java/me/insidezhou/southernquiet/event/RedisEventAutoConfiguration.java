package me.insidezhou.southernquiet.event;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.driver.FstSerializationRedisSerializer;
import me.insidezhou.southernquiet.event.driver.RedisEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
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
}
