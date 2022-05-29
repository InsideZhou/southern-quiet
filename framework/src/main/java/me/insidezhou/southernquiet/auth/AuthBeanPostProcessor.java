package me.insidezhou.southernquiet.auth;

import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static me.insidezhou.southernquiet.FrameworkAutoConfiguration.ConfigRoot_Auth;

@Component
@ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Auth, matchIfMissing = true)
public class AuthBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {
    private AuthAdvice authAdvice;

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);

        var pointcut = new AuthPointcut();
        this.authAdvice = new AuthAdvice(beanFactory);
        this.advisor = new DefaultPointcutAdvisor(pointcut, authAdvice);
    }

    public AuthAdvice getAuthAdvice() {
        return authAdvice;
    }
}
