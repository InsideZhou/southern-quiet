package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.annotation.ThrottledSchedule;
import me.insidezhou.southernquiet.throttle.annotation.Throttle;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

public class ThrottleBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {
    public ThrottleBeanPostProcessor(ThrottleAdvice advice) {
        this.advisor = new DefaultPointcutAdvisor(
            new ComposablePointcut(AnnotationMatchingPointcut.forMethodAnnotation(Throttle.class))
                .union(AnnotationMatchingPointcut.forMethodAnnotation(ThrottledSchedule.class)),
            advice);
    }
}
