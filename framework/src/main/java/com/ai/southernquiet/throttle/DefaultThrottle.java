package com.ai.southernquiet.throttle;

@SuppressWarnings("WeakerAccess")
public class DefaultThrottle implements Throttle {
    private Long lastOpenedAt;
    private long counter;

    public void setLastOpenedAt(Long lastOpenedAt) {
        this.lastOpenedAt = lastOpenedAt;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public Long getElapsed() {
        return null == lastOpenedAt ? 0 : System.currentTimeMillis() - lastOpenedAt;
    }

    public long getCounter() {
        return counter;
    }

    @Override
    public boolean openByTime(long threshold) {
        long counter = getCounter();
        Long elapsed = getElapsed();

        if (null == elapsed || elapsed >= threshold) {
            setLastOpenedAt(System.currentTimeMillis());
            setCounter(0);

            return true;
        }
        else {
            setCounter(counter + 1);

            return false;
        }
    }

    @Override
    public boolean openByCount(long threshold) {
        long counter = getCounter();

        if (counter > threshold) {
            setLastOpenedAt(System.currentTimeMillis());
            setCounter(0);

            return true;
        }
        else {
            setCounter(counter + 1);

            return false;
        }
    }
}
