package com.ai.southernquiet.broadcasting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.PostConstruct;

import static com.ai.southernquiet.broadcasting.Broadcaster.CustomApplicationEventChannel;

public class CustomApplicationEventRedisRelay implements ApplicationEventPublisherAware {
    private final static Log log = LogFactory.getLog(CustomApplicationEventRedisRelay.class);

    private RedisTemplate redisTemplate;
    private RedisSerializer eventSerializer;
    private RedisSerializer channelSerializer;

    private ApplicationEventPublisher applicationEventPublisher;
    private RedisMessageListenerContainer container;

    public CustomApplicationEventRedisRelay(RedisTemplateBuilder builder, RedisConnectionFactory redisConnectionFactory) {
        this.redisTemplate = builder.getRedisTemplate();
        this.eventSerializer = builder.getEventSerializer();
        this.channelSerializer = builder.getChannelSerializer();

        this.container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
    }

    @PostConstruct
    public void postConstruct() {
        container.addMessageListener(this::onMessage, new ChannelTopic(CustomApplicationEventChannel));
    }

    protected void onMessage(Message message, byte[] pattern) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                "CustomApplicationEventRelay在 %s 频道收到事件，pattern=%s",
                channelSerializer.deserialize(message.getChannel()),
                redisTemplate.getStringSerializer().deserialize(pattern)
            ));
        }

        Object event = eventSerializer.deserialize(message.getBody());
        applicationEventPublisher.publishEvent(event);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
