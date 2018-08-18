package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.broadcasting.Broadcaster;
import com.ai.southernquiet.broadcasting.Publisher;
import com.ai.southernquiet.broadcasting.ShouldBroadcast;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

public class DefaultPublisher implements Publisher, ApplicationEventPublisherAware {
    private ApplicationEventPublisher applicationEventPublisher;
    private Broadcaster broadcaster;
    private String[] defaultChannel;

    public DefaultPublisher(Broadcaster broadcaster, FrameworkAutoConfiguration.BroadcastingProperties properties) {
        this.broadcaster = broadcaster;
        this.defaultChannel = properties.getDefaultChannels();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishToLocalOnly(Object event) {
        applicationEventPublisher.publishEvent(event);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void publish(Object event) {
        publishToLocalOnly(event);

        ShouldBroadcast annotation = AnnotationUtils.getAnnotation(event.getClass(), ShouldBroadcast.class);
        if (null != annotation) {
            String[] channels = annotation.channels();
            if (null == channels || 0 == channels.length) {
                channels = defaultChannel;
            }

            broadcaster.broadcast(event, channels);
        }
    }
}
