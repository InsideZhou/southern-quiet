package com.ai.southernquiet.web;

import ch.qos.logback.classic.LoggerContext;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.logging.FileAppender;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 初始化web应用。
 */
public abstract class WebInit implements ServletContextInitializer, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private FileSystem fileSystem;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        fileSystem = applicationContext.getBean(FileSystem.class);

        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (LoggerContext.class.isAssignableFrom(factory.getClass())) {
            setupFileAppender((LoggerContext) factory);
        }
    }

    private void setupFileAppender(LoggerContext loggerContext) {
        if (null == fileSystem) return;

        loggerContext.getLoggerList().forEach(logger -> {
            logger.iteratorForAppenders().forEachRemaining(appender -> {
                if (FileAppender.class.isAssignableFrom(appender.getClass())) {
                    FileAppender fileAppender = (FileAppender) appender;
                    if (null == fileAppender.getFileSystem()) {
                        fileAppender.setFileSystem(fileSystem);
                    }
                }
            });
        });
    }
}
