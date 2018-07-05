package com.ai.southernquiet.job;

import instep.dao.sql.BinaryColumn;
import instep.dao.sql.DateTimeColumn;
import instep.dao.sql.IntegerColumn;
import instep.dao.sql.Table;

public class JobTable extends Table {
    public JobTable(String tableName) {
        super(tableName);
    }

    public IntegerColumn id = autoIncrementLong("id").primary();
    public BinaryColumn payload = lob("payload");
    public DateTimeColumn createdAt = datetime("createdAt").notnull();
    public DateTimeColumn executionStartedAt = datetime("executionStartedAt");
}
