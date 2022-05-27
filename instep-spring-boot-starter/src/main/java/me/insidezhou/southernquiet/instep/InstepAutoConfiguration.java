package me.insidezhou.southernquiet.instep;

import instep.Instep;
import instep.InstepLogger;
import instep.InstepLoggerFactory;
import instep.dao.sql.ConnectionProvider;
import instep.dao.sql.Dialect;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.TransactionRunner;
import instep.servicecontainer.ServiceNotFoundException;
import kotlin.jvm.functions.Function0;
import me.insidezhou.southernquiet.instep.dao.TemplateConnectionProvider;
import me.insidezhou.southernquiet.instep.dao.TemplateTransactionRunner;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnBean({DataSource.class, DataSourceProperties.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class InstepAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Instep instep(InstepLoggerFactory factory) {
        Instep.INSTANCE.bind(InstepLoggerFactory.class, factory);
        InstepLogger.Companion.setFactory(factory);

        return Instep.INSTANCE;
    }

    @Bean
    @ConditionalOnMissingBean
    public InstepLoggerFactory instepLoggerFactory() {
        return cls -> new InstepLogger() {
            private final SouthernQuietLogger logger = SouthernQuietLoggerFactory.getLogger(cls);

            @NotNull
            @Override
            public InstepLogger message(@NotNull String s) {
                logger.message(s);
                return this;
            }

            @NotNull
            @Override
            public InstepLogger exception(@NotNull Throwable throwable) {
                logger.exception(throwable);
                return this;
            }

            @NotNull
            @Override
            public InstepLogger context(@NotNull String s, @NotNull Object o) {
                logger.context(s, o);
                return this;
            }

            @NotNull
            @Override
            public InstepLogger context(@NotNull String s, @NotNull Function0<String> function0) {
                logger.context(s, function0::invoke);
                return this;
            }

            @Override
            public void debug() {
                logger.debug();
            }

            @Override
            public void info() {
                logger.info();
            }

            @Override
            public void warn() {
                logger.warn();
            }
        };
    }

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

    @Bean
    @ConditionalOnMissingBean
    public TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new TemplateTransactionRunner(transactionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectionProvider connectionProvider(DataSource dataSource, Dialect dialect, Instep instep, TransactionRunner transactionRunner) {
        return new TemplateConnectionProvider(dataSource, dialect, transactionRunner);
    }

    @Bean
    @ConditionalOnMissingBean
    public InstepSQL instepSQL(Instep instep, ConnectionProvider connectionProvider) {
        instep.bind(ConnectionProvider.class, connectionProvider);
        return InstepSQL.INSTANCE;
    }
}
