package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.BinaryColumnType;
import instep.dao.sql.Table;
import org.jetbrains.annotations.NotNull;

public class BinaryColumn extends instep.dao.sql.BinaryColumn implements Column<BinaryColumn> {
    public BinaryColumn(@NotNull String name, @NotNull Table table, @NotNull BinaryColumnType type, int length) {
        super(name, table, type, length);
    }

    public BinaryColumn(@NotNull String name, @NotNull Table table, @NotNull BinaryColumnType type) {
        this(name, table, type, 0);
    }

    @NotNull
    @Override
    public BinaryColumn comment(@NotNull String txt) {
        return (BinaryColumn) super.comment(txt);
    }

    @NotNull
    @Override
    public BinaryColumn defaultValue(@NotNull String exp) {
        return (BinaryColumn) super.defaultValue(exp);
    }

    @NotNull
    @Override
    public BinaryColumn notnull() {
        return (BinaryColumn) super.notnull();
    }

    @NotNull
    @Override
    public BinaryColumn primary() {
        return (BinaryColumn) super.primary();
    }

    @NotNull
    @Override
    public BinaryColumn unique() {
        return (BinaryColumn) super.unique();
    }
}
