package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.util.Tuple;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.Expression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static me.insidezhou.southernquiet.throttle.annotation.Throttle.DefaultThreshold;

public class ThrottleAdvice implements MethodInterceptor {
    private final BeanFactory beanFactory;
    private final StringValueResolver stringValueResolver;
    private final Map<String, Tuple<String, Boolean, Long>> methodThrottle = new ConcurrentHashMap<>();

    private ThrottleManager throttleManager;
    private NameEvaluator nameEvaluator;
    private boolean initialized = false;

    public ThrottleAdvice(BeanFactory beanFactory, StringValueResolver stringValueResolver) {
        this.beanFactory = beanFactory;
        this.stringValueResolver = stringValueResolver;
    }

    protected void initOnceBeforeWork() {
        throttleManager = beanFactory.getBean(ThrottleManager.class);
        nameEvaluator = new NameEvaluator(beanFactory);
    }

    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        if (!initialized) {
            initOnceBeforeWork();
            initialized = true;
        }

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
            //noinspection ConstantConditions
            throttleName = nameEvaluator.evalName(
                annotation.name(), invocation, annotation, new AnnotatedElementKey(method, invocation.getThis().getClass())
            );
        }
        else if (!StringUtils.hasText(annotation.name())) {
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

            if (StringUtils.hasText(scheduledAnnotation.cron())) {
                String cron = stringValueResolver.resolveStringValue(scheduledAnnotation.cron());
                var cronExp = CronExpression.parse(Objects.requireNonNull(cron));
                var start = Objects.requireNonNull(cronExp.next(LocalDateTime.now()));
                var end = Objects.requireNonNull(cronExp.next(start));

                thresholdFromSchedule = Duration.between(start, end).toMillis();
            }
            else if (scheduledAnnotation.fixedRate() > 0) {
                thresholdFromSchedule = scheduledAnnotation.fixedRate();
            }
            else if (StringUtils.hasText(scheduledAnnotation.fixedRateString())) {
                thresholdFromSchedule = Duration.parse(scheduledAnnotation.fixedRateString()).toMillis();
            }

            if (null != thresholdFromSchedule) {
                optionalTimeUnit = Optional.of(TimeUnit.MILLISECONDS);
                threshold = (long) (thresholdFromSchedule * 0.98); //只要节流的时间达到调度计划时间间隔的0.98倍就算节流成功，暂时妥协一下，这个值需要考虑是否值得配置。
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
        //noinspection ConstantConditions
        return invocation.getThis().getClass().getName() + "#" + invocation.getMethod().getName();
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
