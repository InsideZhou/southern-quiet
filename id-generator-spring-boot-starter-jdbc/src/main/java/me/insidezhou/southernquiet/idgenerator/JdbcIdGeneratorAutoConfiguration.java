package me.insidezhou.southernquiet.idgenerator;

import instep.dao.DaoException;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.SQLPlan;
import me.insidezhou.southernquiet.util.IdGenerator;
import me.insidezhou.southernquiet.util.Metadata;
import me.insidezhou.southernquiet.util.SnowflakeIdGenerator;
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
@EnableConfigurationProperties
@EnableTransactionManagement
@EnableScheduling
@ConditionalOnMissingBean(IdGenerator.class)
public class JdbcIdGeneratorAutoConfiguration {
    @SuppressWarnings("rawtypes")
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
    public IdGeneratorWorkerTable.Cleaner idGeneratorWorkerTableCleaner(IdGeneratorWorkerTable workerTable, JdbcIdGeneratorAutoConfiguration.Properties properties) {
        return new IdGeneratorWorkerTable.Cleaner(workerTable, InstepSQL.INSTANCE, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcIdGenerator jdbcIdGenerator(Metadata metadata, IdGeneratorWorkerTable workerTable, Properties properties) {
        return new JdbcIdGenerator(metadata, workerTable, InstepSQL.INSTANCE, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("southern-quiet.framework.util.id-generator")
    public Properties jdbcIdGeneratorProperties() {
        return new Properties();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Properties {
        private String workerTable = "ID_GENERATOR_WORKER";

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration considerWorkerDowned = Duration.ofDays(1);

        private String reportCron = "*/5 * * * * *";

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
        private boolean randomSequenceStart = true;

        /**
         * 发号器的时间精度/步长，单位：毫秒。如果值为1000，则发号器每滴答（发生变化的最小时间单位）一次，时间实际过去了1秒。
         */
        private int tickAccuracy = 1000;

        /**
         * Thu Feb 01 2018 00:00:00 GMT, seconds
         */
        private long epoch = 1517414400L;

        public String getReportCron() {
            return reportCron;
        }

        public void setReportCron(String reportCron) {
            this.reportCron = reportCron;
        }

        public int getTickAccuracy() {
            return tickAccuracy;
        }

        public void setTickAccuracy(int tickAccuracy) {
            this.tickAccuracy = tickAccuracy;
        }

        public boolean isRandomSequenceStart() {
            return randomSequenceStart;
        }

        public void setRandomSequenceStart(boolean randomSequenceStart) {
            this.randomSequenceStart = randomSequenceStart;
        }

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
