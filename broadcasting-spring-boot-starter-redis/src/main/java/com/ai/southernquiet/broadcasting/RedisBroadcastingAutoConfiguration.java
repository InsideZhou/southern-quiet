package com.ai.southernquiet.broadcasting;

import com.ai.southernquiet.broadcasting.driver.FstSerializationRedisSerializer;
import com.ai.southernquiet.broadcasting.driver.RedisBroadcaster;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisBroadcastingAutoConfiguration {
    @SuppressWarnings("unchecked")
    public RedisBroadcaster redisBroadcaster(RedisTemplate redisTemplate, FstSerializationRedisSerializer fstSerializationRedisSerializer) {
        redisTemplate.setDefaultSerializer(fstSerializationRedisSerializer);
        return new RedisBroadcaster(redisTemplate, fstSerializationRedisSerializer);
    }

    public FstSerializationRedisSerializer fstSerializationRedisSerializer() {
        return new FstSerializationRedisSerializer();
    }
}
