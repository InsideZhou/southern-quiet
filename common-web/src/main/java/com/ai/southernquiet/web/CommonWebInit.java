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
    private static Logger logger = LoggerFactory.getLogger(CommonWebInit.class);

    public void onStartup(ServletContext servletContext) throws ServletException {
        setupLogAppender(servletContext);
        setupRequestWrapperFilter(servletContext);
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

    protected void setupRequestWrapperFilter(ServletContext servletContext) {
        if (null == authService) {
            logger.warn("无法获取AuthService，身份验证关闭。");
            return;
        }

        RequestWrapperFilter filter = new RequestWrapperFilter();
        filter.setAuthService(authService);
        filter.setRememberMeProperties(rememberMeProperties);
        filter.setWebProperties(webProperties);

        servletContext.addFilter("requestWrapper", filter).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    }

    private ApplicationContext applicationContext;
    private FileSystem fileSystem;
    private CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties;
    private CommonWebAutoConfiguration.WebProperties webProperties;
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    @Autowired(required = false)
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

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

    public CommonWebAutoConfiguration.SessionRememberMeProperties getRememberMeProperties() {
        return rememberMeProperties;
    }

    @Autowired
    public void setRememberMeProperties(CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties) {
        this.rememberMeProperties = rememberMeProperties;
    }

    public CommonWebAutoConfiguration.WebProperties getWebProperties() {
        return webProperties;
    }

    @Autowired
    public void setWebProperties(CommonWebAutoConfiguration.WebProperties webProperties) {
        this.webProperties = webProperties;
    }
}
