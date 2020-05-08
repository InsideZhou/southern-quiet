package me.insidezhou.southernquiet.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;
import java.util.Objects;

public class OnQualifiedBeanCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = Objects.requireNonNull(metadata.getAnnotationAttributes(ConditionalOnQualifiedBean.class.getName()));
        Class<?> beanType = (Class<?>) attributes.get("type");
        String qualifier = (String) attributes.get("qualifier");

        try {
            BeanFactoryAnnotationUtils.qualifiedBeanOfType(Objects.requireNonNull(context.getBeanFactory()), beanType, qualifier);
        }
        catch (BeansException e) {
            return ConditionOutcome.noMatch("没有找到类型为: " + beanType.getName() + "，修饰符为" + qualifier + "的bean。");
        }

        return ConditionOutcome.match();
    }
}
