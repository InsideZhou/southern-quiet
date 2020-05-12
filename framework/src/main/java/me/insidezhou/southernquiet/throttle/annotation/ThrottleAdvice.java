package me.insidezhou.southernquiet.throttle.annotation;

import me.insidezhou.southernquiet.throttle.ThrottleManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ThrottleAdvice implements MethodBeforeAdvice {
    private final ThrottleManager throttleManager;

    public ThrottleAdvice(ThrottleManager throttleManager) {
        this.throttleManager = throttleManager;
    }

    @Override
    public void before(@NotNull Method method, @NotNull Object[] args, Object target) throws Throwable {
        Throttle annotation = AnnotationUtils.getAnnotation(method, Throttle.class);

        String throttleName = Objects.requireNonNull(annotation).throttleName();
        long threshold = annotation.threshold();
        TimeUnit[] timeUnits = annotation.timeUnit();

        if (StringUtils.isEmpty(throttleName)) {
            throttleName = method.getDeclaringClass().getName() + "#" + method.getName();
        }

        me.insidezhou.southernquiet.throttle.Throttle throttle;
        if (timeUnits.length > 0) {
            //time based
            throttle = throttleManager.getTimeBased(throttleName);

            TimeUnit timeUnit = timeUnits[0];
            threshold = timeUnit.toMillis(threshold);
        }
        else {
            //count based
            throttle = throttleManager.getCountBased(throttleName);
        }

        if (!throttle.open(threshold)) throw new ThrottleException();
    }
}
