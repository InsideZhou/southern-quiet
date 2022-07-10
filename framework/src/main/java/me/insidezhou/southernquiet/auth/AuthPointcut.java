package me.insidezhou.southernquiet.auth;

import org.jetbrains.annotations.NotNull;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

import java.util.Arrays;

public class AuthPointcut implements Pointcut {
    private final ComposablePointcut pointcut = new ComposablePointcut(AnnotationMatchingPointcut.forMethodAnnotation(Auth.class)).union(AnnotationMatchingPointcut.forClassAnnotation(Auth.class));

    public AuthPointcut() {}

    public AuthPointcut(Pointcut... pointcuts) {
        Arrays.stream(pointcuts).forEach(pointcut::union);
    }

    @NotNull
    @Override
    public ClassFilter getClassFilter() {
        return pointcut.getClassFilter();
    }

    @NotNull
    @Override
    public MethodMatcher getMethodMatcher() {
        return pointcut.getMethodMatcher();
    }
}
