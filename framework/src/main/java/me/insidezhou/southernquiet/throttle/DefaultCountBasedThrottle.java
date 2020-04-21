package me.insidezhou.southernquiet.throttle;

@SuppressWarnings("WeakerAccess")
public class DefaultCountBasedThrottle implements Throttle {

    private long counter = 0;

    @Override
    public synchronized boolean open(long threshold) {
        if (threshold <= 0) return reset();
        if (counter++ >= threshold) return reset();

        return false;
    }

    private boolean reset() {
        counter = 0;
        return true;
    }
}
