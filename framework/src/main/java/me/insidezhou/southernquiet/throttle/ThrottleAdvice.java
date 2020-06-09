package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.util.Tuple;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.Expression;
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
    private final NameEvaluator nameEvaluator;

    private final Map<String, Tuple<String, Boolean, Long>> methodThrottle = new ConcurrentHashMap<>();

    public ThrottleAdvice(ThrottleManager throttleManager, BeanFactory beanFactory) {
        this.throttleManager = throttleManager;
        this.nameEvaluator = new NameEvaluator(beanFactory);
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

        String throttleName;
        if (annotation.isSpELName()) {
            throttleName = nameEvaluator.evalName(
                annotation.name(), invocation, annotation, new AnnotatedElementKey(method, invocation.getThis().getClass())
            );
        }
        else if (StringUtils.isEmpty(annotation.name())) {
            throttleName = getDefaultThrottleName(invocation);
        }
        else {
            throttleName = annotation.name();
        }

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

        if (optionalTimeUnit.isPresent()) {
            throttleValues = new Tuple<>(throttleName, true, optionalTimeUnit.get().toMillis(threshold));//time based
        }
        else {
            throttleValues = new Tuple<>(throttleName, false, threshold);//count based
        }

        methodThrottle.put(throttleName, throttleValues);
        return throttleValues;
    }

    private static String getDefaultThrottleName(MethodInvocation invocation) {
        return invocation.getThis().getClass().getName() + "#" + invocation.getMethod().getName();
    }

    @Override
    public void setEmbeddedValueResolver(@NotNull StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    public static class NameEvaluator extends CachedExpressionEvaluator {
        private final Map<ExpressionKey, Expression> expressionCache = new ConcurrentHashMap<>();
        private final BeanFactory beanFactory;

        public NameEvaluator(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        public String evalName(String expression, MethodInvocation invocation, me.insidezhou.southernquiet.throttle.annotation.Throttle annotation, AnnotatedElementKey methodKey) {
            MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
                new EvaluationRoot(invocation, annotation),
                invocation.getMethod(),
                invocation.getArguments(),
                getParameterNameDiscoverer()
            );
            evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));

            return getExpression(this.expressionCache, methodKey, expression).getValue(evaluationContext, String.class);
        }
    }

    public static class EvaluationRoot {
        private Object instance;
        private me.insidezhou.southernquiet.throttle.annotation.Throttle annotation;

        private String defaultName;

        public EvaluationRoot(MethodInvocation invocation, me.insidezhou.southernquiet.throttle.annotation.Throttle annotation) {
            this.instance = invocation.getThis();
            this.annotation = annotation;

            this.defaultName = getDefaultThrottleName(invocation);
        }

        public String getDefaultName() {
            return defaultName;
        }

        public void setDefaultName(String defaultName) {
            this.defaultName = defaultName;
        }

        public Object getInstance() {
            return instance;
        }

        public void setInstance(Object instance) {
            this.instance = instance;
        }

        public me.insidezhou.southernquiet.throttle.annotation.Throttle getAnnotation() {
            return annotation;
        }

        public void setAnnotation(me.insidezhou.southernquiet.throttle.annotation.Throttle annotation) {
            this.annotation = annotation;
        }
    }
}
