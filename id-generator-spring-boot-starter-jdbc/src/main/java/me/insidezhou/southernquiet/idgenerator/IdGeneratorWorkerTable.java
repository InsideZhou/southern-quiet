package me.insidezhou.southernquiet.idgenerator;

import instep.dao.sql.*;
import instep.dao.sql.dialect.MySQLDialect;
import instep.dao.sql.dialect.PostgreSQLDialect;
import instep.dao.sql.dialect.SQLServerDialect;
import me.insidezhou.southernquiet.instep.dao.TableWithColumnConditionGeneration;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.DateTimeColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.IntegerColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.StringColumn;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SuppressWarnings({"WeakerAccess", "unused"})
public class IdGeneratorWorkerTable extends TableWithColumnConditionGeneration {
    public IdGeneratorWorkerTable(String tableName) {
        super(tableName, "id生成器的worker记录");
    }

    public IntegerColumn workerId = integer("worker_id").primary().comment("对应id中的workerId部分。");
    public StringColumn workerName = varchar("worker_name", 512).comment("worker的名字，便于查看。");
    public DateTimeColumn workerTime = instant("worker_time").comment("worker上报的时间，用于防止时间回退导致id重复。");
    public StringColumn appId = varchar("app_id", 128).unique().comment("worker所在应用的标识，方便应用重启后获取其上次用过的workerId。");

    public static class Cleaner {
        private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(Cleaner.class);

        private final IdGeneratorWorkerTable workerTable;
        private final InstepSQL instepSQL;
        private final JdbcIdGeneratorAutoConfiguration.Properties properties;

        public Cleaner(IdGeneratorWorkerTable workerTable, InstepSQL instepSQL, JdbcIdGeneratorAutoConfiguration.Properties properties) {
            this.workerTable = workerTable;
            this.instepSQL = instepSQL;
            this.properties = properties;
        }

        @SuppressWarnings({"rawtypes", "SpringElInspection"})
        @Scheduled(cron = "#{jdbcIdGeneratorProperties.cleanCron}")
        @PostConstruct
        @PreDestroy
        public void clearConsiderDowned() {
            SQLPlan plan = workerTable.delete().where(lastWorkerTimePlusIntervalLesserThanNow()).trace();
            int rowAffected;
            try {
                rowAffected = instepSQL.executor().executeUpdate(plan);
            }
            catch (SQLPlanExecutionException e) {
                throw new RuntimeException(e);
            }

            if (rowAffected > 0) {
                log.message("已清理长时间无上报的Worker")
                    .context("rowAffected", rowAffected)
                    .info();
            }
        }

        private Condition lastWorkerTimePlusIntervalLesserThanNow() {
            Dialect dialect = workerTable.getDialect();

            long interval = properties.getConsiderWorkerDowned().getSeconds();

            if (dialect instanceof PostgreSQLDialect) {
                return new Condition(workerTable.workerTime.getName() + "+ INTERVAL '" + interval + " SECONDS' < CURRENT_TIMESTAMP");
            }
            else if (dialect instanceof MySQLDialect) {
                return new Condition(
                    "DATE_ADD(" + workerTable.workerTime.getName() +
                        ", INTERVAL " + interval + " SECOND) < CURRENT_TIMESTAMP");
            }
            else if (dialect instanceof SQLServerDialect) {
                return new Condition(
                    "DATEADD(second, " + interval + ", " + workerTable.workerTime.getName() + ") < CURRENT_TIMESTAMP");
            }
            else {
                throw new UnsupportedOperationException("不支持当前数据库：" + dialect.getClass().getSimpleName());
            }
        }
    }
}
