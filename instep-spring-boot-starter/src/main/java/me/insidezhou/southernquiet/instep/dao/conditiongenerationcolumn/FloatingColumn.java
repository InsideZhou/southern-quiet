package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.FloatingColumnType;
import instep.dao.sql.Table;
import org.jetbrains.annotations.NotNull;

public class FloatingColumn extends instep.dao.sql.FloatingColumn implements NumberColumn<FloatingColumn> {
    public FloatingColumn(@NotNull String name, @NotNull Table table, @NotNull FloatingColumnType type, int precision, int scale) {
        super(name, table, type, precision, scale);
    }

    public FloatingColumn(@NotNull String name, @NotNull Table table, @NotNull FloatingColumnType type) {
        this(name, table, type, 0, 0);
    }

    @NotNull
    @Override
    public FloatingColumn comment(@NotNull String txt) {
        return (FloatingColumn) super.comment(txt);
    }

    @NotNull
    @Override
    public FloatingColumn defaultValue(@NotNull String exp) {
        return (FloatingColumn) super.defaultValue(exp);
    }

    @NotNull
    @Override
    public FloatingColumn notnull() {
        return (FloatingColumn) super.notnull();
    }

    @NotNull
    @Override
    public FloatingColumn primary() {
        return (FloatingColumn) super.primary();
    }

    @NotNull
    @Override
    public FloatingColumn unique() {
        return (FloatingColumn) super.unique();
    }
}
