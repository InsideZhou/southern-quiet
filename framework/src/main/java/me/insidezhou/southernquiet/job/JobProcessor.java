package me.insidezhou.southernquiet.job;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Inherited
@Documented
@Repeatable(JobProcessor.List.class)
public @interface JobProcessor {
    /**
     * 要处理的任务类。
     */
    Class<?> job();

    /**
     * 处理器的名称。
     */
    String name() default "";

    /**
     * recover时要使用的amplifier bean在容器内的名字
     */
    String amplifierBeanName() default "";

    @SuppressWarnings("unused")
    @Target({ElementType.METHOD})
    @Retention(RUNTIME)
    @Inherited
    @Documented
    @interface List {
        JobProcessor[] value();
    }
}
