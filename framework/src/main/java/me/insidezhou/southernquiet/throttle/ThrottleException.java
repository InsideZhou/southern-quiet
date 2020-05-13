package me.insidezhou.southernquiet.throttle;

import java.util.concurrent.TimeUnit;

public class ThrottleException extends Exception {
    public ThrottleException(String throttleName, long threshold, TimeUnit timeUnit) {
        super("throttleName='" + throttleName + '\'' +
            ", threshold=" + threshold +
            ", timeUnit=" + timeUnit);

        this.throttleName = throttleName;
        this.threshold = threshold;
        this.timeUnit = timeUnit;
    }

    private String throttleName;
    private long threshold;
    private TimeUnit timeUnit;

    public String getThrottleName() {
        return throttleName;
    }

    public void setThrottleName(String throttleName) {
        this.throttleName = throttleName;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
