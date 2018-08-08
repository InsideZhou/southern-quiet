package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.JobProcessor;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.AsyncRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务加入队列时在当前ApplicationContext中立即被异步处理的队列。
 * 没有找到相应处理器时，会抛出ProcessorNotFoundException。
 */
public abstract class OnSiteJobQueue<T> implements JobQueue<T>, ApplicationContextAware {
    private final static Log log = LogFactory.getLog(OnSiteJobQueue.class);

    protected Map<Class<T>, JobProcessor<T>> jobHandlerMap = new ConcurrentHashMap<>();
    protected List<JobProcessor<T>> jobProcessorList = Collections.emptyList();
    protected AsyncRunner asyncRunner;

    @Override
    public void enqueue(T job) {
        asyncRunner.run(() -> {
            process(job, getProcessor(job));
        });
    }

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


        return optional.orElseThrow(() -> {
            ProcessorNotFoundException e = new ProcessorNotFoundException(jobClass.getName());
            onJobFail(job, e);
            return e;
        });
    }

    protected void process(T job, JobProcessor<T> processor) {
        try {
            processor.process(job);
            onJobSuccess(job);
        }
        catch (Exception e) {
            log.error("Job处理失败：", e);

            onJobFail(job, e);
        }
    }

    protected void onJobSuccess(T job) {
        log.debug("Job处理完成：" + job.toString());
    }

    abstract protected void onJobFail(T job, Exception e);
}
