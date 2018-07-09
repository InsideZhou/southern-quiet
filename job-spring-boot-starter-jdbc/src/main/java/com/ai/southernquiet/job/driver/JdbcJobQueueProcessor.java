package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.AbstractJobQueueProcessor;
import com.ai.southernquiet.job.FailedJobTable;
import com.ai.southernquiet.job.JobTable;
import instep.dao.DaoException;
import instep.dao.Plan;
import instep.dao.sql.InstepSQL;
import instep.dao.sql.TableRow;

import java.time.Instant;

public class JdbcJobQueueProcessor extends AbstractJobQueueProcessor {
    private JdbcJobQueue jobQueue;
    private JobTable jobTable;
    private FailedJobTable failedJobTable;
    private InstepSQL instepSQL;

    public JdbcJobQueueProcessor(JdbcJobQueue jobQueue, JobTable jobTable, FailedJobTable failedJobTable, InstepSQL instepSQL) {
        this.jobQueue = jobQueue;
        this.jobTable = jobTable;
        this.failedJobTable = failedJobTable;
        this.instepSQL = instepSQL;
    }

    @Override
    public <T> void process() {
        instepSQL.transaction().repeatable(context -> {
            super.process();
            return null;
        });
    }

    @Override
    protected <T> T getJobFromQueue() {
        return jobQueue.dequeue();
    }

    @Override
    public <T> void onJobSuccess(T job) {
        TableRow row = jobQueue.getLastDequeuedTableRow();
        try {
            Plan plan = jobTable.delete().where(row.getLong(jobTable.id));
            instepSQL.executor().execute(plan);
        }
        catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> void onJobFail(T job, Exception e) {
        TableRow jobRow = jobQueue.getLastDequeuedTableRow();
        Long jobId = jobRow.getLong(jobTable.id);

        try {
            TableRow failedJobRow = failedJobTable.get(jobId);

            Plan plan;

            if (null == failedJobRow) {
                plan = failedJobTable.insert()
                    .addValue(failedJobTable.id, jobId)
                    .addValue(failedJobTable.payload, jobRow.get(jobTable.payload))
                    .addValue(failedJobTable.failureCount, 1)
                    .addValue(failedJobTable.exception, e.toString())
                    .addValue(failedJobTable.createdAt, jobRow.get(jobTable.createdAt))
                    .addValue(failedJobTable.lastExecutionStartedAt, jobRow.get(jobTable.executionStartedAt));

                instepSQL.executor().execute(jobTable.delete().where(jobId));
            }
            else {
                plan = failedJobTable.update()
                    .set(failedJobTable.failureCount, failedJobRow.get(failedJobTable.failureCount) + 1)
                    .set(failedJobTable.exception, e.toString())
                    .set(failedJobTable.lastExecutionStartedAt, Instant.now());
            }

            instepSQL.executor().execute(plan);
        }
        catch (DaoException e1) {
            throw new RuntimeException(e1);
        }
    }
}
