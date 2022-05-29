package me.insidezhou.southernquiet.throttle;


import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

import static me.insidezhou.southernquiet.FrameworkAutoConfiguration.ConfigRoot_Throttle;

@Component
@ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Throttle, matchIfMissing = true)
public class ThrottleBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor implements EmbeddedValueResolverAware {
    private StringValueResolver stringValueResolver;
    private BeanFactory beanFactory;
    private ThrottleAdvice advice;

    public ThrottleAdvice getAdvice() {
        return advice;
    }

    @Override
    public void setEmbeddedValueResolver(@NotNull StringValueResolver resolver) {
        this.stringValueResolver = resolver;
        setAdvisor();
    }

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        super.setBeanFactory(beanFactory);
        setAdvisor();
    }

    private void setAdvisor() {
        if (null == stringValueResolver) return;
        if (null == beanFactory) return;

        var pointcut = new ThrottlePointcut();
        advice = new ThrottleAdvice(beanFactory, stringValueResolver);
        this.advisor = new DefaultPointcutAdvisor(pointcut, advice);
    }
}
