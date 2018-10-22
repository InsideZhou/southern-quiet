package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.JobEngine;
import com.ai.southernquiet.job.JobProcessor;
import com.ai.southernquiet.util.AsyncRunner;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractJobEngine<T> implements JobEngine<T>, ApplicationContextAware {
    protected Map<Class<T>, JobProcessor<T>> jobHandlerMap = new ConcurrentHashMap<>();
    protected List<JobProcessor<T>> jobProcessorList = Collections.emptyList();
    protected AsyncRunner asyncRunner;

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        jobProcessorList = new ArrayList(applicationContext.getBeansOfType(JobProcessor.class).values());
        asyncRunner = applicationContext.getBean(AsyncRunner.class);
    }

    @SuppressWarnings("unchecked")
    protected JobProcessor<T> getProcessor(T job) {
        Class<T> jobClass = (Class<T>) job.getClass();

        JobProcessor<T> processor = jobHandlerMap.get(jobClass);
        if (null != processor) return processor;

        Optional<JobProcessor<T>> optional = jobProcessorList.stream()
            .filter(p -> {
                boolean matched = p.getJobClass() == jobClass;
                if (matched) {
                    jobHandlerMap.put(jobClass, p);
                }

                return matched;
            })
            .limit(1)
            .findFirst();

        if (!optional.isPresent()) {
            optional = jobProcessorList.stream()
                .filter(p -> {
                    boolean matched = p.getJobClass().isAssignableFrom(jobClass);
                    if (matched) {
                        jobHandlerMap.put(jobClass, p);
                    }

                    return matched;
                })
                .limit(1)
                .findFirst();
        }

        return optional.orElseThrow(() -> new ProcessorNotFoundException(jobClass.getName()));
    }
}
