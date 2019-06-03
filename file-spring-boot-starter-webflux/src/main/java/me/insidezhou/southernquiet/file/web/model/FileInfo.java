package me.insidezhou.southernquiet.file.web.model;

public class FileInfo {
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
        int i = url.indexOf("://");
        if (i >= 0) {
            this.url = url.substring(i + 1);
        }
        else {
            this.url = url;
        }
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
