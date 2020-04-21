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

    /**
     * 事件类型标识与实际运行时类型的映射，用于跨语言事件支持。
     */
    public final static ConcurrentMap<String, Class<?>> EventTypeMap = new ConcurrentHashMap<>();

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
            broadcast(event, getEventChannel(annotation), getEventTypeId(eventClass, annotation));
        }
        else {
            publishToLocalOnly(event);
        }
    }

    protected String[] getEventChannel(ShouldBroadcast annotation) {
        return annotation.channels().length > 0 ? annotation.channels() : defaultChannel;
    }

    public String getEventTypeId(Class<?> eventClass, ShouldBroadcast annotation) {
        if (null != annotation && !StringUtils.isEmpty(annotation.typeId())) return annotation.typeId();

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
                    log.info("查找EventListener时，bean未能初始化\tname={}", name);
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
                                        "找到EventListener\t{}#{}",
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
    abstract protected void broadcast(E event, String[] channels, String eventType);

    protected void initChannel(String channel) {}

    protected void initListener(Method method) {
        Arrays.stream(method.getParameterTypes())
            .flatMap(Stream::of)
            .filter(parameterType -> !BeanUtils.isSimpleValueType(parameterType))
            .flatMap(parameterType -> {
                ShouldBroadcast annotation = AnnotationUtils.getAnnotation(parameterType, ShouldBroadcast.class);
                if (null == annotation) return Stream.empty();

                String[] channels = 0 == annotation.channels().length ? eventProperties.getDefaultChannels() : annotation.channels();

                String typeId = getEventTypeId(parameterType, annotation);
                EventTypeMap.put(typeId, parameterType);
                log.info("已订阅事件\ttypeId={}, channels={}, event={}", typeId, channels, parameterType.getName());

                Reflections reflections = new Reflections(parameterType);
                reflections.getSubTypesOf(parameterType).forEach(subType -> {
                    String subTypeId = getEventTypeId(subType, annotation);
                    EventTypeMap.putIfAbsent(subTypeId, subType);
                    log.info("已订阅子事件\ttypeId={}, channels={}, event={}", subTypeId, channels, parameterType.getName());
                });

                return Arrays.stream(channels);
            })
            .distinct()
            .forEach(this::initChannel);
    }
}
