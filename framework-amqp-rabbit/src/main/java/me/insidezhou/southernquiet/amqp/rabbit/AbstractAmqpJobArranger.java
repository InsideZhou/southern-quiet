package me.insidezhou.southernquiet.amqp.rabbit;

import me.insidezhou.southernquiet.job.JobArranger;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractAmqpJobArranger<J> implements JobArranger<J> {
    @Override
    public void arrange(J job) {
        int delay = 0;

        DelayedMessage annotation = AnnotatedElementUtils.findMergedAnnotation(job.getClass(), DelayedMessage.class);
        if (null != annotation) {
            delay = annotation.delay();
        }

        arrange(job, delay);
    }

    public static String getQueueSource(Class<?> cls) {
        MessageSource annotation = AnnotationUtils.getAnnotation(cls, MessageSource.class);
        return null == annotation || StringUtils.isEmpty(annotation.source()) ? cls.getSimpleName() : annotation.source();
    }

    public static String getExchange(String prefix, Class<?> cls) {
        return getExchange(prefix, getQueueSource(cls));
    }

    public static String getExchange(String prefix, String source) {
        return prefix + "EXCHANGE." + source;
    }

    public static String getRouting(String prefix, Class<?> cls) {
        return getRouting(prefix, getQueueSource(cls));
    }

    public static String getRouting(String prefix, String source) {
        return prefix + source;
    }
}
