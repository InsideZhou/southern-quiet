package com.ai.southernquiet.web;

import ch.qos.logback.classic.LoggerContext;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.logging.FileAppender;
import com.ai.southernquiet.web.auth.AuthService;
import com.ai.southernquiet.web.auth.Request;
import com.ai.southernquiet.web.auth.RequestWrapperFilter;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.EnumSet;

/**
 * 初始化web应用。
 */
public abstract class WebInit implements ServletContextInitializer, ApplicationContextAware {
    private Logger logger = LoggerFactory.getLogger(WebInit.class);

    private ApplicationContext applicationContext;
    private FileSystem fileSystem;
    private CommonWebProperties webProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        fileSystem = applicationContext.getBean(FileSystem.class);
        webProperties = applicationContext.getBean(CommonWebProperties.class);

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
            servletContext.addFilter("requestWrapper", filter).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

            Request.REMEMBER_ME_TIMEOUT = webProperties.getSession().getRememberMe().getTimeout();
            Request.KEY_REMEMBER_ME_COOKIE = webProperties.getSession().getRememberMe().getCookie();
            Request.KEY_USER = webProperties.getSession().getUser();
        }
        catch (BeansException e) {
            logger.warn("无法获取AuthService，身份验证关闭。");
        }
    }
}
