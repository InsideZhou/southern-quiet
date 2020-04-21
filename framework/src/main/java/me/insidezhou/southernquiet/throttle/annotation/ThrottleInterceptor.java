package me.insidezhou.southernquiet.throttle.annotation;

import me.insidezhou.southernquiet.throttle.ThrottleManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class ThrottleInterceptor implements MethodInterceptor {

    private final ThrottleManager throttleManager;

    ThrottleInterceptor(ThrottleManager throttleManager) {
        this.throttleManager = throttleManager;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Throttle annotation = invocation.getMethod().getAnnotation(Throttle.class);

        String throttleName = annotation.throttleName();
        long threshold = annotation.threshold();
        TimeUnit[] timeUnits = annotation.timeUnit();

        if (StringUtils.isEmpty(throttleName)) {
            Method method = invocation.getMethod();
            throttleName = method.getDeclaringClass().getName() + "#" + method.getName();
        }

        me.insidezhou.southernquiet.throttle.Throttle throttle;
        if (timeUnits.length > 0) {
            //time based
            throttle = throttleManager.getTimeBased(throttleName);

            TimeUnit timeUnit = timeUnits[0];
            threshold = timeUnit.toMillis(threshold);
        }else{
            //count based
            throttle = throttleManager.getCountBased(throttleName);
        }

        boolean open = throttle.open(threshold);
        if (open) {
            return invocation.proceed();
        }
        return null;
    }
}
