package me.insidezhou.southernquiet.throttle.annotation;

import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

public class ThrottleBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {
    public ThrottleBeanPostProcessor(ThrottleAdvice advice) {
        this.advisor = new DefaultPointcutAdvisor(AnnotationMatchingPointcut.forMethodAnnotation(Throttle.class), advice);
    }
}
