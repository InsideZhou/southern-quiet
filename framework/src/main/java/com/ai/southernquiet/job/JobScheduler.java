package com.ai.southernquiet.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务调度器。
 * <p>调度器会查找任务实例中首参数类型是JobScheduler的方法并执行，其余参数会实时从ApplicationContext中查找。</p>
 * <p>如找不到方法，会执行{@link Job#execute()}</p>
 */
public class JobScheduler {
    private Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    private JobQueue queue;
    private ApplicationContext context;

    public JobScheduler(JobQueue queue, ApplicationContext context) {
        this.queue = queue;
        this.context = context;
    }

    /**
     * 将一项任务安排进计划。
     */
    public <T extends Job> void schedule(T job) {
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

        Optional<Method> opt = Arrays.stream(job.getClass().getMethods())
            .filter(m -> {
                Class<?>[] paramTypes = m.getParameterTypes();

                return paramTypes.length > 0 && JobScheduler.class.isAssignableFrom(paramTypes[0]);
            })
            .findAny();

        boolean retryNeeded = false;
        try {
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
            logger.warn(String.format("未能成功调用Job %s的处理方法", job.getId()), e);
            retryNeeded = true;
        }

        job.setExecutionCount(job.getExecutionCount() + 1);
        if (retryNeeded && job.getRetryLimit() >= job.getExecutionCount()) {
            queue.enqueue(job);
            return;
        }

        unschedule(job);
    }
}
