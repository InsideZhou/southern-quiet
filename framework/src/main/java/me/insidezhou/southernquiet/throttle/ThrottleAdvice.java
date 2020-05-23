package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.util.Tuple;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static me.insidezhou.southernquiet.throttle.annotation.Throttle.DefaultThreshold;

public class ThrottleAdvice implements MethodInterceptor, EmbeddedValueResolverAware {
    private final ThrottleManager throttleManager;
    private StringValueResolver embeddedValueResolver;

    private final Map<String, Tuple<String, Boolean, Long>> methodThrottle = new ConcurrentHashMap<>();

    public ThrottleAdvice(ThrottleManager throttleManager) {
        this.throttleManager = throttleManager;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Tuple<String, Boolean, Long> throttleValues = getThrottleValues(invocation);
        Throttle throttle = throttleValues.getSecond() ? throttleManager.getTimeBased(throttleValues.getFirst(), 1) : throttleManager.getCountBased(throttleValues.getFirst());
        long threshold = throttleValues.getThird();

        return throttle.open(threshold) ? invocation.proceed() : null;
    }

    public int advisingCount() {
        return methodThrottle.size();
    }

    private Tuple<String, Boolean, Long> getThrottleValues(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        me.insidezhou.southernquiet.throttle.annotation.Throttle annotation = AnnotatedElementUtils.findMergedAnnotation(method, me.insidezhou.southernquiet.throttle.annotation.Throttle.class);
        assert annotation != null;

        String throttleName = annotation.name();

        Tuple<String, Boolean, Long> throttleValues = methodThrottle.get(throttleName);
        if (null != throttleValues) return throttleValues;

        long threshold = annotation.threshold();
        Optional<TimeUnit> optionalTimeUnit = Arrays.stream(annotation.timeUnit()).findFirst();

        Scheduled scheduledAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Scheduled.class);

        if (DefaultThreshold == threshold && null != scheduledAnnotation) {
            Long thresholdFromSchedule = null;

            if (!StringUtils.isEmpty(scheduledAnnotation.cron())) {
                String cron = embeddedValueResolver.resolveStringValue(scheduledAnnotation.cron());
                CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(Objects.requireNonNull(cron));
                Instant start = cronSequenceGenerator.next(Date.from(Instant.now())).toInstant();
                Instant end = cronSequenceGenerator.next(Date.from(start)).toInstant();

                thresholdFromSchedule = end.toEpochMilli() - start.toEpochMilli();
            }
            else if (scheduledAnnotation.fixedRate() > 0) {
                thresholdFromSchedule = scheduledAnnotation.fixedRate();
            }
            else if (!StringUtils.isEmpty(scheduledAnnotation.fixedRateString())) {
                thresholdFromSchedule = Duration.parse(scheduledAnnotation.fixedRateString()).toMillis();
            }

            if (null != thresholdFromSchedule) {
                optionalTimeUnit = Optional.of(TimeUnit.MILLISECONDS);
                threshold = (long) (thresholdFromSchedule * 0.98); //只要节流的时间达到调度计划时间间隔的0.98倍就算节流成功。
            }
        }

        if (StringUtils.isEmpty(throttleName)) {
            throttleName = method.getDeclaringClass().getName() + "#" + method.getName();
        }

        if (optionalTimeUnit.isPresent()) {
            throttleValues = new Tuple<>(throttleName, true, optionalTimeUnit.get().toMillis(threshold));//time based
        }
        else {
            throttleValues = new Tuple<>(throttleName, false, threshold);//count based
        }

        methodThrottle.put(throttleName, throttleValues);
        return throttleValues;
    }

    @Override
    public void setEmbeddedValueResolver(@NotNull StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }
}
