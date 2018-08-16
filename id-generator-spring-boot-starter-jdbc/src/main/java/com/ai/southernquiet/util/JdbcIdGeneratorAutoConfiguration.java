package com.ai.southernquiet.util;

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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties(JdbcIdGeneratorAutoConfiguration.Properties.class)
@EnableTransactionManagement
@EnableScheduling
@AutoConfigureAfter(SQLAutoConfiguration.class)
public class JdbcIdGeneratorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorWorkerTable idGeneratorWorkerTable(Properties properties, InstepSQL instepSQL) {
        IdGeneratorWorkerTable table = new IdGeneratorWorkerTable(properties.getWorkerTable());

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
    @ConditionalOnMissingBean(IdGenerator.class)
    public JdbcIdGenerator jdbcIdGenerator(Metadata metadata, IdGeneratorWorkerTable workerTable, InstepSQL instepSQL, Properties properties) {
        return new JdbcIdGenerator(metadata, workerTable, instepSQL, properties);
    }

    @ConfigurationProperties("southern-quiet.framework.util.id-generator")
    public static class Properties {
        private String workerTable = "id_generator_worker";

        /**
         * timestamp - highPadding - worker - lowPadding - sequence
         *
         * @see instep.util.LongIdGenerator
         */
        private int timestampBits = 32;
        private int highPaddingBits = 0;
        private int workerIdBits = 12;
        private int lowPaddingBits = 4;

        public int getTimestampBits() {
            return timestampBits;
        }

        public void setTimestampBits(int timestampBits) {
            this.timestampBits = timestampBits;
        }

        public int getHighPaddingBits() {
            return highPaddingBits;
        }

        public void setHighPaddingBits(int highPaddingBits) {
            this.highPaddingBits = highPaddingBits;
        }

        public int getWorkerIdBits() {
            return workerIdBits;
        }

        public void setWorkerIdBits(int workerIdBits) {
            this.workerIdBits = workerIdBits;
        }

        public int getLowPaddingBits() {
            return lowPaddingBits;
        }

        public void setLowPaddingBits(int lowPaddingBits) {
            this.lowPaddingBits = lowPaddingBits;
        }

        public String getWorkerTable() {
            return workerTable;
        }

        public void setWorkerTable(String workerTable) {
            this.workerTable = workerTable;
        }
    }
}
