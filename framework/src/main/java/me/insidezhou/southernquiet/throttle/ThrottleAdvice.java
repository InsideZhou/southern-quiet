package me.insidezhou.southernquiet.throttle;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class ThrottleAdvice implements MethodInterceptor {
    private final ThrottleManager throttleManager;

    public ThrottleAdvice(ThrottleManager throttleManager) {
        this.throttleManager = throttleManager;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        me.insidezhou.southernquiet.throttle.annotation.Throttle annotation = AnnotatedElementUtils.findMergedAnnotation(method, me.insidezhou.southernquiet.throttle.annotation.Throttle.class);
        assert annotation != null;

        String throttleName = annotation.name();
        long threshold = annotation.threshold();
        TimeUnit[] timeUnits = annotation.timeUnit();

        if (StringUtils.isEmpty(throttleName)) {
            throttleName = method.getDeclaringClass().getName() + "#" + method.getName();
        }

        me.insidezhou.southernquiet.throttle.Throttle throttle;
        TimeUnit timeUnit = null;

        if (timeUnits.length > 0) {
            //time based
            throttle = throttleManager.getTimeBased(throttleName);

            timeUnit = timeUnits[0];
            threshold = timeUnit.toMillis(threshold);
        }
        else {
            //count based
            throttle = throttleManager.getCountBased(throttleName);
        }

        return throttle.open(threshold) ? invocation.proceed() : null;
    }
}
