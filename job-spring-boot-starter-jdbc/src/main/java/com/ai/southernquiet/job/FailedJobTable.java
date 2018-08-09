package com.ai.southernquiet.job;

import instep.dao.sql.*;

@SuppressWarnings("unused")
public class FailedJobTable extends Table {
    public FailedJobTable(String tableName) {
        super(tableName);
    }

    public IntegerColumn id = autoIncrementLong("id").primary();
    public BinaryColumn payload = lob("payload");
    public StringColumn exception = text("exception");
    public StringColumn workingStatus = varchar("working_status", 16);
    public IntegerColumn failureCount = integer("failure_count").notnull();
    public DateTimeColumn createdAt = datetime("created_at").notnull();
    public DateTimeColumn lastExecutionStartedAt = datetime("last_execution_started_at").notnull();
}
