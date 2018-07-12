package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.broadcasting.Broadcaster;
import com.ai.southernquiet.broadcasting.Publisher;
import com.ai.southernquiet.broadcasting.ShouldBroadcast;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

public class DefaultPublisher<E> implements Publisher<E>, ApplicationEventPublisherAware {
    private ApplicationEventPublisher applicationEventPublisher;
    private Broadcaster<E> broadcaster;
    private String[] defaultChannel;

    public DefaultPublisher(Broadcaster<E> broadcaster, FrameworkAutoConfiguration.BroadcastingProperties properties) {
        this.broadcaster = broadcaster;
        this.defaultChannel = properties.getDefaultChannels();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public <T> void publishToLocalOnly(T event) {
        applicationEventPublisher.publishEvent(event);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void publish(E event) {
        publishToLocalOnly(event);

        ShouldBroadcast annotation = event.getClass().getAnnotation(ShouldBroadcast.class);
        if (null != annotation) {
            String[] channels = annotation.channels();
            if (null == channels || 0 == channels.length) {
                channels = defaultChannel;
            }

            broadcaster.broadcast(event, channels);
        }
    }
}
