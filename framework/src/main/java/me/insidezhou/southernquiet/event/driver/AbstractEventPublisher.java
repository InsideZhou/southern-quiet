package me.insidezhou.southernquiet.event.driver;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.event.EventPublisher;
import me.insidezhou.southernquiet.event.ShouldBroadcast;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.annotation.AnnotationUtils;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractEventPublisher<E> implements EventPublisher<E>, ApplicationEventPublisherAware {
    private ApplicationEventPublisher applicationEventPublisher;
    private String[] defaultChannel;

    public AbstractEventPublisher(FrameworkAutoConfiguration.EventProperties properties) {
        this.defaultChannel = properties.getDefaultChannels();
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishToLocalOnly(E event) {
        applicationEventPublisher.publishEvent(event);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Override
    public void publish(E event) {
        ShouldBroadcast annotation = AnnotationUtils.getAnnotation(event.getClass(), ShouldBroadcast.class);
        if (null != annotation) {
            String[] channels = annotation.channels();
            if (null == channels || 0 == channels.length) {
                channels = defaultChannel;
            }

            broadcast(event, channels);
        } else {
            publishToLocalOnly(event);
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
