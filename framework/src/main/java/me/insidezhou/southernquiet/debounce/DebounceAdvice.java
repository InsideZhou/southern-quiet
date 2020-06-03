package me.insidezhou.southernquiet.debounce;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

public class DebounceAdvice implements MethodInterceptor {
    private final DebouncerProvider debouncerProvider;

    public DebounceAdvice(DebouncerProvider debouncerProvider) {
        this.debouncerProvider = debouncerProvider;
    }

    @Override
    public Object invoke(MethodInvocation invocation) {
        Method method = invocation.getMethod();

        Debounce annotation = AnnotatedElementUtils.findMergedAnnotation(method, Debounce.class);
        assert annotation != null;

        Debouncer debouncer = debouncerProvider.getDebouncer(invocation, annotation.waitFor(), annotation.maxWaitFor());
        debouncer.bounce();
        return null;
    }
}
