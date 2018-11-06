package com.ai.southernquiet.idgenerator;

import com.ai.southernquiet.util.IdGenerator;
import com.ai.southernquiet.util.Metadata;
import com.ai.southernquiet.util.SnowflakeIdGenerator;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
    @ConditionalOnMissingBean
    public IdGeneratorWorkerTable.Cleaner idGeneratorWorkerTableCleaner(IdGeneratorWorkerTable workerTable, InstepSQL instepSQL, JdbcIdGeneratorAutoConfiguration.Properties properties) {
        return new IdGeneratorWorkerTable.Cleaner(workerTable, instepSQL, properties);
    }

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public JdbcIdGenerator jdbcIdGenerator(Metadata metadata, IdGeneratorWorkerTable workerTable, InstepSQL instepSQL, Properties properties) {
        return new JdbcIdGenerator(metadata, workerTable, instepSQL, properties);
    }

    @ConfigurationProperties("southern-quiet.framework.util.id-generator")
    public static class Properties {
        private String workerTable = "ID_GENERATOR_WORKER";

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration considerWorkerDowned = Duration.ofDays(1);

        /**
         * timestamp - highPadding - worker - lowPadding - sequence
         *
         * @see SnowflakeIdGenerator
         */
        private int timestampBits = 32;
        private int highPaddingBits = 0;
        private int workerIdBits = 12;
        private int lowPaddingBits = 2;
        private int sequenceStartRange = 1000;

        /**
         * Thu Feb 01 2018 00:00:00 GMT, seconds
         */
        private long epoch = 1517414400L;

        public int getSequenceStartRange() {
            return sequenceStartRange;
        }

        public void setSequenceStartRange(int sequenceStartRange) {
            this.sequenceStartRange = sequenceStartRange;
        }

        public long getEpoch() {
            return epoch;
        }

        public void setEpoch(long epoch) {
            this.epoch = epoch;
        }

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

        public Duration getConsiderWorkerDowned() {
            return considerWorkerDowned;
        }

        public void setConsiderWorkerDowned(Duration considerWorkerDowned) {
            this.considerWorkerDowned = considerWorkerDowned;
        }
    }
}
