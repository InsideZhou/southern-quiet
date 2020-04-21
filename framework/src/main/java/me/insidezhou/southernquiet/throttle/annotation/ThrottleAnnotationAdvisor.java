package me.insidezhou.southernquiet.throttle.annotation;

import me.insidezhou.southernquiet.throttle.ThrottleManager;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

public class ThrottleAnnotationAdvisor extends AbstractPointcutAdvisor {
    private AnnotationMatchingPointcut pointcut;
    private Advice advice;

    public ThrottleAnnotationAdvisor(ThrottleManager throttleManager) {
        this.advice = new ThrottleInterceptor(throttleManager);
        this.pointcut = AnnotationMatchingPointcut.forMethodAnnotation(Throttle.class);
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

}
