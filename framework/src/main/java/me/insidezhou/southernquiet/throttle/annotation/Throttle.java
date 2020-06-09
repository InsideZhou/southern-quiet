package me.insidezhou.southernquiet.throttle.annotation;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Throttle {
    long DefaultThreshold = -1;

    @AliasFor("threshold")
    long value() default DefaultThreshold;

    @AliasFor("value")
    long threshold() default DefaultThreshold;

    /**
     * 支持SpEL，参考{@link EventListener#condition()}。
     * root对象是{@link me.insidezhou.southernquiet.throttle.ThrottleAdvice.EvaluationRoot}
     */
    String name() default "";

    /**
     * {@link #name()}是否SpEL字符串，默认false。
     */
    boolean isSpELName() default false;

    /**
     * 若为空则创建计数器节流器，否则创建时间节流器。
     */
    TimeUnit[] timeUnit() default {};
}
