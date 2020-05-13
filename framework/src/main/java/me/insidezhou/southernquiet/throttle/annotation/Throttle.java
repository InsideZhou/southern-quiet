package me.insidezhou.southernquiet.throttle.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Throttle {
    @AliasFor("threshold")
    long value() default -1;

    @AliasFor("value")
    long threshold() default -1;

    /**
     * 若为空则使用类名#方法名作为节流器名称。
     */
    String name() default "";

    /**
     * 若为空则创建计数器节流器，否则创建时间节流器。
     */
    TimeUnit[] timeUnit() default {};
}
