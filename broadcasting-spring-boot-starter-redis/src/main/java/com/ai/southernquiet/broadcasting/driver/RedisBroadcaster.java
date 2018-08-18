package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.broadcasting.Broadcaster;
import com.ai.southernquiet.broadcasting.RedisTemplateBuilder;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;

import java.io.Serializable;

public class RedisBroadcaster<T extends Serializable> implements Broadcaster<T> {
    private RedisTemplate redisTemplate;
    private RedisSerializer<T> eventSerializer;
    private RedisSerializer channelSerializer;

    public RedisBroadcaster(RedisTemplateBuilder<T> builder) {
        this.redisTemplate = builder.getRedisTemplate();
        this.eventSerializer = builder.getEventSerializer();
        this.channelSerializer = builder.getChannelSerializer();
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void broadcast(T event, String[] channels) {
        Assert.notNull(event, "null事件无法发布");

        byte[] message = eventSerializer.serialize(event);

        redisTemplate.execute((RedisConnection connection) -> {
            for (String channel : channels) {
                connection.publish(channelSerializer.serialize(channel), message);
            }

            return null;
        });
    }
}
