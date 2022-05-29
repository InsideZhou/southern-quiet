package me.insidezhou.southernquiet.debounce;

import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static me.insidezhou.southernquiet.FrameworkAutoConfiguration.ConfigRoot_Debounce;

@Component
@ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Debounce, matchIfMissing = true)
public class DebounceBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {
    private DebounceAdvice advice;

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);

        var pointcut = new DebouncePointcut();
        advice = new DebounceAdvice(beanFactory);
        this.advisor = new DefaultPointcutAdvisor(pointcut, advice);
    }

    public DebounceAdvice getAdvice() {
        return advice;
    }
}
