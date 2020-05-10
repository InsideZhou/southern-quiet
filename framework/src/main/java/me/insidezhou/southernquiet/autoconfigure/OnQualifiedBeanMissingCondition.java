package me.insidezhou.southernquiet.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OnQualifiedBeanMissingCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = Objects.requireNonNull(metadata.getAnnotationAttributes(ConditionalOnQualifiedBeanMissing.class.getName()));
        Class<?>[] beanClasses = (Class<?>[]) attributes.get("classes");
        String qualifier = (String) Objects.requireNonNull(metadata.getAnnotationAttributes(Qualifier.class.getName()))
            .getOrDefault("value", attributes.get("qualifier"));

        if (StringUtils.isEmpty(qualifier)) {
            throw new RuntimeException("使用ConditionalOnQualifiedBeanMissing必须指定qualifier");
        }

        boolean matched = Arrays.stream(beanClasses).anyMatch(beanType -> {
            try {
                BeanFactoryAnnotationUtils.qualifiedBeanOfType(Objects.requireNonNull(context.getBeanFactory()), beanType, qualifier);
                return true;
            }
            catch (BeansException e) {
                return false;
            }
        });

        if (matched) {
            return ConditionOutcome.noMatch("没有找到类型为: " + Arrays.stream(beanClasses).map(Class::getName).collect(Collectors.joining(",")) + "，限定符为" + qualifier + "的bean。");
        }

        return ConditionOutcome.match();
    }
}
