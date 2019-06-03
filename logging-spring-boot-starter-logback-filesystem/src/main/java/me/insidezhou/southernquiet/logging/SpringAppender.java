package me.insidezhou.southernquiet.logging;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * 用来代理到spring bean的的appender。
 */
public class SpringAppender<E> extends UnsynchronizedAppenderBase<E> {
    private ApplicationContext applicationContext;
    private Appender<E> appender;
    private String beanName;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Appender<E> getAppender() {
        return appender;
    }

    public void setAppender(Appender<E> appender) {
        this.appender = appender;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @SuppressWarnings("unchecked")
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;

        if (null != applicationContext) {
            Appender<E> Appender;

            if (StringUtils.hasText(beanName)) {
                Appender = applicationContext.getBean(beanName, Appender.class);
            }
            else {
                Appender = applicationContext.getBean(Appender.class);
            }

            setAppender(Appender);

            if (!Appender.isStarted()) {
                Appender.start();
            }
        }
    }

    @Override
    protected void append(E eventObject) {
        if (null != appender) {
            appender.doAppend(eventObject);
        }
    }
}
