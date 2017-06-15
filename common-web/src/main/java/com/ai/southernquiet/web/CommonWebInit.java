package com.ai.southernquiet.web;

import ch.qos.logback.classic.LoggerContext;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.logging.FileAppender;
import com.ai.southernquiet.logging.SpringProxyAppender;
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

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private FileSystem fileSystem;
    @Autowired
    private CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties;
    @Autowired
    private CommonWebAutoConfiguration.WebProperties webProperties;

    public CommonWebAutoConfiguration.WebProperties getWebProperties() {
        return webProperties;
    }

    public void setWebProperties(CommonWebAutoConfiguration.WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public CommonWebAutoConfiguration.SessionRememberMeProperties getRememberMeProperties() {
        return rememberMeProperties;
    }

    public void setRememberMeProperties(CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties) {
        this.rememberMeProperties = rememberMeProperties;
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
                    Class<?> cls = appender.getClass();

                    if (FileAppender.class.isAssignableFrom(cls)) {
                        FileAppender fileAppender = (FileAppender) appender;
                        if (null == fileAppender.getFileSystem()) {
                            fileAppender.setFileSystem(fileSystem);
                        }
                    }
                    else if (SpringProxyAppender.class.isAssignableFrom(cls)) {
                        SpringProxyAppender proxyAppender = (SpringProxyAppender) appender;
                        if (null == proxyAppender.getApplicationContext()) {
                            proxyAppender.setApplicationContext(applicationContext);
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
            filter.setRememberMeProperties(rememberMeProperties);
            filter.setWebProperties(webProperties);

            servletContext.addFilter("requestWrapper", filter).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
        }
        catch (BeansException e) {
            logger.warn("无法获取AuthService，身份验证关闭。");
        }
    }
}
