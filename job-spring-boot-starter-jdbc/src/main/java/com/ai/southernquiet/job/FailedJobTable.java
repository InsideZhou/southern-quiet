package com.ai.southernquiet.job;

import instep.dao.sql.*;

public class FailedJobTable extends Table {
    public FailedJobTable(String tableName) {
        super(tableName);
    }

    public IntegerColumn id = longColumn("id").primary();
    public BinaryColumn payload = lob("payload");
    public StringColumn exception = text("exception");
    public IntegerColumn failureCount = integer("failureCount").notnull();
    public DateTimeColumn createdAt = datetime("createdAt").notnull();
    public DateTimeColumn lastExecutionStartedAt = datetime("lastExecutionStartedAt").notnull();
}
