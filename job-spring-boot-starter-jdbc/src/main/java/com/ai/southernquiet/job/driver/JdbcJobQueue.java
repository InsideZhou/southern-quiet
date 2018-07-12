package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.JobQueue;
import com.ai.southernquiet.job.JobTable;
import com.ai.southernquiet.util.SerializationUtils;
import instep.dao.DaoException;
import instep.dao.Plan;
import instep.dao.sql.ColumnExtensionKt;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.TableRow;
import instep.dao.sql.TransactionTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class JdbcJobQueue<T extends Serializable> implements JobQueue<T> {
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

    private JobTable jobTable;
    private InstepSQL instepSQL;
    private ThreadLocal<TableRow> lastDequeuedTableRow = new ThreadLocal<>();

    public JdbcJobQueue(JobTable jobTable, InstepSQL instepSQL) {
        this.jobTable = jobTable;
        this.instepSQL = instepSQL;
    }

    @Override
    public void enqueue(T job) {
        Plan plan;
        try {
            plan = jobTable.insert()
                .addValue(jobTable.payload, serialize(job))
                .addValue(jobTable.createdAt, Instant.now());

            instepSQL.executor().execute(plan);
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public T dequeue() {
        InputStream data = TransactionTemplate.INSTANCE.repeatable((context) -> {
            Plan plan = jobTable.select(ColumnExtensionKt.min(jobTable.id)).where(ColumnExtensionKt.isNull(jobTable.executionStartedAt));
            String scalar;
            try {
                scalar = instepSQL.executor().executeScalar(plan);
            }
            catch (DaoException e) {
                throw new RuntimeException(e);
            }

            if (!StringUtils.hasText(scalar)) return null;

            long min = Long.parseLong(scalar);

            Plan updatePlan;
            TableRow row;
            try {
                updatePlan = jobTable.update().set(jobTable.executionStartedAt, Instant.now()).where(min);

                instepSQL.executor().execute(updatePlan);
                row = jobTable.get(min);
            }
            catch (DaoException e) {
                throw new RuntimeException(e);
            }

            lastDequeuedTableRow.set(row);
            return Objects.requireNonNull(row).get(jobTable.payload);
        });

        return null == data ? null : deserialize(data);
    }

    public TableRow getLastDequeuedTableRow() {
        return lastDequeuedTableRow.get();
    }
}
