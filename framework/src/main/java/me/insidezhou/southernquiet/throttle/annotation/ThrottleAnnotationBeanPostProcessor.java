package me.insidezhou.southernquiet.throttle.annotation;

import me.insidezhou.southernquiet.throttle.ThrottleManager;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanFactory;

public class ThrottleAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

    private ThrottleManager throttleManager;

    public ThrottleAnnotationBeanPostProcessor(ThrottleManager throttleManager) {
        this.throttleManager = throttleManager;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);

        this.advisor = new ThrottleAnnotationAdvisor(throttleManager);
    }
}
