package me.insidezhou.southernquiet.logging;

import me.insidezhou.southernquiet.util.Pair;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class SouthernQuietLogger {
    private final ThreadLocal<LogContext> logContextThreadLocal = ThreadLocal.withInitial(LogContext::new);

    private final Logger logger;
    private SouthernQuietLogFormatter formatter;

    public SouthernQuietLogger(Logger logger, SouthernQuietLogFormatter formatter) {
        this.logger = logger;
        this.formatter = formatter;
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

    public void trace() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isTraceEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.trace(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void traceAsync() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isTraceEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        LogContext asyncContext = logContext.clone();
        logContext.clear();
        CompletableFuture.runAsync(() -> {
            Pair<String, List<?>> pair = formatter.formatLogContext(asyncContext);
            logger.trace(pair.getFirst(), pair.getSecond().toArray());
        });
    }

    public void debug() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isDebugEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.debug(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void debugAsync() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isDebugEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        LogContext asyncContext = logContext.clone();
        logContext.clear();
        CompletableFuture.runAsync(() -> {
            Pair<String, List<?>> pair = formatter.formatLogContext(asyncContext);
            logger.debug(pair.getFirst(), pair.getSecond().toArray());
        });
    }

    public void info() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isInfoEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.info(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void infoAsync() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isInfoEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        LogContext asyncContext = logContext.clone();
        logContext.clear();
        CompletableFuture.runAsync(() -> {
            Pair<String, List<?>> pair = formatter.formatLogContext(asyncContext);
            logger.info(pair.getFirst(), pair.getSecond().toArray());
        });
    }

    public void warn() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isWarnEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.warn(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void warnAsync() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isWarnEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        LogContext asyncContext = logContext.clone();
        logContext.clear();
        CompletableFuture.runAsync(() -> {
            Pair<String, List<?>> pair = formatter.formatLogContext(asyncContext);
            logger.warn(pair.getFirst(), pair.getSecond().toArray());
        });
    }

    public void error() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isErrorEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        Pair<String, List<?>> pair = formatter.formatLogContext(logContext);
        logger.error(pair.getFirst(), pair.getSecond().toArray());
        logContext.clear();
    }

    public void errorAsync() {
        LogContext logContext = logContextThreadLocal.get();
        SouthernQuietLogFormatter formatter = this.formatter;

        if (!logger.isErrorEnabled() || null == formatter) {
            logContext.clear();
            return;
        }

        LogContext asyncContext = logContext.clone();
        logContext.clear();
        CompletableFuture.runAsync(() -> {
            Pair<String, List<?>> pair = formatter.formatLogContext(asyncContext);
            logger.error(pair.getFirst(), pair.getSecond().toArray());
        });
    }

    public static class LogContext implements Serializable, Cloneable {
        private final static long serialVersionUID = 8228778036883035515L;

        private String message;
        private Throwable throwable;
        private Map<String, Object> context;

        public LogContext() {
            this.context = new LinkedHashMap<>();
        }

        public LogContext(String message, Throwable throwable, Map<String, Object> context) {
            this.message = message;
            this.throwable = throwable;
            this.context = new LinkedHashMap<>(context);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public LogContext clone() {
            return new LogContext(message, throwable, context);
        }

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
