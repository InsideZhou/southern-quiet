package me.insidezhou.southernquiet.event.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.RedisTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class RedisEventPubSub<E extends Serializable> extends AbstractEventPubSub<E> {
    private final static Logger log = LoggerFactory.getLogger(RedisEventPubSub.class);

    public final static String EventIdKeyName = "EventId";

    private final RedisTemplate redisTemplate;
    private final RedisSerializer<E> eventSerializer;
    private final RedisSerializer channelSerializer;
    private final ObjectMapper objectMapper;

    private final RedisMessageListenerContainer container;

    public RedisEventPubSub(RedisTemplateBuilder<E> builder,
                            ObjectMapper objectMapper,
                            FrameworkAutoConfiguration.EventProperties properties,
                            ApplicationContext applicationContext) {

        super(properties, applicationContext);

        this.objectMapper = objectMapper;
        this.redisTemplate = builder.getRedisTemplate();
        this.eventSerializer = builder.getEventSerializer();
        this.channelSerializer = builder.getChannelSerializer();

        this.container = new RedisMessageListenerContainer();
        container.setConnectionFactory(builder.getConnectionFactory());
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    protected void broadcast(E event, String[] channels, String eventId) {
        Assert.notNull(event, "null事件无法发布");

        Map map = objectMapper.convertValue(event, Map.class);
        map.put(EventIdKeyName, eventId);

        byte[] message = eventSerializer.serialize((E) map);

        redisTemplate.execute((RedisConnection connection) -> {
            for (String channel : channels) {
                connection.publish(channelSerializer.serialize(channel), message);
            }

            return null;
        });
    }

    @Override
    protected void initChannel(String channel) {
        if (log.isDebugEnabled()) {
            log.debug("创建RedisMessageListener: channel={}", channel);
        }

        container.addMessageListener(this::onMessage, new ChannelTopic(channel));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        container.afterPropertiesSet();
        container.start();
    }

    protected void onMessage(Message message, byte[] pattern) {
        byte[] data = message.getBody();

        if (log.isDebugEnabled()) {
            log.debug(
                "收到事件\tchannel={}, pattern={}, data={}",
                channelSerializer.deserialize(message.getChannel()),
                redisTemplate.getStringSerializer().deserialize(pattern),
                new String(data)
            );
        }

        Object event = eventSerializer.deserialize(data);
        if (null == event) {
            log.warn(
                "收到空事件\tchannel={}, pattern={}, data={}",
                channelSerializer.deserialize(message.getChannel()),
                redisTemplate.getStringSerializer().deserialize(pattern),
                new String(data)
            );

            return;
        }

        getApplicationEventPublisher().publishEvent(event);
    }
}
