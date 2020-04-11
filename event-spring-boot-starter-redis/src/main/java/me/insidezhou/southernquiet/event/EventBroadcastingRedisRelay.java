package me.insidezhou.southernquiet.event;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public class EventBroadcastingRedisRelay implements ApplicationEventPublisherAware {
    private final static Logger log = LoggerFactory.getLogger(EventBroadcastingRedisRelay.class);

    private RedisTemplate redisTemplate;
    private RedisSerializer eventSerializer;
    private RedisSerializer channelSerializer;

    private ApplicationEventPublisher applicationEventPublisher;
    private RedisMessageListenerContainer container;
    private FrameworkAutoConfiguration.EventProperties eventProperties;

    private ApplicationContext applicationContext;

    public EventBroadcastingRedisRelay(RedisTemplateBuilder builder, RedisConnectionFactory redisConnectionFactory, FrameworkAutoConfiguration.EventProperties eventProperties, ApplicationContext applicationContext) {
        this.redisTemplate = builder.getRedisTemplate();
        this.eventSerializer = builder.getEventSerializer();
        this.channelSerializer = builder.getChannelSerializer();
        this.eventProperties = eventProperties;
        this.applicationContext = applicationContext;

        this.container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
    }

    @PostConstruct
    public void postConstruct() {
        Arrays.stream(applicationContext.getBeanDefinitionNames())
            .map(name -> {
                try {
                    return applicationContext.getBean(name);
                }
                catch (BeansException e) {
                    log.info("查找EventListener时，bean未能初始化: name={}", name);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(bean ->
                Arrays.stream(ReflectionUtils.getAllDeclaredMethods(bean.getClass()))
                    .forEach(method -> {
                        AnnotationUtils.getRepeatableAnnotations(method, EventListener.class)
                            .forEach(listener -> {
                                if (log.isDebugEnabled()) {
                                    log.debug(
                                        "找到EventListener：{}#{}",
                                        bean.getClass().getSimpleName(),
                                        method.getName()
                                    );
                                }

                                initListener(method);
                            });
                    })
            );

        container.afterPropertiesSet();
        container.start();
    }

    private void initListener(Method method) {
        Arrays.stream(method.getParameterTypes())
            .flatMap(parameterType -> {
                ShouldBroadcast annotation = AnnotationUtils.getAnnotation(parameterType, ShouldBroadcast.class);
                if (null == annotation) return Stream.empty();

                return Arrays.stream(0 == annotation.channels().length ? eventProperties.getDefaultChannels() : annotation.channels());
            })
            .distinct()
            .forEach(this::addMessageListener);
    }

    private void addMessageListener(String channel) {
        if (log.isDebugEnabled()) {
            log.debug("创建RedisMessageListener: channel={}", channel);
        }

        container.addMessageListener(this::onMessage, new ChannelTopic(channel));
    }

    protected void onMessage(Message message, byte[] pattern) {
        if (log.isDebugEnabled()) {
            log.debug(
                "收到事件: channel={}, pattern={}",
                channelSerializer.deserialize(message.getChannel()),
                redisTemplate.getStringSerializer().deserialize(pattern)
            );
        }

        Object event = eventSerializer.deserialize(message.getBody());
        if (null == event) {
            log.warn(
                "收到空事件: channel={}, pattern={}",
                channelSerializer.deserialize(message.getChannel()),
                redisTemplate.getStringSerializer().deserialize(pattern)
            );

            return;
        }

        applicationEventPublisher.publishEvent(event);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
