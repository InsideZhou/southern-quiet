package com.ai.southernquiet.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * common-web模块依赖的外部配置。
 */
@Component
@ConfigurationProperties("web")
public class CommonWebProperties {
    private Session session = new Session();

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public static class Session {
        private FileSystem fileSystem = new FileSystem();
        private RememberMe rememberMe = new RememberMe();
        private String user;

        public FileSystem getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(FileSystem fileSystem) {
            this.fileSystem = fileSystem;
        }

        public RememberMe getRememberMe() {
            return rememberMe;
        }

        public void setRememberMe(RememberMe rememberMe) {
            this.rememberMe = rememberMe;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public static class FileSystem {
            private String workingRoot;

            public String getWorkingRoot() {
                return workingRoot;
            }

            public void setWorkingRoot(String workingRoot) {
                this.workingRoot = workingRoot;
            }
        }

        public static class RememberMe {
            private String cookie;
            private Integer timeout;

            public String getCookie() {
                return cookie;
            }

            public void setCookie(String cookie) {
                this.cookie = cookie;
            }

            public Integer getTimeout() {
                return timeout;
            }

            public void setTimeout(Integer timeout) {
                this.timeout = timeout;
            }
        }
    }
}
