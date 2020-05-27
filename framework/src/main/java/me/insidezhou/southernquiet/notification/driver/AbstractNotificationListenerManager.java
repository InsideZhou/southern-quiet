package me.insidezhou.southernquiet.notification.driver;

import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.notification.NotificationListener;
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
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AbstractNotificationListenerManager.class);

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
                    log.message("查找NotificationListener时，bean未能初始化").context("name", name).info();
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(bean -> Arrays.stream(ReflectionUtils.getAllDeclaredMethods(bean.getClass()))
                .forEach(method -> AnnotationUtils.getRepeatableAnnotations(method, NotificationListener.class)
                    .forEach(listener -> {
                        log.message("找到NotificationListener：")
                            .context(context -> {
                                context.put("notification", listener.notification().getSimpleName());
                                context.put("name", listener.name());
                                context.put("listenerName", bean.getClass().getSimpleName());
                                context.put("method", method.getName());
                            })
                            .debug();

                        initListener(listener, bean, method);
                    })
                )
            );
    }

    protected abstract void initListener(NotificationListener listener, Object bean, Method method);
}
