package com.ai.southernquiet.web;

import ch.qos.logback.classic.LoggerContext;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.logging.FileAppender;
import com.ai.southernquiet.logging.SpringProxyAppender;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 初始化web应用。
 */
public abstract class CommonWebInit {
    public void onStartup(ServletContext servletContext) throws ServletException {
        setupLogAppender(servletContext);
    }

    @SuppressWarnings("unused")
    protected void setupLogAppender(ServletContext servletContext) {
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
                    else if (SpringProxyAppender.class.isAssignableFrom(cls) && null != applicationContext) {
                        SpringProxyAppender proxyAppender = (SpringProxyAppender) appender;
                        if (null == proxyAppender.getApplicationContext()) {
                            proxyAppender.setApplicationContext(applicationContext);
                        }
                    }
                });
            });
        }
    }

    private ApplicationContext applicationContext;
    private FileSystem fileSystem;

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Autowired(required = false)
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Autowired(required = false)
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
}
