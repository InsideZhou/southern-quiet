package com.ai.southernquiet.util;

import instep.dao.DaoException;
import instep.dao.sql.*;
import instep.util.LongIdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class JdbcIdGenerator implements IdGenerator {
    public final static int AutoReportInterval = 5; //秒
    private final static Log log = LogFactory.getLog(JdbcIdGenerator.class);

    private LongIdGenerator longIdGenerator;
    private Metadata metadata;
    private IdGeneratorWorkerTable workerTable;
    private InstepSQL instepSQL;
    private AsyncRunner asyncRunner;
    private int workerIdInUse;

    private Duration reportInterval;
    private Instant lastWorkerTime;

    public JdbcIdGenerator(Metadata metadata, IdGeneratorWorkerTable workerTable, InstepSQL instepSQL, AsyncRunner asyncRunner, JdbcIdGeneratorAutoConfiguration.Properties properties) {
        this.metadata = metadata;
        this.workerTable = workerTable;
        this.instepSQL = instepSQL;
        this.asyncRunner = asyncRunner;

        Assert.hasText(metadata.getRuntimeId(), "应用的id不能为空");

        workerIdInUse = getWorkerId();

        longIdGenerator = new LongIdGenerator(
            workerIdInUse,
            properties.getTimestampBits(),
            properties.getHighPaddingBits(),
            properties.getWorkerIdBits(),
            properties.getLowPaddingBits()
        );

        reportInterval = properties.getReportInterval();
        lastWorkerTime = Instant.now();
    }

    private int getWorkerId() {
        String appId = metadata.getRuntimeId();

        SQLPlan plan = workerTable.select().where(ColumnExtensionKt.eq(workerTable.appId, appId));
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

        return row.get(workerTable.workerId);
    }

    private void waitUntilPreviousWorkerTimePassed(Instant previousWorkerTime) {
        Instant now = Instant.now();
        while (now.compareTo(previousWorkerTime) <= 0) {
            now = Instant.now();
        }
    }

    private int newWorkerId() {
        return instepSQL.transaction().repeatable(context -> {
            SQLPlan plan = workerTable.select(ColumnExtensionKt.min(workerTable.workerId))
                .where(ColumnExtensionKt.isNull(workerTable.appId).and(ColumnExtensionKt.isNull(workerTable.workerTime)));

            Integer workerId;
            try {
                workerId = instepSQL.executor().executeScalar(plan, Integer.class);
            }
            catch (SQLPlanExecutionException e) {
                throw new RuntimeException(e);
            }

            if (null != workerId) return workerId;

            workerId = 0;
            while (workerId <= LongIdGenerator.Companion.maxIntegerAtBits(12)) {
                plan = workerTable.select(workerTable.workerId).where(ColumnExtensionKt.eq(workerTable.workerId, workerId));

                try {
                    if (!StringUtils.hasText(instepSQL.executor().executeScalar(plan))) {
                        plan = workerTable.insert()
                            .addValue(workerTable.appId, metadata.getRuntimeId())
                            .addValue(workerTable.workerTime, Instant.now())
                            .addValue(workerTable.workerId, workerId);

                        int rowAffected = instepSQL.executor().executeUpdate(plan);
                        Assert.isTrue(1 == rowAffected, "workerId插入异常。rowAffected=" + rowAffected);

                        return workerId;
                    }
                }
                catch (DaoException e) {
                    throw new RuntimeException(e);
                }

                ++workerId;
            }

            throw new RuntimeException("无法从数据库中获取workerId");
        });
    }

    @Scheduled(fixedRate = AutoReportInterval * 1000)
    @PreDestroy
    public void report() {
        Instant now = Instant.now();

        instepSQL.transaction().committed(context -> {
            String runtimeId = metadata.getRuntimeId();

            try {
                SQLPlan plan = workerTable.update()
                    .set(workerTable.workerTime, now)
                    .where(
                        ColumnExtensionKt.eq(workerTable.workerId, workerIdInUse)
                            .and(ColumnExtensionKt.eq(workerTable.appId, runtimeId))
                            .and(ColumnExtensionKt.lt(workerTable.workerTime, now))
                    );

                int rowAffected = instepSQL.executor().executeUpdate(plan);
                if (1 != rowAffected) {
                    log.warn(String.format("workerTime上报异常。workerId=%s,appId=%s,rowAffected=%s", workerIdInUse, runtimeId, rowAffected));
                }
                else {
                    lastWorkerTime = now;
                }
            }
            catch (DaoException e) {
                throw new RuntimeException(e);
            }

            return null;
        });
    }

    @Override
    public long generate() {
        long id = longIdGenerator.generate();

        Instant now = Instant.now();
        if (now.isAfter(lastWorkerTime.plus(reportInterval))) {
            asyncRunner.run(this::report);
        }

        return id;
    }
}
