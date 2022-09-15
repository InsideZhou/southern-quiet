package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.ColumnCondition;
import instep.dao.sql.ColumnExtensionsKt;
import instep.dao.sql.StringColumnType;
import instep.dao.sql.Table;
import org.jetbrains.annotations.NotNull;

public class StringColumn extends instep.dao.sql.StringColumn implements Column<StringColumn> {
    public StringColumn(@NotNull String name, @NotNull Table table, @NotNull StringColumnType type, int length) {
        super(name, table, type, length);
    }

    public StringColumn(@NotNull String name, @NotNull Table table, @NotNull StringColumnType type) {
        this(name, table, type, 256);
    }

    public ColumnCondition startsWith(String value) {
        return ColumnExtensionsKt.startsWith(this, value);
    }

    public ColumnCondition endsWith(String value) {
        return ColumnExtensionsKt.endsWith(this, value);
    }

    public ColumnCondition contains(String value) {
        return ColumnExtensionsKt.contains(this, value);
    }

    @NotNull
    @Override
    public StringColumn comment(@NotNull String txt) {
        return (StringColumn) super.comment(txt);
    }

    @NotNull
    @Override
    public StringColumn defaultValue(@NotNull String exp) {
        return (StringColumn) super.defaultValue(exp);
    }

    @NotNull
    @Override
    public StringColumn notnull() {
        return (StringColumn) super.notnull();
    }

    @NotNull
    @Override
    public StringColumn primary() {
        return (StringColumn) super.primary();
    }

    @NotNull
    @Override
    public StringColumn unique() {
        return (StringColumn) super.unique();
    }
}
