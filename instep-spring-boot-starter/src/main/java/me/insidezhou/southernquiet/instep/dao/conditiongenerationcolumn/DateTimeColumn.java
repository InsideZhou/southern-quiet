package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.ColumnExtensionsKt;
import instep.dao.sql.DateTimeColumnType;
import instep.dao.sql.SelectExpression;
import instep.dao.sql.Table;
import org.jetbrains.annotations.NotNull;

public class DateTimeColumn extends instep.dao.sql.DateTimeColumn implements Column<DateTimeColumn> {
    public DateTimeColumn(@NotNull String name, @NotNull Table table, @NotNull DateTimeColumnType type) {
        super(name, table, type);
    }

    public SelectExpression max(String alias) {
        return ColumnExtensionsKt.max(this, alias);
    }

    public SelectExpression min() {
        return ColumnExtensionsKt.min(this, "");
    }

    @NotNull
    @Override
    public DateTimeColumn comment(@NotNull String txt) {
        return (DateTimeColumn) super.comment(txt);
    }

    @NotNull
    @Override
    public DateTimeColumn defaultValue(@NotNull String exp) {
        return (DateTimeColumn) super.defaultValue(exp);
    }

    @NotNull
    @Override
    public DateTimeColumn notnull() {
        return (DateTimeColumn) super.notnull();
    }

    @NotNull
    @Override
    public DateTimeColumn primary() {
        return (DateTimeColumn) super.primary();
    }

    @NotNull
    @Override
    public DateTimeColumn unique() {
        return (DateTimeColumn) super.unique();
    }
}
