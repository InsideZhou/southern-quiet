package com.ai.southernquiet.web;

import ch.qos.logback.classic.LoggerContext;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.logging.FileAppender;
import com.ai.southernquiet.web.auth.AuthService;
import com.ai.southernquiet.web.auth.RequestWrapperFilter;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.EnumSet;

/**
 * 初始化web应用。
 */
public abstract class CommonWebInit {
    private Logger logger = LoggerFactory.getLogger(CommonWebInit.class);

    private ApplicationContext applicationContext;
    private FileSystem fileSystem;
    private CommonWebProperties webProperties;

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Autowired
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public CommonWebProperties getWebProperties() {
        return webProperties;
    }

    @Autowired
    public void setWebProperties(CommonWebProperties webProperties) {
        this.webProperties = webProperties;
    }

    public void onStartup(ServletContext servletContext) throws ServletException {
        setupLogAppender(servletContext);
        setupRequestWrapperFilter(servletContext);
    }

    @SuppressWarnings("unused")
    protected void setupLogAppender(ServletContext servletContext) {
        if (null == fileSystem) return;

        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (LoggerContext.class.isAssignableFrom(factory.getClass())) {
            LoggerContext loggerContext = (LoggerContext) factory;

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

    protected void setupRequestWrapperFilter(ServletContext servletContext) {
        try {
            AuthService authService = applicationContext.getBean(AuthService.class);
            RequestWrapperFilter filter = new RequestWrapperFilter();
            filter.setAuthService(authService);
            filter.setWebProperties(webProperties);

            servletContext.addFilter("requestWrapper", filter).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
        }
        catch (BeansException e) {
            logger.warn("无法获取AuthService，身份验证关闭。");
        }
    }
}
