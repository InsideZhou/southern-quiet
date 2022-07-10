package test.auth.component;

import me.insidezhou.southernquiet.auth.AuthBeanPostProcessor;
import me.insidezhou.southernquiet.auth.AuthPointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import test.auth.AuthTest;

@Component
@Primary
public class AuthTestBeanPostProcessor extends AuthBeanPostProcessor {
    @Override
    public AuthPointcut createPointcut() {
        return new AuthPointcut(
            AnnotationMatchingPointcut.forClassAnnotation(AuthTest.BusinessAuth.class),
            AnnotationMatchingPointcut.forMethodAnnotation(AuthTest.BusinessAuth.class)
        );
    }
}
