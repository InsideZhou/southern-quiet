package com.ai.southernquiet.throttle;

public class DebouncedThrottle implements Throttle {
    /**
     * 节流器什么时候创建的
     */
    private long createdAt = System.currentTimeMillis();

    /**
     * 节流器开启计数器
     */
    private long counter = 0;

    /**
     * 抖动清除开始的时间
     */
    private Long debouncedStartedAt;

    /**
     * 上次抖动清除的时间
     */
    private Long latestDebouncedAt;

    /**
     * 清除抖动的等待时间
     */
    private long waiting;

    /**
     * 抖动清除最多可持续的时间
     */
    private Long maxDebouncedTime;

    /**
     * 节流器在下次打开前去除了多少次抖动。
     */
    private long debouncedCount = 0;

    /**
     * 抖动最多可清除多少次
     */
    private Long maxDebouncedCount;

    public DebouncedThrottle(long waiting, Long maxDebouncedTime) {
        this.waiting = waiting;
        this.maxDebouncedTime = maxDebouncedTime;
    }

    @SuppressWarnings("unused")
    public DebouncedThrottle(long waiting, Long maxDebouncedTime, Long maxDebouncedCount) {
        this.waiting = waiting;
        this.maxDebouncedTime = maxDebouncedTime;
        this.maxDebouncedCount = maxDebouncedCount;
    }

    @SuppressWarnings("unused")
    public DebouncedThrottle(Long maxDebouncedCount) {
        this.maxDebouncedCount = maxDebouncedCount;
    }

    @Override
    public long elapsed() {
        return System.currentTimeMillis() - createdAt;
    }

    @Override
    public long counter() {
        return counter;
    }

    @Override
    synchronized public boolean open() {
        if (null == debouncedStartedAt) {
            startDebounce();
            return false;
        }

        if (debouncedTimeExceeded() || maxDebouncedTimeReached() || maxDebouncedCountReached()) {
            terminateDebounce();
            return true;
        }

        continueDebounce();
        return false;
    }

    private long totalDebouncedTime() {
        return System.currentTimeMillis() - debouncedStartedAt;
    }

    private long debouncedTime() {
        return System.currentTimeMillis() - latestDebouncedAt;
    }

    private boolean debouncedTimeExceeded() {
        return debouncedTime() > waiting;
    }

    private boolean maxDebouncedTimeReached() {
        return null != maxDebouncedCount && totalDebouncedTime() >= maxDebouncedTime;
    }

    private boolean maxDebouncedCountReached() {
        return null != maxDebouncedCount && debouncedCount >= maxDebouncedCount;
    }

    private void startDebounce() {
        debouncedStartedAt = System.currentTimeMillis();
        latestDebouncedAt = debouncedStartedAt;
        debouncedCount = 0;
    }

    private void terminateDebounce() {
        debouncedStartedAt = null;
        debouncedCount = -1;

        counter += 1;
    }

    private void continueDebounce() {
        latestDebouncedAt = System.currentTimeMillis();
        debouncedCount += 1;
    }
}
