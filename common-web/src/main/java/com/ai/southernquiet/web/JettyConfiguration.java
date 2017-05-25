package com.ai.southernquiet.web;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

@Component
public class JettyConfiguration extends AbstractConfiguration {
    private SessionDataStore sessionDataStore;

    public JettyConfiguration(SessionDataStore sessionDataStore) {
        this.sessionDataStore = sessionDataStore;
    }

    @Override
    public void configure(WebAppContext context) throws Exception {
        configureErrorHandler(context);
        configureSessionHandler(context);
    }

    private void configureErrorHandler(WebAppContext context) {
        context.setErrorHandler(new ErrorHandler() {
            @Override
            protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {
                writer.write(message);
            }
        });
    }

    private void configureSessionHandler(WebAppContext context) {
        SessionHandler handler = context.getSessionHandler();
        SessionCache sessionCache = new DefaultSessionCache(handler);
        sessionCache.setSessionDataStore(sessionDataStore);
        handler.setSessionCache(sessionCache);
    }
}
