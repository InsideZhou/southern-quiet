package me.insidezhou.southernquiet.throttle;


import me.insidezhou.southernquiet.AbstractBeanPostProcessor;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

import java.util.Objects;

import static me.insidezhou.southernquiet.FrameworkAutoConfiguration.ConfigRoot_Throttle;

@Component
@ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Throttle, matchIfMissing = true)
public class ThrottleBeanPostProcessor extends AbstractBeanPostProcessor<ThrottleAdvice, ThrottlePointcut> implements EmbeddedValueResolverAware {
    protected StringValueResolver stringValueResolver;

    public ThrottleAdvice getAdvice() {
        return advice;
    }

    @Override
    public void setEmbeddedValueResolver(@NotNull StringValueResolver resolver) {
        this.stringValueResolver = resolver;
        setAdvisor();
    }

    @Override
    protected ThrottleAdvice createAdvice() {
        Objects.requireNonNull(beanFactory);
        Objects.requireNonNull(stringValueResolver);
        return new ThrottleAdvice(beanFactory, stringValueResolver);
    }

    @Override
    protected ThrottlePointcut createPointcut() {
        return new ThrottlePointcut();
    }

    protected void setAdvisor() {
        if (null == stringValueResolver) return;
        if (null == beanFactory) return;

        advice = createAdvice();
        pointcut = createPointcut();
        advisor = new DefaultPointcutAdvisor(pointcut, advice);
    }
}
