package me.insidezhou.southernquiet.event.driver;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.EventPubSub;
import me.insidezhou.southernquiet.event.ShouldBroadcast;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractEventPubSub<E> implements EventPubSub<E>, InitializingBean, ApplicationEventPublisherAware {
    private final static Logger log = LoggerFactory.getLogger(AbstractEventPubSub.class);

    public final static ConcurrentMap<String, Class<?>> EventIdTypeMap = new ConcurrentHashMap<>();

    private ApplicationEventPublisher applicationEventPublisher;

    private final FrameworkAutoConfiguration.EventProperties eventProperties;
    private final ApplicationContext applicationContext;
    private final String[] defaultChannel;

    public AbstractEventPubSub(FrameworkAutoConfiguration.EventProperties eventProperties, ApplicationContext applicationContext) {
        this.defaultChannel = eventProperties.getDefaultChannels();
        this.eventProperties = eventProperties;
        this.applicationContext = applicationContext;
    }

    public ApplicationEventPublisher getApplicationEventPublisher() {
        return applicationEventPublisher;
    }

    @Override
    public void setApplicationEventPublisher(@NotNull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishToLocalOnly(E event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(E event) {
        Class<?> eventClass = event.getClass();
        ShouldBroadcast annotation = AnnotationUtils.getAnnotation(eventClass, ShouldBroadcast.class);
        if (null != annotation) {
            broadcast(event, getEventChannel(annotation), getEventId(eventClass, annotation));
        }
        else {
            publishToLocalOnly(event);
        }
    }

    protected String[] getEventChannel(ShouldBroadcast annotation) {
        return annotation.channels().length > 0 ? annotation.channels() : defaultChannel;
    }

    public String getEventId(Class<?> eventClass, ShouldBroadcast annotation) {
        if (null != annotation && !StringUtils.isEmpty(annotation.eventId())) return annotation.eventId();

        return eventClass.getSimpleName();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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
    }

    /**
     * 广播事件。
     *
     * @param event    事件
     * @param channels 频道
     */
    abstract protected void broadcast(E event, String[] channels, String eventId);

    protected void initListener(Method method) {
        Arrays.stream(method.getParameterTypes())
            .flatMap(Stream::of)
            .filter(parameterType -> !BeanUtils.isSimpleValueType(parameterType))
            .flatMap(parameterType -> {
                ShouldBroadcast annotation = AnnotationUtils.getAnnotation(parameterType, ShouldBroadcast.class);
                EventIdTypeMap.put(getEventId(parameterType, annotation), parameterType);

                Reflections reflections = new Reflections(parameterType);
                reflections.getSubTypesOf(parameterType).forEach(subType -> EventIdTypeMap.putIfAbsent(getEventId(subType, annotation), subType));

                if (null == annotation) return Stream.empty();

                return Arrays.stream(0 == annotation.channels().length ? eventProperties.getDefaultChannels() : annotation.channels());
            })
            .distinct()
            .forEach(this::initChannel);
    }

    protected void initChannel(String channel) {}
}
