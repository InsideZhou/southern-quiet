package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.broadcasting.Broadcaster;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

public class RedisBroadcaster implements Broadcaster {
    private RedisTemplate redisTemplate;
    private RedisSerializer redisSerializer;
    private RedisSerializer stringRedisSerializer;

    public RedisBroadcaster(RedisTemplate redisTemplate, RedisSerializer redisSerializer) {
        this.redisTemplate = redisTemplate;
        this.redisSerializer = redisSerializer;
        this.stringRedisSerializer = redisTemplate.getStringSerializer();
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public <E> void broadcast(E event, String[] channels) {
        byte[] message = redisSerializer.serialize(event);

        redisTemplate.execute((RedisConnection connection) -> {
            for (String channel : channels) {
                connection.publish(stringRedisSerializer.serialize(channel), message);
            }

            return null;
        });
    }
}
