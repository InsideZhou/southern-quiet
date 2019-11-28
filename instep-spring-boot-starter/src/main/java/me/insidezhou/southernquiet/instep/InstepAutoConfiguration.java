package me.insidezhou.southernquiet.instep;

import instep.Instep;
import instep.InstepLogger;
import instep.dao.sql.ConnectionProvider;
import instep.dao.sql.Dialect;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.TransactionContext;
import instep.servicecontainer.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ConditionalOnBean({DataSource.class, DataSourceProperties.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class InstepAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Instep instep(InstepLogger instepLogger) {
        Instep.INSTANCE.bind(InstepLogger.class, instepLogger);
        InstepLogger.Companion.setLogger(instepLogger);

        return Instep.INSTANCE;
    }

    @SuppressWarnings("NullableProblems")
    @Bean
    @ConditionalOnMissingBean
    public InstepLogger instepLogger() {
        final Map<String, Logger> loggerCache = new ConcurrentHashMap<>();
        InstepLogger instepLogger = new InstepLogger() {
            @Override
            public boolean getEnableDebug() {
                return true;
            }

            @Override
            public boolean getEnableInfo() {
                return true;
            }

            @Override
            public boolean getEnableWarning() {
                return true;
            }

            @Override
            public void debug(String s, String s1) {
                Logger logger = loggerCache.getOrDefault(s1, LoggerFactory.getLogger(s1));
                logger.debug(s);
                loggerCache.putIfAbsent(s1, logger);
            }

            @Override
            public void info(String s, String s1) {
                Logger logger = loggerCache.getOrDefault(s1, LoggerFactory.getLogger(s1));
                logger.info(s);
                loggerCache.putIfAbsent(s1, logger);
            }

            @Override
            public void warning(String s, String s1) {
                Logger logger = loggerCache.getOrDefault(s1, LoggerFactory.getLogger(s1));
                logger.warn(s);
                loggerCache.putIfAbsent(s1, logger);
            }

            @Override
            public void debug(String s, Class<?> aClass) {
                debug(s, aClass.getName());
            }

            @Override
            public void info(String s, Class<?> aClass) {
                info(s, aClass.getName());
            }

            @Override
            public void warning(String s, Class<?> aClass) {
                warning(s, aClass.getName());
            }
        };
        Instep.INSTANCE.bind(InstepLogger.class, instepLogger);
        return instepLogger;
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    public Dialect dialect(DataSourceProperties dataSourceProperties, Instep instep) {
        Dialect dialect = Dialect.Companion.of(dataSourceProperties.getUrl());

        try {
            instep.make(Dialect.class);
        }
        catch (ServiceNotFoundException e) {
            instep.bind(Dialect.class, dialect);
        }

        return dialect;
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    public InstepSQL instepSQL(DataSource dataSource, Dialect dialect, Instep instep) {
        instep.bind(ConnectionProvider.class, new TransactionContext.ConnectionProvider(dataSource, dialect), "");

        return InstepSQL.INSTANCE;
    }

}
