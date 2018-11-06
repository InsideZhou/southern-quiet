package com.ai.southernquiet.event.driver;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.event.EventPublisher;
import com.ai.southernquiet.event.ShouldBroadcast;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.annotation.AnnotationUtils;

public abstract class AbstractEventPublisher<E> implements EventPublisher<E>, ApplicationEventPublisherAware {
    private ApplicationEventPublisher applicationEventPublisher;
    private String[] defaultChannel;

    public AbstractEventPublisher(FrameworkAutoConfiguration.EventProperties properties) {
        this.defaultChannel = properties.getDefaultChannels();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishToLocalOnly(E event) {
        applicationEventPublisher.publishEvent(event);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void publish(E event) {
        publishToLocalOnly(event);

        ShouldBroadcast annotation = AnnotationUtils.getAnnotation(event.getClass(), ShouldBroadcast.class);
        if (null != annotation) {
            String[] channels = annotation.channels();
            if (null == channels || 0 == channels.length) {
                channels = defaultChannel;
            }

            broadcast(event, channels);
        }
    }

    /**
     * 广播事件。
     *
     * @param event    事件
     * @param channels 频道
     */
    abstract protected void broadcast(E event, String[] channels);
}
