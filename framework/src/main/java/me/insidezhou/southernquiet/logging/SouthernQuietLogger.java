package me.insidezhou.southernquiet.logging;

import me.insidezhou.southernquiet.util.Pair;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SouthernQuietLogger {
    private final ThreadLocal<LogContext> logContextThreadLocal = ThreadLocal.withInitial(LogContext::new);

    private Logger logger;
    private SouthernQuietLogFormatter formatter = new SouthernQuietLogFormatter();

    public SouthernQuietLogger(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public SouthernQuietLogFormatter getFormatter() {
        return formatter;
    }

    public void setFormatter(SouthernQuietLogFormatter formatter) {
        this.formatter = formatter;
    }

    public SouthernQuietLogger exception(Throwable throwable) {
        LogContext logContext = logContextThreadLocal.get();
        logContext.throwable = throwable;

        return this;
    }

    public SouthernQuietLogger message(String msg) {
        LogContext logContext = logContextThreadLocal.get();
        logContext.message = msg;

        return this;
    }

    public SouthernQuietLogger context(String key, Supplier<Object> func) {
        LogContext logContext = logContextThreadLocal.get();
        logContext.context.put(key, func);

        return this;
    }

    public SouthernQuietLogger context(Consumer<Map<String, Object>> consumer) {
        LogContext logContext = logContextThreadLocal.get();
        consumer.accept(logContext.context);

        return this;
    }

    public SouthernQuietLogger context(String key, Object value) {
        LogContext logContext = logContextThreadLocal.get();
        logContext.context.put(key, value);

        return this;
    }

    @SuppressWarnings("unused")
    public void trace() {
        LogContext logContext = logContextThreadLocal.get();

        if (!logger.isTraceEnabled() || null == logger) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.trace(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void debug() {
        LogContext logContext = logContextThreadLocal.get();

        if (!logger.isDebugEnabled() || null == logger) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.debug(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void info() {
        LogContext logContext = logContextThreadLocal.get();

        if (!logger.isInfoEnabled() || null == logger) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.info(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void warn() {
        LogContext logContext = logContextThreadLocal.get();

        if (!logger.isWarnEnabled() || null == logger) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.warn(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    @SuppressWarnings("unused")
    public void error() {
        LogContext logContext = logContextThreadLocal.get();

        if (!logger.isErrorEnabled() || null == logger) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.error(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public static class LogContext implements Serializable {
        private final static long serialVersionUID = 8228778036883035515L;

        private String message;
        private Throwable throwable;
        private Map<String, Object> context = new LinkedHashMap<>();

        public void clear() {
            message = null;
            throwable = null;
            context.clear();
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }
}
