package me.insidezhou.southernquiet.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import org.slf4j.Marker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MongoLoggingEvent {
    private String level;
    private Instant instant;
    private String logger;
    private String message;

    private String thread;
    private Caller caller;
    private Throwable throwable;

    private List<String> marker;
    private Map<String, String> properties;

    public MongoLoggingEvent(ILoggingEvent event) {
        this.level = event.getLevel().toString();
        this.instant = Instant.ofEpochMilli(event.getTimeStamp());
        this.logger = event.getLoggerName();
        this.message = event.getFormattedMessage();
        this.thread = event.getThreadName();

        if (event.hasCallerData()) {
            Arrays.stream(event.getCallerData())
                .findFirst()
                .ifPresent(stackTraceElement -> this.caller = new Caller(stackTraceElement));
        }

        IThrowableProxy proxy = event.getThrowableProxy();
        if (null != proxy) {
            this.throwable = new Throwable(proxy);
        }

        Marker marker = event.getMarker();
        List<String> markerNames = new ArrayList<>();
        if (null != marker) {
            markerNames.add(marker.getName());
            marker.iterator().forEachRemaining(m -> markerNames.add(m.getName()));
        }

        this.marker = markerNames;

        this.properties = event.getMDCPropertyMap();
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public Caller getCaller() {
        return caller;
    }

    public void setCaller(Caller caller) {
        this.caller = caller;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public List<String> getMarker() {
        return marker;
    }

    public void setMarker(List<String> marker) {
        this.marker = marker;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public static class Throwable {
        private String className;
        private String message;
        private Throwable cause;
        private List<Caller> stacktrace;

        public Throwable(IThrowableProxy proxy) {
            className = proxy.getClassName();
            message = proxy.getMessage();

            IThrowableProxy cause = proxy.getCause();
            if (null != cause) {
                this.cause = new Throwable(cause);
            }

            Stream<StackTraceElementProxy> stream = Arrays.stream(proxy.getStackTraceElementProxyArray());
            this.stacktrace = stream
                .map(steProxy -> new Caller(steProxy.getStackTraceElement()))
                .collect(Collectors.toList());
        }

        public List<Caller> getStacktrace() {
            return stacktrace;
        }

        public void setStacktrace(List<Caller> stacktrace) {
            this.stacktrace = stacktrace;
        }

        public Throwable getCause() {
            return cause;
        }

        public void setCause(Throwable cause) {
            this.cause = cause;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class Caller {
        private String className;
        private String method;
        private String file;
        private int line;

        public Caller(StackTraceElement stackTraceElement) {
            className = stackTraceElement.getClassName();
            method = stackTraceElement.getMethodName();
            file = stackTraceElement.getFileName();
            line = stackTraceElement.getLineNumber();
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }
    }
}
