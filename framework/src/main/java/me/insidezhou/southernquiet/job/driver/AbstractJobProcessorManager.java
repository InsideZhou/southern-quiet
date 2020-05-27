package me.insidezhou.southernquiet.job.driver;

import me.insidezhou.southernquiet.job.JobProcessor;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractJobProcessorManager implements InitializingBean {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AbstractJobProcessorManager.class);

    protected final ApplicationContext applicationContext;

    public AbstractJobProcessorManager(ApplicationContext applicationContext) {
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
                    log.message("查找JobProcessor时，bean未能初始化").context("name", name).info();
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .forEach(bean -> Arrays.stream(ReflectionUtils.getAllDeclaredMethods(bean.getClass()))
                .forEach(method -> AnnotationUtils.getRepeatableAnnotations(method, JobProcessor.class)
                    .forEach(listener -> {
                        log.message("找到JobProcessor：")
                            .context(context -> {
                                context.put("notification", listener.job().getSimpleName());
                                context.put("name", listener.name());
                                context.put("listenerName", bean.getClass().getSimpleName());
                                context.put("method", method.getName());
                            })
                            .debug();

                        initProcessor(listener, bean, method);
                    })
                )
            );
    }

    protected abstract void initProcessor(JobProcessor listener, Object bean, Method method);
}
