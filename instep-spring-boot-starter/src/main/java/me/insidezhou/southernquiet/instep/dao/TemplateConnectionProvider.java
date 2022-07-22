package me.insidezhou.southernquiet.instep.dao;

import instep.dao.sql.ConnectionProvider;
import instep.dao.sql.Dialect;
import instep.dao.sql.TransactionRunner;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;

public class TemplateConnectionProvider implements ConnectionProvider {
    private final DataSource dataSource;
    private final Dialect dialect;
    private final TransactionRunner transactionRunner;

    public TemplateConnectionProvider(DataSource dataSource, Dialect dialect, TransactionRunner transactionRunner) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.transactionRunner = transactionRunner;
    }

    @NotNull
    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @NotNull
    @Override
    public TransactionRunner getTransactionRunner() {
        return transactionRunner;
    }

    @NotNull
    @Override
    public Connection getConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }

    @Override
    public void releaseConnection(@NotNull Connection connection) {
        DataSourceUtils.releaseConnection(connection, dataSource);
    }
}
