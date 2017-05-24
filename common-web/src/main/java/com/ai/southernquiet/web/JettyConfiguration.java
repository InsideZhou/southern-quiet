package com.ai.southernquiet.web;

import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.stereotype.Component;

@Component
public class JettyConfiguration extends AbstractConfiguration {
    private SessionDataStore sessionDataStore;

    public JettyConfiguration(SessionDataStore sessionDataStore) {
        this.sessionDataStore = sessionDataStore;
    }

    @Override
    public void configure(WebAppContext context) throws Exception {
        SessionHandler handler = context.getSessionHandler();
        SessionCache sessionCache = new DefaultSessionCache(handler);
        sessionCache.setSessionDataStore(sessionDataStore);
        handler.setSessionCache(sessionCache);

        super.configure(context);
    }
}
