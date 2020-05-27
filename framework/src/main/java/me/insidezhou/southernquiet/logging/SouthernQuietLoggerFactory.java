package me.insidezhou.southernquiet.logging;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class SouthernQuietLoggerFactory {
    private static final List<SouthernQuietLogger> southernQuietLoggers = new ArrayList<>();

    public static void setFormatter(SouthernQuietLogFormatter formatter) {
        southernQuietLoggers.forEach(logger -> logger.setFormatter(formatter));
    }

    public static SouthernQuietLogger getLogger(String name) {
        SouthernQuietLogger southernQuietLogger = new SouthernQuietLogger(LoggerFactory.getLogger(name));
        southernQuietLoggers.add(southernQuietLogger);
        return southernQuietLogger;
    }

    public static SouthernQuietLogger getLogger(Class<?> cls) {
        return getLogger(cls.getName());
    }
}
