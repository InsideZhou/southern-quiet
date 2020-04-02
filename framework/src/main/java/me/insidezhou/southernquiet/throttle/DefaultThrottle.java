package me.insidezhou.southernquiet.throttle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class DefaultThrottle implements Throttle {

    DefaultThrottle() {
    }

    /**
     * 上一次清除缓存时间
     */
    private long lastRefreshAt = System.currentTimeMillis();

    private Map<String, Long> shouldOpenedAtMap = new HashMap<>();

    private void setShouldOpenedAt(String key, Long shouldOpenedAt) {
        shouldOpenedAtMap.put(key, shouldOpenedAt);
    }

    /**
     * 以时间为依据打开节流器，上次打开之后必须至少节流了指定时间才能再次打开，如果打开失败返回false。
     */
    @Override
    public synchronized boolean open(String key, long threshold) {
        long now = System.currentTimeMillis();

        //若过了至少10分钟后，去清理缓存数据
        if (now - lastRefreshAt >= 600000) {
            refresh(now);
        }

        long shouldOpenedAt = shouldOpenedAtMap.getOrDefault(key, 0L);
        if (now >= shouldOpenedAt) {
            setShouldOpenedAt(key, now + threshold);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 清除数据，以免shouldOpenedAtMap的数据越来越多占用内存
     */
    private void refresh(long now) {
        if (shouldOpenedAtMap.size() <= 0) {
            return;
        }

        Iterator<Map.Entry<String, Long>> it = shouldOpenedAtMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            Long shouldOpenedAt = entry.getValue();
            if (shouldOpenedAt != null && now > shouldOpenedAt) {
                it.remove();
            }
        }
        lastRefreshAt = now;
    }

}
