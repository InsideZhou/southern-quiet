package me.insidezhou.southernquiet.idgenerator;

import instep.dao.DaoException;
import instep.dao.sql.*;
import me.insidezhou.southernquiet.util.IdGenerator;
import me.insidezhou.southernquiet.util.Metadata;
import me.insidezhou.southernquiet.util.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class JdbcIdGenerator implements IdGenerator {
    private final static Logger log = LoggerFactory.getLogger(JdbcIdGenerator.class);

    private IdGenerator idGenerator;
    private Metadata metadata;
    private IdGeneratorWorkerTable workerTable;
    private InstepSQL instepSQL;
    private int workerIdInUse;
    private int maxWorkerId;

    public JdbcIdGenerator(Metadata metadata, IdGeneratorWorkerTable workerTable, InstepSQL instepSQL, JdbcIdGeneratorAutoConfiguration.Properties properties) {
        this.metadata = metadata;
        this.workerTable = workerTable;
        this.instepSQL = instepSQL;

        Assert.hasText(metadata.getRuntimeId(), "应用的id不能为空");

        maxWorkerId = SnowflakeIdGenerator.maxIntegerAtBits(properties.getWorkerIdBits());
        workerIdInUse = getWorkerId();

        idGenerator = new SnowflakeIdGenerator(
            workerIdInUse,
            properties.getTimestampBits(),
            properties.getHighPaddingBits(),
            properties.getWorkerIdBits(),
            properties.getLowPaddingBits(),
            properties.getEpoch(),
            properties.isRandomSequenceStart() ? new Random() : null,
            properties.getSequenceStartRange(),
            properties.getTickAccuracy()
        );
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
        while (workerId <= maxWorkerId) {
            plan = workerTable.select(workerTable.workerId).where(ColumnExtensionKt.eq(workerTable.workerId, workerId));

            try {
                if (StringUtils.isEmpty(instepSQL.executor().executeScalar(plan))) {
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
    }

    @Scheduled(fixedRate = 5000)
    @PreDestroy
    public void report() {
        Instant now = Instant.now();
        String runtimeId = metadata.getRuntimeId();

        try {
            SQLPlan plan = workerTable.update()
                .set(workerTable.workerTime, now)
                .where(
                    ColumnExtensionKt.eq(workerTable.workerId, workerIdInUse)
                        .and(ColumnExtensionKt.eq(workerTable.appId, runtimeId))
                        .and(ColumnExtensionKt.lt(workerTable.workerTime, now))
                ).debug();

            int rowAffected = instepSQL.executor().executeUpdate(plan);
            if (1 != rowAffected) {
                log.warn("workerTime上报异常。workerId={},appId={},rowAffected={},time={}", workerIdInUse, runtimeId, rowAffected, now);
            }
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long generate() {
        return idGenerator.generate();
    }

    @Override
    public long getTimestampFromId(long id) {
        return idGenerator.getTimestampFromId(id);
    }

    @Override
    public long getWorkerFromId(long id) {
        return idGenerator.getWorkerFromId(id);
    }

    @Override
    public long getSequenceFromId(long id) {
        return idGenerator.getSequenceFromId(id);
    }
}
