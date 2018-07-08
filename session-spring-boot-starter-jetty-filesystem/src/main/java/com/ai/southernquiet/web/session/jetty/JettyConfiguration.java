package com.ai.southernquiet.web.session.jetty;

import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({"WeakerAccess", "unused"})
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
        configureSessionHandler(context);
    }

    protected void configureSessionHandler(WebAppContext context) {
        if (null == sessionDataStore) return;

        SessionHandler handler = context.getSessionHandler();
        SessionCache sessionCache = new DefaultSessionCache(handler);
        sessionCache.setSessionDataStore(sessionDataStore);
        handler.setSessionCache(sessionCache);
    }
}
