package me.insidezhou.southernquiet.amqp.rabbit;

import me.insidezhou.southernquiet.notification.NotificationPublisher;
import org.springframework.core.annotation.AnnotatedElementUtils;

public abstract class AbstractAmqpNotificationPublisher<N> implements NotificationPublisher<N> {
    @Override
    public void publish(N notification) {
        long delay = 0;

        DelayedMessage annotation = AnnotatedElementUtils.findMergedAnnotation(notification.getClass(), DelayedMessage.class);
        if (null != annotation) {
            delay = annotation.delay();
        }

        publish(notification, delay);
    }
}
