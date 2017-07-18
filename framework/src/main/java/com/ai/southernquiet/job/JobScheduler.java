package com.ai.southernquiet.job;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

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

    @Scheduled
    public void process() {
        Job job = queue.dequeue();
        if (null == job) return;

        Exception exception = executeJob(job);

        if (null != exception) {
            job.onFail(exception);
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
