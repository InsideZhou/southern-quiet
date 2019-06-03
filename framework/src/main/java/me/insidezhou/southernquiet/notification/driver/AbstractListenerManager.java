package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.notification.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractListenerManager {
    private final static Logger log = LoggerFactory.getLogger(AbstractListenerManager.class);

    private boolean inited = false;

    @EventListener(ContextRefreshedEvent.class)
    public void initListener(ContextRefreshedEvent event) {
        initListener(event.getApplicationContext());
    }

    public void initListener(ApplicationContext applicationContext) {
        if (inited) return;

        Arrays.stream(applicationContext.getBeanDefinitionNames())
            .map(name -> {
                try {
                    return applicationContext.getBean(name);
                }
                catch (BeansException e) {
                    log.info("查找NotificationListener时，bean未能初始化: {}", name);
                    return null;
                }
            })
            .filter(bean -> null != bean)
            .forEach(bean -> {
                Arrays.stream(ReflectionUtils.getAllDeclaredMethods(bean.getClass()))
                    .forEach(method -> {
                        AnnotationUtils.getRepeatableAnnotations(method, NotificationListener.class)
                            .forEach(listener -> {
                                if (log.isDebugEnabled()) {
                                    log.debug(
                                        "找到NotificationListener：{}(id={}) {}#{}",
                                        listener.notification().getSimpleName(),
                                        listener.name(),
                                        bean.getClass().getSimpleName(),
                                        method.getName()
                                    );
                                }

                                initListener(listener, bean, method);
                            });
                    });
            });

        inited = true;
    }

    protected abstract void initListener(NotificationListener listener, Object bean, Method method);
}
