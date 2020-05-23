package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.notification.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractNotificationListenerManager implements InitializingBean {
    private final static Logger log = LoggerFactory.getLogger(AbstractNotificationListenerManager.class);

    protected final ApplicationContext applicationContext;

    public AbstractNotificationListenerManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
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
            .filter(Objects::nonNull)
            .forEach(bean -> Arrays.stream(ReflectionUtils.getAllDeclaredMethods(bean.getClass()))
                .forEach(method -> AnnotationUtils.getRepeatableAnnotations(method, NotificationListener.class)
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
                    })
                )
            );
    }

    protected abstract void initListener(NotificationListener listener, Object bean, Method method);
}