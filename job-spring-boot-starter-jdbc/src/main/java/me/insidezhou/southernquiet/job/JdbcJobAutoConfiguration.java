package me.insidezhou.southernquiet.job;

import instep.Instep;
import instep.dao.DaoException;
import instep.dao.sql.*;
import instep.servicecontainer.ServiceNotFoundException;
import me.insidezhou.southernquiet.job.driver.JdbcJobEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
@EnableConfigurationProperties
@EnableTransactionManagement
public class JdbcJobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Dialect dialect(@Value("${spring.datasource.url}") String url, Instep instep) {
        Dialect dialect = Dialect.Companion.of(url);

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

    @Bean
    @ConditionalOnMissingBean
    public JdbcJobEngine jdbcJobQueue(FailedJobTable failedJobTable, Properties properties) {
        return new JdbcJobEngine(failedJobTable, InstepSQL.INSTANCE, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FailedJobTable failedJobTable(Properties properties) {
        FailedJobTable table = new FailedJobTable(properties.getFailedTable());

        SQLPlan plan = table.create().debug();
        try {
            InstepSQL.INSTANCE.executor().execute(plan);
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }

        return table;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.job.jdbc")
    public Properties jdbcJobProperties() {
        return new Properties();
    }

    public static class Properties {
        /**
         * 失败Job的表名称。
         */
        private String failedTable = "FAILED_JOB";

        /**
         * 不正常状态Job的清理时间间隔，上次执行时间+间隔小于当前时间且状态不正常的任务会被清理，默认单位：秒。
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration workerStatusCleanInterval = Duration.ofMinutes(3);

        /**
         * 失败任务重试的事件间隔，默认单位：秒。
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration failedJobRetryInterval = null;

        public String getFailedTable() {
            return failedTable;
        }

        public void setFailedTable(String failedTable) {
            this.failedTable = failedTable;
        }

        public Duration getWorkerStatusCleanInterval() {
            return workerStatusCleanInterval;
        }

        public void setWorkerStatusCleanInterval(Duration workerStatusCleanInterval) {
            this.workerStatusCleanInterval = workerStatusCleanInterval;
        }

        public Duration getFailedJobRetryInterval() {
            return failedJobRetryInterval;
        }

        public void setFailedJobRetryInterval(Duration failedJobRetryInterval) {
            this.failedJobRetryInterval = failedJobRetryInterval;
        }
    }
}
