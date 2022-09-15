package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.ColumnCondition;
import instep.dao.sql.ColumnExtensionsKt;
import instep.dao.sql.IntegerColumnType;
import instep.dao.sql.Table;
import org.jetbrains.annotations.NotNull;

public class IntegerColumn extends instep.dao.sql.IntegerColumn implements NumberColumn<IntegerColumn> {
    public IntegerColumn(@NotNull String name, @NotNull Table table, @NotNull IntegerColumnType type) {
        super(name, table, type);
    }

    public ColumnCondition gt(Enum<?> value) {
        return ColumnExtensionsKt.gt(this, value);
    }

    public ColumnCondition lt(Enum<?> value) {
        return ColumnExtensionsKt.lt(this, value);
    }

    public ColumnCondition gte(Enum<?> value) {
        return ColumnExtensionsKt.gte(this, value);
    }

    public ColumnCondition lte(Enum<?> value) {
        return ColumnExtensionsKt.lte(this, value);
    }

    public ColumnCondition inArray(Enum<?>[] values) {
        return ColumnExtensionsKt.inArray(this, values);
    }

    public ColumnCondition inValues(Enum<?>... values) {
        return ColumnExtensionsKt.inArray(this, values);
    }

    @NotNull
    @Override
    public IntegerColumn comment(@NotNull String txt) {
        return (IntegerColumn) super.comment(txt);
    }

    @NotNull
    @Override
    public IntegerColumn defaultValue(@NotNull String exp) {
        return (IntegerColumn) super.defaultValue(exp);
    }

    @NotNull
    @Override
    public IntegerColumn notnull() {
        return (IntegerColumn) super.notnull();
    }

    @NotNull
    @Override
    public IntegerColumn primary() {
        return (IntegerColumn) super.primary();
    }

    @NotNull
    @Override
    public IntegerColumn unique() {
        return (IntegerColumn) super.unique();
    }

    @NotNull
    @Override
    public IntegerColumn autoIncrement() {
        return (IntegerColumn) super.autoIncrement();
    }
}
