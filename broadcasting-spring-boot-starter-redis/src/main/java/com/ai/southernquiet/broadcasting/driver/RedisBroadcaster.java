package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.broadcasting.Broadcaster;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.Serializable;

public class RedisBroadcaster<T extends Serializable> implements Broadcaster<T> {
    private RedisTemplate redisTemplate;
    private RedisSerializer<T> eventSerializer;
    private RedisSerializer channelSerializer;

    public RedisBroadcaster(RedisConnectionFactory redisConnectionFactory, RedisSerializer<T> eventSerializer) {
        this(redisConnectionFactory, eventSerializer, null);
    }

    @SuppressWarnings("unchecked")
    public RedisBroadcaster(RedisConnectionFactory redisConnectionFactory, RedisSerializer<T> eventSerializer, RedisSerializer channelSerializer) {
        this.redisTemplate = new RedisTemplate();
        redisTemplate.setDefaultSerializer(eventSerializer);
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();

        this.eventSerializer = eventSerializer;

        if (null == channelSerializer) {
            this.channelSerializer = redisTemplate.getStringSerializer();
        }
        else {
            this.channelSerializer = channelSerializer;
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void broadcast(T event, String[] channels) {
        byte[] message = eventSerializer.serialize(event);

        redisTemplate.execute((RedisConnection connection) -> {
            for (String channel : channels) {
                connection.publish(channelSerializer.serialize(channel), message);
            }

            return null;
        });
    }
}
