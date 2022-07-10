package me.insidezhou.southernquiet;

import org.aopalliance.aop.Advice;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;

public abstract class AbstractBeanPostProcessor<TAdvice extends Advice, TPointcut extends Pointcut> extends AbstractBeanFactoryAwareAdvisingPostProcessor {
    protected BeanFactory beanFactory;
    protected TAdvice advice;
    protected TPointcut pointcut;

    protected abstract TAdvice createAdvice();

    protected abstract TPointcut createPointcut();

    protected void setAdvisor() {
        advice = createAdvice();
        pointcut = createPointcut();
        advisor = new DefaultPointcutAdvisor(pointcut, advice);
    }

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        super.setBeanFactory(beanFactory);
        setAdvisor();
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public TAdvice getAdvice() {
        return advice;
    }

    public TPointcut getPointcut() {
        return pointcut;
    }
}
