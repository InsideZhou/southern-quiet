package me.insidezhou.southernquiet.idgenerator;

import instep.dao.DaoException;
import instep.dao.sql.*;
import instep.util.LongIdGenerator;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.util.IdGenerator;
import me.insidezhou.southernquiet.util.Metadata;
import me.insidezhou.southernquiet.util.SnowflakeIdGenerator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JdbcIdGenerator implements IdGenerator {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(JdbcIdGenerator.class);

    private static CronTrigger reportTrigger;
    private static boolean clockMovedBack = false;

    private synchronized static void initReporter(JdbcIdGeneratorAutoConfiguration.Properties properties, TaskScheduler taskScheduler, JdbcIdGenerator jdbcIdGenerator) {
        if (null == reportTrigger) {
            reportTrigger = new CronTrigger(properties.getReportCron());
            taskScheduler.schedule(jdbcIdGenerator::report, reportTrigger);
        }
    }

    private final IdGenerator idGenerator;
    private final String runtimeId;
    private final IdGeneratorWorkerTable workerTable;
    private final InstepSQL instepSQL;
    private final int workerIdInUse;
    private final int maxWorkerId;

    public JdbcIdGenerator(Metadata metadata,
                           IdGeneratorWorkerTable workerTable,
                           InstepSQL instepSQL,
                           int timestampBits,
                           int highPaddingBits,
                           int workerIdBits,
                           int lowPaddingBits,
                           long epochInSeconds,
                           int sequenceStartRange,
                           boolean randomSequenceStart,
                           int tickAccuracy
    ) {
        this.runtimeId = metadata.getRuntimeId();
        this.workerTable = workerTable;
        this.instepSQL = instepSQL;

        Assert.hasText(metadata.getRuntimeId(), "应用的id不能为空");

        maxWorkerId = LongIdGenerator.Companion.maxIntAtBits(workerIdBits);
        workerIdInUse = getWorkerId();

        idGenerator = new SnowflakeIdGenerator(
            workerIdInUse,
            timestampBits,
            highPaddingBits,
            workerIdBits,
            lowPaddingBits,
            epochInSeconds,
            sequenceStartRange,
            randomSequenceStart ? new Random() : null,
            tickAccuracy
        );

        log.message(JdbcIdGenerator.class.getSimpleName() + "开始工作")
            .context("workerId", workerIdInUse)
            .context("timestampBits", timestampBits)
            .context("highPaddingBits", highPaddingBits)
            .context("lowPaddingBits", lowPaddingBits)
            .context("epoch", Instant.ofEpochSecond(epochInSeconds))
            .context("sequenceStartRange", sequenceStartRange)
            .context("randomSequenceStart", randomSequenceStart)
            .info();
    }

    @SuppressWarnings("WeakerAccess")
    public JdbcIdGenerator(Metadata metadata,
                           IdGeneratorWorkerTable workerTable,
                           InstepSQL instepSQL,
                           JdbcIdGeneratorAutoConfiguration.Properties properties,
                           TaskScheduler taskScheduler
    ) {
        this(
            metadata,
            workerTable,
            instepSQL,
            properties.getTimestampBits(),
            properties.getHighPaddingBits(),
            properties.getWorkerIdBits(),
            properties.getLowPaddingBits(),
            properties.getEpoch(),
            properties.getSequenceStartRange(),
            properties.isRandomSequenceStart(),
            properties.getTickAccuracy()
        );

        initReporter(properties, taskScheduler, this);
    }

    private int getWorkerId() {
        SQLPlan<TableSelectPlan> plan = workerTable.select().where(workerTable.appId.eq(runtimeId)).orderBy(workerTable.workerTime.desc()).trace();
        List<TableRow> rows;
        try {
            rows = instepSQL.executor().execute(plan, TableRow.class);
        }
        catch (SQLPlanExecutionException e) {
            throw new RuntimeException(e);
        }

        if (rows.isEmpty()) return newWorkerId();

        TableRow row = rows.get(0);

        Instant previousWorkerTime = row.get(workerTable.workerTime);
        waitUntilPreviousWorkerTimePassed(previousWorkerTime);

        log.message("获取原workerId成功")
            .context("appId", workerTable.appId)
            .context("workerName", workerTable.workerName)
            .context("workerId", workerTable.workerId)
            .context("workerTime", workerTable.workerTime)
            .debug();

        return row.get(workerTable.workerId);
    }

    private void waitUntilPreviousWorkerTimePassed(Instant previousWorkerTime) {
        Instant now = Instant.now();
        while (now.compareTo(previousWorkerTime) <= 0) {
            now = Instant.now();
        }
    }

    private int newWorkerId() {
        SQLPlan<TableSelectPlan> plan = workerTable.select(workerTable.workerId);
        List<Integer> exists = instepSQL.executor().execute(plan, TableRow.class).stream().map(row -> row.get(workerTable.workerId)).collect(Collectors.toList());
        List<Integer> available = IntStream.range(1, maxWorkerId).boxed().collect(Collectors.toList());
        available.removeAll(exists);

        for (Integer workerId : available) {
            SQLPlan<TableInsertPlan> insertPlan = workerTable.insert()
                .addValue(workerTable.appId, runtimeId)
                .addValue(workerTable.workerTime, Instant.now())
                .addValue(workerTable.workerId, workerId)
                .trace();

            int rowAffected;
            try {
                rowAffected = instepSQL.executor().executeUpdate(insertPlan);
            }
            catch (SQLPlanExecutionException e) {
                log.message("workerId已被占用，获取新workerId需要重试")
                    .context("workerId", workerId)
                    .exception(e)
                    .info();

                continue;
            }

            if (1 != rowAffected) {
                log.message("获取新workerId异常")
                    .context("rowAffected", rowAffected)
                    .context("workerId", workerId)
                    .error();

                break;
            }

            log.message("获取新workerId成功").context("workerId", workerId).debug();

            return workerId;
        }

        throw new RuntimeException("无法从数据库中获取workerId");
    }

    @PreDestroy
    public synchronized void report() {
        Instant now = Instant.now();

        try {
            SQLPlan<TableUpdatePlan> plan = workerTable.update()
                .set(workerTable.workerTime, now)
                .where(
                    workerTable.workerId.eq(workerIdInUse)
                        .and(workerTable.appId.eq(runtimeId))
                        .and(workerTable.workerTime.lt(now))
                ).trace();

            int rowAffected = instepSQL.executor().executeUpdate(plan);
            clockMovedBack = 0 == rowAffected;

            if (1 != rowAffected) {
                log.message("workerTime上报异常")
                    .context("workerId", workerIdInUse)
                    .context("appId", runtimeId)
                    .context("rowAffected", rowAffected)
                    .context("time", now)
                    .warn();
            }
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized long generate() {
        if (clockMovedBack) throw new RuntimeException("时钟已回退，无法发号。");

        return idGenerator.generate();
    }

    @Override
    public long getTicksFromId(long id) {
        return idGenerator.getTicksFromId(id);
    }

    @Override
    public long getTimestampFromId(long id) {
        return idGenerator.getTimestampFromId(id);
    }

    @Override
    public int getWorkerFromId(long id) {
        return idGenerator.getWorkerFromId(id);
    }

    @Override
    public int getSequenceFromId(long id) {
        return idGenerator.getSequenceFromId(id);
    }
}
