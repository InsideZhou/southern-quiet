package me.insidezhou.southernquiet.throttle.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Throttle(timeUnit = TimeUnit.MILLISECONDS)
@Scheduled
public @interface ThrottledSchedule {
    @AliasFor(annotation = Throttle.class)
    String name() default "";

    @AliasFor(annotation = Scheduled.class)
    String cron() default "";

    @AliasFor(annotation = Scheduled.class)
    String zone() default "";

    @AliasFor(annotation = Scheduled.class)
    long fixedDelay() default -1;

    @AliasFor(annotation = Scheduled.class)
    String fixedDelayString() default "";

    @AliasFor(annotation = Scheduled.class)
    long fixedRate() default -1;

    @AliasFor(annotation = Scheduled.class)
    String fixedRateString() default "";

    @AliasFor(annotation = Scheduled.class)
    long initialDelay() default -1;

    @AliasFor(annotation = Scheduled.class)
    String initialDelayString() default "";
}
