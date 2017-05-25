package com.ai.southernquiet.web.session;

import org.eclipse.jetty.server.session.SessionData;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于转换{@link SessionData}到JSON的中转对象.
 */
public class SessionJSON implements Serializable {
    private String id;
    private String contextPath;
    private String vhost;
    private String lastNode;
    private long expiry;
    private long created;
    private long cookieSet;
    private long accessed;
    private long lastAccessed;
    private long maxInactiveMs;
    private long lastSaved;
    private Map<String, Object> attributes;

    public SessionJSON(SessionData data) {
        setId(data.getId());
        setContextPath(data.getContextPath());
        setVhost(data.getVhost());
        setLastNode(data.getLastNode());
        setExpiry(data.getExpiry());
        setCreated(data.getCreated());
        setCookieSet(data.getCookieSet());
        setAccessed(data.getAccessed());
        setLastAccessed(data.getLastAccessed());
        setMaxInactiveMs(data.getMaxInactiveMs());
        setLastSaved(data.getLastSaved());
        setAttributes(new HashMap<>(data.getAllAttributes()));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getLastNode() {
        return lastNode;
    }

    public void setLastNode(String lastNode) {
        this.lastNode = lastNode;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getCookieSet() {
        return cookieSet;
    }

    public void setCookieSet(long cookieSet) {
        this.cookieSet = cookieSet;
    }

    public long getAccessed() {
        return accessed;
    }

    public void setAccessed(long accessed) {
        this.accessed = accessed;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public long getMaxInactiveMs() {
        return maxInactiveMs;
    }

    public void setMaxInactiveMs(long maxInactiveMs) {
        this.maxInactiveMs = maxInactiveMs;
    }

    public long getLastSaved() {
        return lastSaved;
    }

    public void setLastSaved(long lastSaved) {
        this.lastSaved = lastSaved;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public SessionData toData() {
        SessionData data = new SessionData(getId(), getContextPath(), getVhost(), getCreated(), getAccessed(), getLastAccessed(), getMaxInactiveMs(), getAttributes());
        data.setCookieSet(getCookieSet());
        data.setExpiry(getExpiry());
        data.setLastSaved(getLastSaved());
        data.setLastNode(getLastNode());

        return data;
    }
}
