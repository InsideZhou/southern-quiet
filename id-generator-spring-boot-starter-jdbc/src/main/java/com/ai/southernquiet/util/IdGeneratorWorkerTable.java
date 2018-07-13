package com.ai.southernquiet.util;

import instep.dao.sql.DateTimeColumn;
import instep.dao.sql.IntegerColumn;
import instep.dao.sql.StringColumn;
import instep.dao.sql.Table;

public class IdGeneratorWorkerTable extends Table {
    public IdGeneratorWorkerTable(String tableName) {
        super(tableName);
    }

    /**
     * 对应id中的workerId部分
     */
    public IntegerColumn workerId = integer("worker_id").primary();

    /**
     * worker的名字，便于查看。
     */
    public StringColumn workerName = varchar("worker_name", 512);

    /**
     * worker上报的时间，用于防止时间回退导致id重复。
     */
    public DateTimeColumn workerTime = instant("worker_time");

    /**
     * worker所在应用的标识，方便应用重启后获取其上次用过的workerId。
     */
    public StringColumn appId = varchar("app_id", 512).unique();
}
