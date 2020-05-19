package me.insidezhou.southernquiet;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;

public class AnnotationAdvisingBeanPostProcessor extends AbstractAdvisingBeanPostProcessor {
    public AnnotationAdvisingBeanPostProcessor(Advisor advisor) {
        setProxyTargetClass(true); //必须强行指定为true以使用cglib proxy，否则在类型判定时会失败。org.springframework.web.servlet.handler.AbstractHandlerMethodMapping.isHandler

        this.advisor = advisor;
    }
}
