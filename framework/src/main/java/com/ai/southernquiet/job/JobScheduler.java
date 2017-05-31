package com.ai.southernquiet.job;

import com.ai.southernquiet.FrameworkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务调度器。
 * <p>调度器会查找任务实例中首参数类型是JobScheduler的方法并执行，其余参数会实时从ApplicationContext中查找。</p>
 * <p>如找不到方法，会执行{@link Job#execute()}</p>
 * <p>当超过重试次数时，调用{@link Job#onFail()}通知任务做最终处理，然后将该任务移出执行计划。</p>
 */
public class JobScheduler {
    private Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    private JobQueue queue;
    private JobQueue retryQueue;
    private ApplicationContext context;
    private int retryLimit;

    public JobScheduler(JobQueue queue, JobQueue retryQueue, ApplicationContext context, FrameworkProperties properties) {
        this.queue = queue;
        this.retryQueue = retryQueue;
        this.context = context;

        if (null != properties.getJob().getRetryLimit()) {
            this.retryLimit = properties.getJob().getRetryLimit();
        }
    }

    /**
     * 将一项任务安排进计划。
     */
    public <T extends Job> void schedule(T job) {
        queue.enqueue(job);
    }

    /**
     * 将一项任务安排进计划，并使用全局计划配置覆盖任务状态（如最大重试次数）。
     */
    public <T extends Job> void scheduleWithGlobal(T job) {
        job.setRetryLimit(retryLimit);
        queue.enqueue(job);
    }

    /**
     * 从计划中移除一项任务。
     */
    public <T extends Job> void unschedule(T job) {
        queue.remove(job);
    }

    protected synchronized void process() {
        Job job = queue.dequeue();
        if (null == job) return;

        Exception exception = executeJob(job);

        if (job.isPostpone()) {
            job.setPostpone(false);
            queue.enqueue(job);
        }

        job.setExecutionCount(job.getExecutionCount() + 1);
        if (null != exception) {
            retryQueue.enqueue(job);
        }
    }

    protected synchronized void processRetry() {
        Job job = retryQueue.dequeue();
        if (null == job) return;

        Exception exception = executeJob(job);

        if (job.isPostpone()) {
            job.setPostpone(false);
            retryQueue.enqueue(job);
        }

        job.setExecutionCount(job.getExecutionCount() + 1);
        if (null != exception) {
            if (job.getRetryLimit() >= job.getExecutionCount()) {
                retryQueue.enqueue(job);
            }
            else {
                logger.warn(String.format("未能成功调用Job %s %d的处理方法", job.getId(), job.getExecutionCount()), exception);
                job.onFail();
            }
        }
    }

    private Exception executeJob(Job job) {
        Optional<Method> opt = Arrays.stream(job.getClass().getMethods())
            .filter(m -> {
                Class<?>[] paramTypes = m.getParameterTypes();

                return paramTypes.length > 0 && JobScheduler.class.isAssignableFrom(paramTypes[0]);
            })
            .findAny();

        try {
            job.setLastExecutionTime(Instant.now());

            if (opt.isPresent()) {
                Method method = opt.get();
                List<Object> params = new ArrayList<>();
                params.add(this);
                params.addAll(Arrays.stream(method.getParameterTypes()).skip(1).map(cls -> context.getBean(cls)).collect(Collectors.toList()));
                method.invoke(job, params.toArray());
            }
            else {
                job.execute();
            }
        }
        catch (Exception e) {
            return e;
        }

        return null;
    }
}
