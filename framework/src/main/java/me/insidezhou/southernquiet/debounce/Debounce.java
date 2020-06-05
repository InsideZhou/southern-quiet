package me.insidezhou.southernquiet.debounce;

import org.springframework.context.event.EventListener;

import java.lang.annotation.*;

/**
 * 对容器内bean的方法去除执行抖动，控制方法执行频率，但最终会得到执行。
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Debounce {
    /**
     * 去除多长时间内的抖动。单位：毫秒。
     */
    long waitFor() default 5000;

    /**
     * 去抖动最多持续的时间。单位：毫秒。
     */
    long maxWaitFor() default 60000;

    /**
     * 支持SPEL，参考{@link EventListener#condition()}。
     * root对象是{@link DebounceAdvice.EvaluationRoot}
     */
    String name() default "";
}
