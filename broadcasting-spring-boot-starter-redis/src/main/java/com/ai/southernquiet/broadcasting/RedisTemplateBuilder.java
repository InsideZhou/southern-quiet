package com.ai.southernquiet.broadcasting;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.Serializable;

public class RedisTemplateBuilder<T extends Serializable> {
    private RedisTemplate redisTemplate;
    private RedisSerializer<T> eventSerializer;
    private RedisSerializer channelSerializer;

    public RedisTemplateBuilder(RedisSerializer<T> eventSerializer, RedisConnectionFactory connectionFactory) {
        this(eventSerializer, null, connectionFactory);
    }

    @SuppressWarnings("unchecked")
    public RedisTemplateBuilder(RedisSerializer<T> eventSerializer, RedisSerializer channelSerializer, RedisConnectionFactory connectionFactory) {
        this.redisTemplate = new RedisTemplate();
        redisTemplate.setDefaultSerializer(eventSerializer);
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        this.eventSerializer = eventSerializer;
        if (null == channelSerializer) {
            this.channelSerializer = redisTemplate.getStringSerializer();
        }
        else {
            this.channelSerializer = channelSerializer;
        }

    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RedisSerializer<T> getEventSerializer() {
        return eventSerializer;
    }

    public void setEventSerializer(RedisSerializer<T> eventSerializer) {
        this.eventSerializer = eventSerializer;
    }

    public RedisSerializer getChannelSerializer() {
        return channelSerializer;
    }

    public void setChannelSerializer(RedisSerializer channelSerializer) {
        this.channelSerializer = channelSerializer;
    }
}
