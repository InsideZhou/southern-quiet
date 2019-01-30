package com.ai.southernquiet.throttle;

import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 在{@link FixedFrequencyThrottle}的基础上，增加任务计划。
 * 任务被安排进节流器时不会立即得到执行，会等待到下一次节流器自动打开的时候，除非该任务被新的任务取代。
 * 本节流器是定期自动打开的，不要试图手工打开，否则会出现未定义的行为。
 */
public class ScheduledFixedFrequencyThrottle extends FixedFrequencyThrottle {
    private Object latestRunnable;
    private ScheduledExecutorService executorService;

    /**
     * 任务被执行前去除了多少次抖动。
     */
    private long debouncedCount = 0;

    private synchronized void setLatestRunnable(Object latestRunnable) {
        this.latestRunnable = latestRunnable;

        if (null == latestRunnable) {
            this.debouncedCount = 0;
        }
        else {
            this.debouncedCount += 1;
        }
    }

    public long getDebouncedCount() {
        return debouncedCount;
    }

    public ScheduledFixedFrequencyThrottle(long frequency) {
        super(frequency);

        this.executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::run, 0, frequency, TimeUnit.MICROSECONDS);
    }

    @Override
    public void run(Runnable runnable) {
        Assert.notNull(runnable, "要执行的任务不能为空");
        setLatestRunnable(runnable);
    }

    @Override
    public void run(Consumer<Long> consumer) {
        Assert.notNull(consumer, "要执行的任务不能为空");
        setLatestRunnable(consumer);
    }

    @Override
    public void run(BiConsumer<Long, Long> consumer) {
        Assert.notNull(consumer, "要执行的任务不能为空");
        setLatestRunnable(consumer);
    }

    @SuppressWarnings("unchecked")
    public void run() {
        if (null == latestRunnable) return;

        try {
            open();
        }
        catch (Throttle.CannotOpenException e) {
            return;
        }

        Object runnable = latestRunnable;
        setLatestRunnable(null);

        if (runnable instanceof Consumer<?>) {
            Consumer<Long> counterConsumer = (Consumer<Long>) runnable;
            counterConsumer.accept(getCounter());
        }
        else if (runnable instanceof BiConsumer<?, ?>) {
            BiConsumer<Long, Long> biConsumer = (BiConsumer<Long, Long>) runnable;
            biConsumer.accept(getCounter(), getElapsed());
        }
        else {
            ((Runnable) runnable).run();
        }
    }

    @SuppressWarnings("unused")
    @EventListener
    public void shutdown(ContextStoppedEvent event) {
        if (null == executorService) return;

        executorService.shutdown();
        executorService = null;
    }
}
