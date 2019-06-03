package me.insidezhou.southernquiet.job;

/**
 * 任务引擎。
 * 任务的执行由异步调用{@link JobProcessor}完成。
 */
public interface JobEngine<T> {
    /**
     * 编排任务，编排失败时会立即抛出运行时异常。
     */
    void arrange(T job);
}
