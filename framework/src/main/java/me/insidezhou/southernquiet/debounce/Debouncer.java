package me.insidezhou.southernquiet.debounce;

public interface Debouncer {
    /**
     * 抖动是否已稳定。
     *
     * @return 抖动稳定则返回true。
     */
    boolean isStable();

    /**
     * 抖动一次。
     */
    void bounce();
}
