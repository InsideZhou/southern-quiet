package me.insidezhou.southernquiet.util;

import org.springframework.scheduling.annotation.Async;

@Async
public class AsyncRunner {
    public void run(Runnable runnable) {
        runnable.run();
    }
}
