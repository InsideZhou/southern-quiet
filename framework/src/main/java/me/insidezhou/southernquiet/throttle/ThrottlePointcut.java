package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.annotation.Throttle;
import me.insidezhou.southernquiet.throttle.annotation.ThrottledSchedule;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

public class ThrottlePointcut implements Pointcut {
    private final ComposablePointcut pointcut = new ComposablePointcut(AnnotationMatchingPointcut.forMethodAnnotation(Throttle.class))
        .union(AnnotationMatchingPointcut.forMethodAnnotation(ThrottledSchedule.class));

    public ThrottlePointcut() { }

    public ThrottlePointcut(Pointcut pointcut) {
        this.pointcut.union(pointcut);
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
