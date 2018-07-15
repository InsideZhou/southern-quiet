package com.ai.southernquiet.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Async;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AsyncJobQueue<T> implements JobQueue<T>, ApplicationContextAware {
    private final static Log log = LogFactory.getLog(AsyncJobQueue.class);

    protected Map<Class<T>, JobHandler<T>> jobHandlerMap = new ConcurrentHashMap<>();
    protected List<JobHandler<T>> jobHandlerList = Collections.emptyList();

    @Override
    public void enqueue(T job) {
        process(job);
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        jobHandlerList = new ArrayList(applicationContext.getBeansOfType(JobHandler.class).values());
    }

    @SuppressWarnings({"SuspiciousMethodCalls", "unchecked"})
    @Async
    protected void process(T job) {
        Class<T> jobClass = (Class<T>) job.getClass();
        JobHandler<T> handler = jobHandlerMap.get(jobClass);

        if (null == handler) {
            Optional<JobHandler<T>> optional = jobHandlerList.stream()
                .filter(h -> {
                    boolean matched = h.getJobClass() == jobClass;
                    if (matched) {
                        jobHandlerMap.put(jobClass, h);
                    }

                    return matched;
                })
                .limit(1)
                .findFirst();

            if (optional.isPresent()) {
                handler = optional.get();
            }
            else {
                log.info("没有发现有效的JobHandler：" + jobClass.getName());
                return;
            }
        }

        try {
            handler.handle(job);
            onJobSuccess(job);
        }
        catch (Exception e) {
            log.error("Job处理失败：", e);
            try {
                onJobFail(job, e);
            }
            catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    protected void onJobSuccess(T job) {
        log.debug("Job处理完成：" + job.toString());
    }

    abstract protected void onJobFail(T job, Exception e) throws Exception;
}
