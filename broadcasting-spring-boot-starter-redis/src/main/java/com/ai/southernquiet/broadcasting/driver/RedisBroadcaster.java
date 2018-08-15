package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.broadcasting.Broadcaster;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.Serializable;

public class RedisBroadcaster<T extends Serializable> implements Broadcaster<T> {
    private RedisTemplate redisTemplate;
    private RedisSerializer<T> redisSerializer;
    private RedisSerializer stringRedisSerializer;

    public RedisBroadcaster(RedisConnectionFactory redisConnectionFactory, RedisSerializer<T> redisSerializer) {
        this.redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        this.redisSerializer = redisSerializer;
        this.stringRedisSerializer = redisTemplate.getStringSerializer();
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void broadcast(T event, String[] channels) {
        byte[] message = redisSerializer.serialize(event);

        redisTemplate.execute((RedisConnection connection) -> {
            for (String channel : channels) {
                connection.publish(stringRedisSerializer.serialize(channel), message);
            }

            return null;
        });
    }
}
