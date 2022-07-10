package me.insidezhou.southernquiet.debounce;

import me.insidezhou.southernquiet.AbstractBeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static me.insidezhou.southernquiet.FrameworkAutoConfiguration.ConfigRoot_Debounce;

@Component
@ConditionalOnProperty(value = "enable", prefix = ConfigRoot_Debounce, matchIfMissing = true)
public class DebounceBeanPostProcessor extends AbstractBeanPostProcessor<DebounceAdvice, DebouncePointcut> {
    @Override
    protected DebounceAdvice createAdvice() {
        Objects.requireNonNull(beanFactory);
        return new DebounceAdvice(beanFactory);
    }

    @Override
    protected DebouncePointcut createPointcut() {
        return new DebouncePointcut();
    }
}
