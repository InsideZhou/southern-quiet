package me.insidezhou.southernquiet.job;

import me.insidezhou.southernquiet.job.driver.JdbcJobEngine;
import instep.dao.DaoException;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.SQLPlan;
import instep.springboot.SQLAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringFacetCodeInspection"})
@Configuration
@EnableConfigurationProperties
@EnableTransactionManagement
@AutoConfigureAfter(SQLAutoConfiguration.class)
public class JdbcJobAutoConfiguration {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public JdbcJobEngine jdbcJobQueue(FailedJobTable failedJobTable, InstepSQL instepSQL, Properties properties) {
        return new JdbcJobEngine(failedJobTable, instepSQL, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FailedJobTable failedJobTable(Properties properties, InstepSQL instepSQL) {
        FailedJobTable table = new FailedJobTable(properties.getFailedTable());

        SQLPlan plan = table.create().debug();
        try {
            instepSQL.executor().execute(plan);
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
