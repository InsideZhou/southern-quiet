package me.insidezhou.southernquiet.event.driver;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.RedisTemplateBuilder;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;

import java.io.Serializable;

@SuppressWarnings("rawtypes")
public class RedisEventPublisher<E extends Serializable> extends AbstractEventPublisher<E> {
    private RedisTemplate redisTemplate;
    private RedisSerializer<E> eventSerializer;
    private RedisSerializer channelSerializer;

    public RedisEventPublisher(RedisTemplateBuilder<E> builder, FrameworkAutoConfiguration.EventProperties properties) {
        super(properties);

        this.redisTemplate = builder.getRedisTemplate();
        this.eventSerializer = builder.getEventSerializer();
        this.channelSerializer = builder.getChannelSerializer();
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    protected void broadcast(E event, String[] channels) {
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
