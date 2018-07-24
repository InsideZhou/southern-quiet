package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.FailedJobTable;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;
import instep.dao.DaoException;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.SQLPlan;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;

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

        try {
            SQLPlan plan = failedJobTable.insert()
                .addValue(failedJobTable.payload, serialize(job))
                .addValue(failedJobTable.failureCount, 0)
                .addValue(failedJobTable.createdAt, now)
                .addValue(failedJobTable.lastExecutionStartedAt, now);

            Long id = Long.parseLong(instepSQL.executor().executeScalar(plan));
            currentJobId.set(id);
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }

        super.enqueue(job);
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
                .set(failedJobTable.failureCount, 1)
                .set(failedJobTable.exception, e.getMessage() + "\n" + e.toString())
                .whereKey(currentJobId.get());

            instepSQL.executor().execute(plan);
        }
        catch (DaoException e1) {
            throw new RuntimeException(e1);
        }
    }
}
