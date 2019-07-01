package me.insidezhou.southernquiet.job;

import instep.dao.sql.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FailedJobTable extends Table {
    public FailedJobTable(String tableName) {
        super(tableName, "任务记录");
    }

    public IntegerColumn id = autoIncrementLong("id").primary();
    public BinaryColumn payload = lob("payload").comment("任务数据");
    public StringColumn exception = text("exception").comment("任务执行异常");
    public StringColumn workingStatus = varchar("working_status", 16).comment("任务执行状态: Prepared=已准备好, Retry=重试中, Done=已完成");
    public IntegerColumn failureCount = integer("failure_count").notnull().comment("任务失败次数");
    public DateTimeColumn createdAt = datetime("created_at").notnull().comment("任务创建时间");
    public DateTimeColumn lastExecutionStartedAt = datetime("last_execution_started_at").comment("任务上次开始执行的时间");
}
