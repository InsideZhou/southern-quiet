package me.insidezhou.southernquiet.debounce;

import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;

public class DefaultDebouncer implements Debouncer {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(DefaultDebouncer.class);

    private final long waitFor;
    private final long maxWaitFor;

    private long firstBounceAt = 0;
    private long lastBounceAt = 0;

    public DefaultDebouncer(long waitFor, long maxWaitFor) {
        this.waitFor = waitFor;
        this.maxWaitFor = maxWaitFor;
    }

    @Override
    public synchronized boolean isStable() {
        if (0 == firstBounceAt || 0 == lastBounceAt) return false;

        long now = System.currentTimeMillis();
        long interval = now - lastBounceAt;
        long maxWait = now - firstBounceAt;

        log.message("正在检查抖动去除情况")
            .context(context -> {
                context.put("hashCode", this.hashCode());
                context.put("interval", interval);
                context.put("maxWait", maxWait);
            })
            .trace();

        if (interval >= waitFor || maxWait >= maxWaitFor) return reset();

        return false;
    }

    @Override
    public synchronized void bounce() {
        long now = System.currentTimeMillis();
        if (firstBounceAt <= 0) {
            firstBounceAt = now;
        }

        lastBounceAt = now;
    }

    private synchronized boolean reset() {
        firstBounceAt = 0;
        lastBounceAt = 0;
        return true;
    }
}
