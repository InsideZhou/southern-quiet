package me.insidezhou.southernquiet.event.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.RedisTemplateProvider;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.springframework.beans.factory.DisposableBean;
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
import java.util.Objects;

@SuppressWarnings("rawtypes")
public class RedisEventPubSub<E extends Serializable> extends AbstractEventPubSub<E> implements DisposableBean {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(RedisEventPubSub.class);

    public final static String EventTypeIdName = "TypeId";

    private final RedisTemplate redisTemplate;
    private final RedisSerializer<E> eventSerializer;
    private final RedisSerializer channelSerializer;
    private final ObjectMapper objectMapper;

    private final RedisMessageListenerContainer container;

    public RedisEventPubSub(RedisTemplateProvider<E> provider,
                            ObjectMapper objectMapper,
                            FrameworkAutoConfiguration.EventProperties properties,
                            ApplicationContext applicationContext) {

        super(properties, applicationContext);

        this.objectMapper = objectMapper;
        this.redisTemplate = provider.getRedisTemplate();
        this.eventSerializer = provider.getEventSerializer();
        this.channelSerializer = provider.getChannelSerializer();

        this.container = new RedisMessageListenerContainer();
        container.setConnectionFactory(provider.getConnectionFactory());
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    protected void broadcast(E event, String[] channels, String eventType) {
        Assert.notNull(event, "null事件无法发布");

        Map map = objectMapper.convertValue(event, Map.class);
        map.put(EventTypeIdName, eventType);

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
        log.message("创建RedisMessageListener").context("channel", channel).debug();

        container.addMessageListener(this::onMessage, new ChannelTopic(channel));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        container.afterPropertiesSet();
        container.start();
    }

    protected void onMessage(Message message, byte[] patternRaw) {
        String channel = Objects.requireNonNull(channelSerializer.deserialize(message.getChannel())).toString();
        String pattern = Objects.requireNonNull(redisTemplate.getStringSerializer().deserialize(patternRaw)).toString();
        byte[] data = message.getBody();

        onMessageReceived(channel, data, pattern);

        E event = eventSerializer.deserialize(data);
        if (null == event) {
            log.message("收到空事件")
                .context(context -> {
                    context.put("channel", channel);
                    context.put("pattern", pattern);
                    context.put("data", new String(data));
                })
                .trace();

            return;
        }

        onEventDeserialized(channel, event, pattern);
    }

    protected void onMessageReceived(String channel, byte[] data, String pattern) {
        log.message("收到事件")
            .context(context -> {
                context.put("channel", channel);
                context.put("pattern", pattern);
                context.put("data", new String(data));
            })
            .debug();
    }

    @SuppressWarnings("unused")
    protected void onEventDeserialized(String channel, E event, String pattern) {
        publishToLocalOnly(event);
    }

    @Override
    public void destroy() throws Exception {
        container.destroy();
    }
}
