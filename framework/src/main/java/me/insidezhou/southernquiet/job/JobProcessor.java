package me.insidezhou.southernquiet.job;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Inherited
@Documented
public @interface JobProcessor {
    /**
     * 要处理的任务类。
     */
    Class<?> job();

    /**
     * 处理器的名称。
     */
    String name() default "";
}
