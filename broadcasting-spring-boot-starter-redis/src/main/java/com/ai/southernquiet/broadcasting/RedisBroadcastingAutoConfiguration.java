package com.ai.southernquiet.broadcasting;

import com.ai.southernquiet.broadcasting.driver.FstSerializationRedisSerializer;
import com.ai.southernquiet.broadcasting.driver.RedisBroadcaster;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisBroadcastingAutoConfiguration {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public RedisBroadcaster redisBroadcaster(RedisTemplate redisTemplate, FstSerializationRedisSerializer fstSerializationRedisSerializer) {
        redisTemplate.setDefaultSerializer(fstSerializationRedisSerializer);
        return new RedisBroadcaster(redisTemplate, fstSerializationRedisSerializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public FstSerializationRedisSerializer fstSerializationRedisSerializer() {
        return new FstSerializationRedisSerializer();
    }
}
