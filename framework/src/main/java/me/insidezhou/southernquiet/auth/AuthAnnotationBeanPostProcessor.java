package me.insidezhou.southernquiet.auth;

import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

public class AuthAnnotationBeanPostProcessor extends AbstractAdvisingBeanPostProcessor {
    public AuthAnnotationBeanPostProcessor(AuthAdvice authAdvice) {
        super();

        this.advisor = new DefaultPointcutAdvisor(
            new ComposablePointcut(AnnotationMatchingPointcut.forMethodAnnotation(Auth.class))
                .union(AnnotationMatchingPointcut.forClassAnnotation(Auth.class)),
            authAdvice
        );
    }
}
