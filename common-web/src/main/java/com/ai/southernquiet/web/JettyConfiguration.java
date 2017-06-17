package com.ai.southernquiet.web;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

public class JettyConfiguration extends AbstractConfiguration {
    private SessionDataStore sessionDataStore;

    public SessionDataStore getSessionDataStore() {
        return sessionDataStore;
    }

    @Autowired(required = false)
    public void setSessionDataStore(SessionDataStore sessionDataStore) {
        this.sessionDataStore = sessionDataStore;
    }

    @Override
    public void configure(WebAppContext context) throws Exception {
        configureErrorHandler(context);
        configureSessionHandler(context);
    }

    protected void configureErrorHandler(WebAppContext context) {
        context.setErrorHandler(new ErrorHandler() {
            @Override
            protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {
                writer.write(message);
            }
        });
    }

    protected void configureSessionHandler(WebAppContext context) {
        if (null == sessionDataStore) return;

        SessionHandler handler = context.getSessionHandler();
        SessionCache sessionCache = new DefaultSessionCache(handler);
        sessionCache.setSessionDataStore(sessionDataStore);
        handler.setSessionCache(sessionCache);
    }
}
