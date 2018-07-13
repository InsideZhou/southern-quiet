package com.ai.southernquiet.job;

import com.ai.southernquiet.job.driver.JdbcJobQueue;
import instep.dao.DaoException;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.SQLPlan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringFacetCodeInspection"})
@Configuration
@EnableConfigurationProperties(JdbcJobAutoConfiguration.Properties.class)
public class JdbcJobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JdbcJobQueue jdbcJobQueue(JobTable jobTable, InstepSQL instepSQL) {
        return new JdbcJobQueue(jobTable, instepSQL);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobTable jobTable(Properties properties, InstepSQL instepSQL) {
        JobTable table = new JobTable(properties.getTable());

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

    @ConfigurationProperties("framework.job.jdbc")
    public static class Properties {
        /**
         * Job的表名称。
         */
        private String table = "job_table";

        /**
         * 失败Job的表名称。
         */
        private String failedTable = "failed_job_table";

        public String getFailedTable() {
            return failedTable;
        }

        public void setFailedTable(String failedTable) {
            this.failedTable = failedTable;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }
    }
}
