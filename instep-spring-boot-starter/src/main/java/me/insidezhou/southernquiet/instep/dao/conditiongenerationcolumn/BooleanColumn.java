package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.Table;
import org.jetbrains.annotations.NotNull;

public class BooleanColumn extends instep.dao.sql.BooleanColumn implements Column<BooleanColumn> {
    public BooleanColumn(@NotNull String name, @NotNull Table table) {
        super(name, table);
    }

    @NotNull
    @Override
    public BooleanColumn comment(@NotNull String txt) {
        return (BooleanColumn) super.comment(txt);
    }

    @NotNull
    @Override
    public BooleanColumn defaultValue(@NotNull String exp) {
        return (BooleanColumn) super.defaultValue(exp);
    }

    @NotNull
    @Override
    public BooleanColumn notnull() {
        return (BooleanColumn) super.notnull();
    }

    @NotNull
    @Override
    public BooleanColumn primary() {
        return (BooleanColumn) super.primary();
    }

    @NotNull
    @Override
    public BooleanColumn unique() {
        return (BooleanColumn) super.unique();
    }
}
