package me.insidezhou.southernquiet.auth;

import me.insidezhou.southernquiet.AbstractBeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static me.insidezhou.southernquiet.FrameworkAutoConfiguration.ConfigRoot_Auth;

@Component
@ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Auth, matchIfMissing = true)
public class AuthBeanPostProcessor extends AbstractBeanPostProcessor<AuthAdvice, AuthPointcut> {
    @Override
    protected AuthAdvice createAdvice() {
        Objects.requireNonNull(beanFactory);
        return new AuthAdvice(beanFactory);
    }

    @Override
    protected AuthPointcut createPointcut() {
        return new AuthPointcut();
    }
}
