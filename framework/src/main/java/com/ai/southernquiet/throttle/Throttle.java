package com.ai.southernquiet.throttle;

import org.springframework.util.Assert;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Throttle {
    /**
     * 节流器的开启计数器
     */
    long getCounter();

    /**
     * 节流器的存活时间，单位：毫秒
     */
    long getElapsed();

    void open() throws CannotOpenException;

    default void run(Runnable runnable) {
        Assert.notNull(runnable, "要执行的任务不能为空");

        try {
            open();
        }
        catch (CannotOpenException e) {
            return;
        }

        runnable.run();
    }

    /**
     * 为consumer提供的参数为：计数器
     */
    default void run(Consumer<Long> consumer) {
        Assert.notNull(consumer, "要执行的任务不能为空");

        try {
            open();
        }
        catch (Throttle.CannotOpenException e) {
            return;
        }

        consumer.accept(getCounter());
    }

    /**
     * 为consumer提供的参数为：计数器、存活时间
     */
    default void run(BiConsumer<Long, Long> consumer) {
        Assert.notNull(consumer, "要执行的任务不能为空");

        try {
            open();
        }
        catch (Throttle.CannotOpenException e) {
            return;
        }

        consumer.accept(getCounter(), getElapsed());
    }

    @SuppressWarnings("WeakerAccess")
    class CannotOpenException extends Exception {
        /**
         * estimated time to arrival，还剩多少时间可以打开，单位：毫秒，-1意味着打开跟时间无关。
         */
        private long eta;

        /**
         * estimated count to arrival，还剩多少次计数可以打开，-1意味着打开跟计数器无关。
         */
        private long etaByCount;

        public CannotOpenException(long eta, long etaByCount) {
            this.eta = eta;
            this.etaByCount = etaByCount;
        }

        public CannotOpenException(long eta) {
            this(eta, -1);
        }

        public long getEta() {
            return eta;
        }

        public long getEtaByCount() {
            return etaByCount;
        }

        @Override
        public String toString() {
            return "CannotOpenException{" +
                "eta=" + eta +
                ", etaByCount=" + etaByCount +
                "} " + super.toString();
        }
    }
}
