package com.ai.southernquiet.job;

import com.ai.southernquiet.job.driver.JdbcJobQueue;
import instep.dao.DaoException;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.SQLPlan;
import instep.springboot.SQLAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringFacetCodeInspection"})
@Configuration
@EnableConfigurationProperties(JdbcJobAutoConfiguration.Properties.class)
@EnableTransactionManagement
@AutoConfigureAfter(SQLAutoConfiguration.class)
public class JdbcJobAutoConfiguration {
    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnMissingBean
    public JdbcJobQueue jdbcJobQueue(FailedJobTable failedJobTable, InstepSQL instepSQL) {
        return new JdbcJobQueue(failedJobTable, instepSQL);
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
         * 失败Job的表名称。
         */
        private String failedTable = "failed_job";

        public String getFailedTable() {
            return failedTable;
        }

        public void setFailedTable(String failedTable) {
            this.failedTable = failedTable;
        }
    }
}
