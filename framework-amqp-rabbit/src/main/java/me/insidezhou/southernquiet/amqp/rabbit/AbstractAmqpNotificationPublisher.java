package me.insidezhou.southernquiet.amqp.rabbit;

import me.insidezhou.southernquiet.notification.NotificationPublisher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractAmqpNotificationPublisher<N> implements NotificationPublisher<N> {
    @Override
    public void publish(N notification) {
        int delay = 0;

        DelayedMessage annotation = AnnotatedElementUtils.findMergedAnnotation(notification.getClass(), DelayedMessage.class);
        if (null != annotation) {
            delay = annotation.delay();
        }

        publish(notification, delay);
    }

    public static String getNotificationSource(Class<?> cls) {
        MessageSource annotation = AnnotationUtils.getAnnotation(cls, MessageSource.class);
        return null == annotation || StringUtils.isEmpty(annotation.source()) ? cls.getSimpleName() : annotation.source();
    }

    public static String getRouting(String prefix, Class<?> cls) {
        return getRouting(prefix, getNotificationSource(cls));
    }

    public static String getRouting(String prefix, String source) {
        return prefix + source;
    }

    public static String getDelayRouting(String prefix, Class<?> cls) {
        return getDelayRouting(prefix, getNotificationSource(cls));
    }

    public static String getDelayRouting(String prefix, String source) {
        return prefix + "DELAY." + source;
    }
}
