package com.ai.southernquiet.logging;

import ch.qos.logback.classic.LoggerContext;
import com.ai.southernquiet.filesystem.FileSystem;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@SuppressWarnings("CodeBlock2Expr")
@Component
public class AppenderInitializer {
    @EventListener(ContextRefreshedEvent.class)
    public void init(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        FileSystem fs = null;
        try {
            fs = context.getBean(FileSystem.class);
        }
        catch (BeansException e) {
            e.printStackTrace();
        }

        final FileSystem fileSystem = fs;

        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (LoggerContext.class.isAssignableFrom(factory.getClass())) {
            LoggerContext loggerContext = (LoggerContext) factory;

            loggerContext.getLoggerList().forEach(logger -> {
                logger.iteratorForAppenders().forEachRemaining(appender -> {
                    Class<?> cls = appender.getClass();

                    if (FileAppender.class.isAssignableFrom(cls) && null != fileSystem) {
                        FileAppender fileAppender = (FileAppender) appender;
                        if (null == fileAppender.getFileSystem()) {
                            fileAppender.setFileSystem(fileSystem);
                        }
                    }
                    else if (SpringAppender.class.isAssignableFrom(cls)) {
                        SpringAppender proxyAppender = (SpringAppender) appender;
                        if (null == proxyAppender.getApplicationContext()) {
                            proxyAppender.setApplicationContext(context);
                        }
                    }
                });
            });
        }
    }
}
