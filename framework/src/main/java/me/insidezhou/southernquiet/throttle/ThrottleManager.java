package me.insidezhou.southernquiet.throttle;

/**
 * 节流管理器
 */
public interface ThrottleManager {
    String DEFAULT_THROTTLE_NAME = "southernquiet.throttle";

    default Throttle getTimeBased() {
        return getTimeBased(DEFAULT_THROTTLE_NAME);
    }

    /**
     * 获取基于时间的节流器
     */
    Throttle getTimeBased(String throttleName);

    default Throttle getCountBased() {
        return getCountBased(DEFAULT_THROTTLE_NAME);
    }

    /**
     * 获取基于次数的节流器
     */
    Throttle getCountBased(String throttleName);
}
