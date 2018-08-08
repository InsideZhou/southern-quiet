package com.ai.southernquiet.util;

import org.springframework.scheduling.annotation.Async;

public class AsyncRunner {
    @Async
    public void run(Runnable runnable) {
        runnable.run();
    }
}
