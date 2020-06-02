package me.insidezhou.southernquiet.amqp.rabbit;

import me.insidezhou.southernquiet.job.JobArranger;
import org.springframework.core.annotation.AnnotatedElementUtils;

public abstract class AbstractAmqpJobArranger<J> implements JobArranger<J> {
    @Override
    public void arrange(J job) {
        long delay = 0;

        DelayedMessage annotation = AnnotatedElementUtils.findMergedAnnotation(job.getClass(), DelayedMessage.class);
        if (null != annotation) {
            delay = annotation.delay();
        }

        arrange(job, delay);
    }
}
