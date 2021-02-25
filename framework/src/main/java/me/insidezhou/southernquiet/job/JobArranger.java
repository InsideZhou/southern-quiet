package me.insidezhou.southernquiet.job;

/**
 * 编排任务。
 */
public interface JobArranger<J> {
    /**
     * 编排任务。
     */
    default void arrange(J job) {
        arrange(job, 0);
    }

    /**
     * 在指定延迟后编排任务。
     *
     * @param delay 要延迟的时间，单位：毫秒。
     */
    void arrange(J job, int delay);
}
