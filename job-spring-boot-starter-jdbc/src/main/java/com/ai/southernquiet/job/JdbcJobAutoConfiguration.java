package com.ai.southernquiet.job;

import com.ai.southernquiet.job.driver.JdbcJobQueue;
import instep.dao.DaoException;
import instep.dao.Plan;
import instep.dao.sql.ConnectionProvider;
import instep.dao.sql.Dialect;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.TransactionContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringFacetCodeInspection"})
@Configuration
@ConditionalOnClass(InstepSQL.class)
@EnableConfigurationProperties(JdbcJobAutoConfiguration.Properties.class)
public class JdbcJobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JdbcJobQueue jdbcJobQueue(JobTable jobTable, InstepSQL instepSQL) {
        return new JdbcJobQueue(jobTable, instepSQL);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectionProvider connectionProvider(DataSource dataSource, Dialect dialect) {
        return new TransactionContext.ConnectionProvider(dataSource, dialect);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobTable jobTable(Properties properties, InstepSQL instepSQL) {
        JobTable table = new JobTable(properties.getTable());

        Plan plan = table.create().debug();
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

        Plan plan = table.create();
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
