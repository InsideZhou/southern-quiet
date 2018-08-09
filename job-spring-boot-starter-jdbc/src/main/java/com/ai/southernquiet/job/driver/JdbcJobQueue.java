package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.FailedJobTable;
import com.ai.southernquiet.job.JobProcessor;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;
import instep.dao.DaoException;
import instep.dao.sql.ColumnExtensionKt;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.SQLPlan;
import instep.dao.sql.TableRow;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class JdbcJobQueue<T extends Serializable> extends OnSiteJobQueue<T> implements JobQueue<T> {
    public static <T extends Serializable> byte[] serialize(T data) {
        return SerializationUtils.serialize(data);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(InputStream stream) {
        byte[] bytes;
        try {
            bytes = StreamUtils.copyToByteArray(stream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return (T) SerializationUtils.deserialize(bytes);
    }

    private FailedJobTable failedJobTable;
    private InstepSQL instepSQL;
    private ThreadLocal<Long> currentJobId = new ThreadLocal<>();

    public JdbcJobQueue(FailedJobTable failedJobTable, InstepSQL instepSQL) {
        this.failedJobTable = failedJobTable;
        this.instepSQL = instepSQL;
    }

    @Override
    public void enqueue(T job) {
        Instant now = Instant.now();

        Long id;
        try {
            SQLPlan plan = failedJobTable.insert()
                .addValue(failedJobTable.payload, serialize(job))
                .addValue(failedJobTable.failureCount, 0)
                .addValue(failedJobTable.workingStatus, "init")
                .addValue(failedJobTable.createdAt, now)
                .addValue(failedJobTable.lastExecutionStartedAt, now);

            id = Long.parseLong(instepSQL.executor().executeScalar(plan));
            currentJobId.set(id);
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }

        JobProcessor<T> processor = getProcessor(job); //放在同步的位置，以便调用端可以方便的感知到异常。

        asyncRunner.run(() -> {
            currentJobId.set(id);
            process(job, processor);
        });
    }

    public void retryFailedJob() {
        try {
            SQLPlan plan = failedJobTable.select()
                .where(
                    ColumnExtensionKt.gt(failedJobTable.failureCount, 0),
                    ColumnExtensionKt.isNull(failedJobTable.workingStatus)
                )
                .limit(1)
                .orderBy(ColumnExtensionKt.asc(failedJobTable.lastExecutionStartedAt)).info();

            List<TableRow> rowList = instepSQL.executor().execute(plan, TableRow.class);
            if (rowList.size() > 0) {
                TableRow row = rowList.get(0);

                Instant now = Instant.now();
                Long jobId = row.getLong(failedJobTable.id);

                plan = failedJobTable.update()
                    .set(failedJobTable.workingStatus, "on")
                    .set(failedJobTable.lastExecutionStartedAt, now)
                    .where(ColumnExtensionKt.isNull(failedJobTable.workingStatus))
                    .whereKey(jobId);

                if (instepSQL.executor().executeUpdate(plan) > 0) {
                    T job = deserialize(row.get(failedJobTable.payload));
                    currentJobId.set(jobId);

                    process(job, getProcessor(job));
                }
            }
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onJobSuccess(T job) {
        super.onJobSuccess(job);

        try {
            SQLPlan plan = failedJobTable.delete().whereKey(currentJobId.get());
            instepSQL.executor().execute(plan);
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    protected void onJobFail(T job, Exception e) {
        try {
            SQLPlan plan = failedJobTable.update()
                .step(failedJobTable.failureCount, 1)
                .set(failedJobTable.workingStatus, null)
                .set(failedJobTable.exception, e.getMessage() + "\n" + e.toString())
                .whereKey(currentJobId.get());

            instepSQL.executor().execute(plan);
        }
        catch (DaoException e1) {
            throw new RuntimeException(e1);
        }
    }
}
