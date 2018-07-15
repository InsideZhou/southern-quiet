package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AsyncJobQueue;
import com.ai.southernquiet.job.FailedJobTable;
import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.util.SerializationUtils;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.SQLPlan;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;

public class JdbcJobQueue<T extends Serializable> extends AsyncJobQueue<T> implements JobQueue<T> {
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

    public JdbcJobQueue(FailedJobTable failedJobTable, InstepSQL instepSQL) {
        this.failedJobTable = failedJobTable;
        this.instepSQL = instepSQL;
    }

    @Override
    public void enqueue(T job) {
        process(job);
    }

    protected void onJobFail(T job, Exception e) throws Exception {
        Instant now = Instant.now();

        SQLPlan plan = failedJobTable.insert()
            .addValue(failedJobTable.payload, serialize(job))
            .addValue(failedJobTable.failureCount, 1)
            .addValue(failedJobTable.exception, e.getMessage() + "\n" + e.toString())
            .addValue(failedJobTable.createdAt, now)
            .addValue(failedJobTable.lastExecutionStartedAt, now);

        instepSQL.executor().execute(plan);
    }
}
