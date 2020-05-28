package me.insidezhou.southernquiet.logging;

import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class SouthernQuietLoggerFactory {
    private static final Map<String, SouthernQuietLogger> southernQuietLoggers = new HashMap<>();

    private static SouthernQuietLogFormatter formatter = new SouthernQuietLogFormatter();

    public static void setFormatter(SouthernQuietLogFormatter formatter) {
        SouthernQuietLoggerFactory.formatter = formatter;
        southernQuietLoggers.values().forEach(logger -> logger.setFormatter(formatter));
    }

    public static synchronized SouthernQuietLogger getLogger(String name) {
        SouthernQuietLogger logger = southernQuietLoggers
            .computeIfAbsent(name, nm -> new SouthernQuietLogger(LoggerFactory.getLogger(nm), formatter));

        southernQuietLoggers.putIfAbsent(name, logger);
        return logger;
    }

    public static SouthernQuietLogger getLogger(Class<?> cls) {
        return getLogger(cls.getName());
    }
}
