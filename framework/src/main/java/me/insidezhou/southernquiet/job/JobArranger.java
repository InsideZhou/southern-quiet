package me.insidezhou.southernquiet.job;

/**
 * 编排任务。
 */
public interface JobArranger<J> {
    /**
     * 编排任务。
     */
    void arrange(J notification);
}
