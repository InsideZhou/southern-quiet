package me.insidezhou.southernquiet.file.web.model;

import java.io.Serializable;

public class FileInfo implements Serializable {
    private final static long serialVersionUID = 8826108912694997037L;

    private String id;
    private String url;
    private String contentType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
