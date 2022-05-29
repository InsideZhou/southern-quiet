package me.insidezhou.southernquiet.debounce;

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
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DebounceAdvice implements MethodInterceptor {
    private final BeanFactory beanFactory;
    private DebouncerProvider debouncerProvider;
    private NameEvaluator nameEvaluator;
    private boolean initialized = false;

    public DebounceAdvice(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    protected void initOnceBeforeWork() {
        debouncerProvider = beanFactory.getBean(DebouncerProvider.class);
        nameEvaluator = new NameEvaluator(beanFactory);
    }

    @Override
    public Object invoke(@NotNull MethodInvocation invocation) {
        if (!initialized) {
            initOnceBeforeWork();
            initialized = true;
        }

        Method method = invocation.getMethod();

        Debounce annotation = AnnotatedElementUtils.findMergedAnnotation(method, Debounce.class);
        assert annotation != null;

        String debouncerName;
        if (annotation.isSpELName()) {
            debouncerName = nameEvaluator.evalName(
                annotation.name(), invocation, annotation, new AnnotatedElementKey(method, Objects.requireNonNull(invocation.getThis()).getClass())
            );
        }
        else if (!StringUtils.hasText(annotation.name())) {
            debouncerName = getDefaultDebouncerName(invocation, annotation);
        }
        else {
            debouncerName = annotation.name();
        }

        Debouncer debouncer = debouncerProvider.getDebouncer(invocation, annotation.waitFor(), annotation.maxWaitFor(), debouncerName, annotation.executionTimeout());
        debouncer.bounce();
        return null;
    }

    private static String getDefaultDebouncerName(MethodInvocation invocation, Debounce annotation) {
        return Objects.requireNonNull(invocation.getThis()).getClass().getName() + "#" + invocation.getMethod().getName() + "_" + annotation.waitFor() + "_" + annotation.maxWaitFor();
    }

    public static class NameEvaluator extends CachedExpressionEvaluator {
        private final Map<ExpressionKey, Expression> expressionCache = new ConcurrentHashMap<>();
        private final BeanFactory beanFactory;

        public NameEvaluator(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        public String evalName(String expression, MethodInvocation invocation, Debounce annotation, AnnotatedElementKey methodKey) {
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
        private Debounce annotation;

        private String defaultName;

        public EvaluationRoot(MethodInvocation invocation, Debounce annotation) {
            this.instance = invocation.getThis();
            this.annotation = annotation;

            this.defaultName = getDefaultDebouncerName(invocation, annotation);
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

        public Debounce getAnnotation() {
            return annotation;
        }

        public void setAnnotation(Debounce annotation) {
            this.annotation = annotation;
        }
    }
}
