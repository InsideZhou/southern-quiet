package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.FailedJobTable;
import com.ai.southernquiet.job.JdbcJobAutoConfiguration;
import com.ai.southernquiet.job.JobEngine;
import com.ai.southernquiet.job.JobProcessor;
import com.ai.southernquiet.util.SerializationUtils;
import instep.dao.DaoException;
import instep.dao.sql.*;
import instep.dao.sql.dialect.MySQLDialect;
import instep.dao.sql.dialect.PostgreSQLDialect;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class JdbcJobEngine<T extends Serializable> extends AbstractJobEngine<T> implements JobEngine<T> {
    private final static Log log = LogFactory.getLog(JdbcJobEngine.class);

    public enum WorkingStatus {
        Prepared, Retry, Done
    }

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
    private JdbcJobAutoConfiguration.Properties properties;

    private ThreadLocal<Long> currentJobId = new ThreadLocal<>();

    public JdbcJobEngine(FailedJobTable failedJobTable, InstepSQL instepSQL, JdbcJobAutoConfiguration.Properties properties) {
        this.failedJobTable = failedJobTable;
        this.instepSQL = instepSQL;
        this.properties = properties;
    }

    @Override
    public void arrange(T job) {
        Instant now = Instant.now();

        JobCreated<T> jobCreated = instepSQL.transaction(context -> {
            JobCreated<T> result = new JobCreated<>();

            try {
                SQLPlan plan = failedJobTable.insert()
                    .addValue(failedJobTable.payload, serialize(job))
                    .addValue(failedJobTable.failureCount, 0)
                    .addValue(failedJobTable.workingStatus, WorkingStatus.Prepared)
                    .addValue(failedJobTable.createdAt, now);

                Long id = Long.parseLong(instepSQL.executor().executeScalar(plan));
                result.setId(id);
            }
            catch (DaoException e) {
                throw new RuntimeException(e);
            }

            result.setProcessor(getProcessor(job)); //放在同步的位置，以便调用端可以方便的感知到任务创建失败异常。

            return result;
        });

        asyncRunner.run(() -> {
            currentJobId.set(jobCreated.getId());
            try {
                exec(job, jobCreated.getProcessor());
            }
            catch (Exception e) {
                addToFailedTable(jobCreated.getId(), e);
            }
        });
    }

    static class JobCreated<T> {
        private Long id;
        private JobProcessor<T> processor;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public JobProcessor<T> getProcessor() {
            return processor;
        }

        public void setProcessor(JobProcessor<T> processor) {
            this.processor = processor;
        }
    }

    private void exec(T job, JobProcessor<T> processor) throws Exception {
        Instant now = Instant.now();
        Long jobId = currentJobId.get();
        currentJobId.set(null);

        Exception exception = instepSQL.transaction(context -> {
            try {
                SQLPlan plan = failedJobTable.update()
                    .set(failedJobTable.workingStatus, WorkingStatus.Done)
                    .set(failedJobTable.lastExecutionStartedAt, now)
                    .whereKey(jobId);

                int rowAffected = instepSQL.executor().executeUpdate(plan);
                if (1 != rowAffected) {
                    return new RuntimeException(String.format("任务在处理前，任务状态更新结果不正确，jobId=%s，rowAffected=%s", jobId, rowAffected));
                }

                processor.process(job);
            }
            catch (Exception e) {
                return e;
            }

            return null;
        });

        if (null == exception) {
            SQLPlan plan = failedJobTable.delete().whereKey(jobId);
            instepSQL.executor().execute(plan);
        }
        else {
            throw exception;
        }
    }

    public void retryFailedJob() {
        try {
            SQLPlan plan = failedJobTable.select()
                .where(
                    ColumnExtensionKt.gt(failedJobTable.failureCount, 0),
                    ColumnExtensionKt.isNull(failedJobTable.workingStatus),
                    null == properties.getFailedJobRetryInterval() ? lastExecutionStartedAtPlusFailedCountIntervalLesserThanNow() :
                        lastExecutionStartedAtPlusIntervalLesserThanNow(properties.getFailedJobRetryInterval().getSeconds())
                )
                .limit(1)
                .orderBy(ColumnExtensionKt.asc(failedJobTable.lastExecutionStartedAt)).debug();

            List<TableRow> rowList = instepSQL.executor().execute(plan, TableRow.class);
            if (rowList.size() > 0) {
                TableRow row = rowList.get(0);

                Long jobId = row.getLong(failedJobTable.id);

                plan = failedJobTable.update()
                    .set(failedJobTable.workingStatus, WorkingStatus.Retry)
                    .where(ColumnExtensionKt.isNull(failedJobTable.workingStatus))
                    .whereKey(jobId).debug();

                if (instepSQL.executor().executeUpdate(plan) > 0) {
                    T job = deserialize(row.get(failedJobTable.payload));
                    currentJobId.set(jobId);

                    exec(job, getProcessor(job));
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanWorkingStatus() {
        try {
            SQLPlan plan = failedJobTable.update()
                .set(failedJobTable.workingStatus, null)
                .where(
                    ColumnExtensionKt.inArray(failedJobTable.workingStatus, new WorkingStatus[]{WorkingStatus.Retry, WorkingStatus.Done}),
                    lastExecutionStartedAtPlusIntervalLesserThanNow(properties.getWorkerStatusCleanInterval().getSeconds())
                )
                .debug();

            int rowAffected = instepSQL.executor().executeUpdate(plan);
            if (rowAffected > 0) {
                log.info(String.format("已经更正%s行状态异常的Job记录", rowAffected));
            }
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    private Condition lastExecutionStartedAtPlusIntervalLesserThanNow(long interval) {
        Dialect dialect = failedJobTable.getDialect();

        if (PostgreSQLDialect.class.isInstance(dialect)) {
            return Condition.Companion.plain(failedJobTable.lastExecutionStartedAt.getName() +
                "'" + interval + " SECONDS'::INTERVAL < CURRENT_TIMESTAMP");
        }
        else if (MySQLDialect.class.isInstance(dialect)) {
            return Condition.Companion.plain(
                "DATE_ADD(" + failedJobTable.lastExecutionStartedAt.getName() +
                    ", INTERVAL " + interval + " SECOND) < CURRENT_TIMESTAMP");
        }
        else {
            throw new UnsupportedOperationException("不支持当前数据库：" + dialect.getClass().getSimpleName());
        }
    }

    private Condition lastExecutionStartedAtPlusFailedCountIntervalLesserThanNow() {
        Dialect dialect = failedJobTable.getDialect();

        if (PostgreSQLDialect.class.isInstance(dialect)) {
            return Condition.Companion.plain(failedJobTable.lastExecutionStartedAt.getName() +
                " + ((" + failedJobTable.failureCount.getName() + " * 2) || ' SECONDS')::INTERVAL < CURRENT_TIMESTAMP");
        }
        else if (MySQLDialect.class.isInstance(dialect)) {
            return Condition.Companion.plain(
                "DATE_ADD(" + failedJobTable.lastExecutionStartedAt.getName() +
                    ", INTERVAL " + failedJobTable.failureCount.getName() + " * 2 SECOND) < CURRENT_TIMESTAMP");
        }
        else {
            throw new UnsupportedOperationException("不支持当前数据库：" + dialect.getClass().getSimpleName());
        }
    }

    void addToFailedTable(Long jobId, Exception exception) {
        try {
            SQLPlan plan = failedJobTable.update()
                .step(failedJobTable.failureCount, 1)
                .set(failedJobTable.workingStatus, null)
                .set(failedJobTable.exception, exception.getMessage() + "\n" + exception.toString())
                .whereKey(jobId);

            instepSQL.executor().execute(plan);
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }
}
