package com.ai.southernquiet.broadcasting.driver;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.broadcasting.Broadcaster;
import com.ai.southernquiet.broadcasting.Publisher;
import com.ai.southernquiet.broadcasting.ShouldBroadcast;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.annotation.AnnotationUtils;

public class DefaultPublisher implements Publisher, ApplicationEventPublisherAware {
    private final static Log log = LogFactory.getLog(DefaultPublisher.class);

    private ApplicationEventPublisher applicationEventPublisher;
    private Broadcaster broadcaster;
    private String[] defaultChannel;

    public DefaultPublisher(FrameworkAutoConfiguration.BroadcastingProperties properties) {
        this.defaultChannel = properties.getDefaultChannels();
    }

    public Broadcaster getBroadcaster() {
        return broadcaster;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired(required = false)
    public void setBroadcaster(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
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
            if (null == broadcaster) {
                log.warn("事件被ShouldBroadcast标注，但没有找到broadcaster");
                return;
            }

            String[] channels = annotation.channels();
            if (null == channels || 0 == channels.length) {
                channels = defaultChannel;
            }

            broadcaster.broadcast(event, channels);
        }
    }
}
