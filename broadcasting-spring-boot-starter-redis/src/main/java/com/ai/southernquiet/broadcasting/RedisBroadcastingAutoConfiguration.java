package com.ai.southernquiet.broadcasting;

import com.ai.southernquiet.broadcasting.driver.FstSerializationRedisSerializer;
import com.ai.southernquiet.broadcasting.driver.RedisBroadcaster;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisBroadcastingAutoConfiguration {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public RedisBroadcaster redisBroadcaster(RedisTemplateBuilder builder) {
        return new RedisBroadcaster(builder);
    }

    @Bean
    @ConditionalOnMissingBean
    public FstSerializationRedisSerializer fstSerializationRedisSerializer() {
        return new FstSerializationRedisSerializer();
    }

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplateBuilder redisTemplateBuilder(FstSerializationRedisSerializer eventSerializer, RedisConnectionFactory connectionFactory) {
        return new RedisTemplateBuilder(eventSerializer, connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplateBuilder builder) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(builder.getRedisTemplate().getConnectionFactory());

        return container;
    }
}
