package me.insidezhou.southernquiet.throttle.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 节流器注解，在method上使用，对该method进行节流。
 * <p>timeUnit可空，若为空则创建计数器节流器，否则创建时间节流器；
 * <p>throttleName可空，若为空则使用类名#方法名作为节流器名称
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Throttle {

    String throttleName() default "";

    long threshold();

    TimeUnit[] timeUnit() default {};

}
