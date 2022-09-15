package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.Table;
import org.jetbrains.annotations.NotNull;

public class ArbitraryColumn extends instep.dao.sql.ArbitraryColumn implements Column<ArbitraryColumn> {
    public ArbitraryColumn(@NotNull String name, @NotNull Table table, @NotNull String definition) {
        super(name, table, definition);
    }

    @NotNull
    @Override
    public ArbitraryColumn comment(@NotNull String txt) {
        return (ArbitraryColumn) super.comment(txt);
    }

    @NotNull
    @Override
    public ArbitraryColumn defaultValue(@NotNull String exp) {
        return (ArbitraryColumn) super.defaultValue(exp);
    }

    @NotNull
    @Override
    public ArbitraryColumn notnull() {
        return (ArbitraryColumn) super.notnull();
    }

    @NotNull
    @Override
    public ArbitraryColumn primary() {
        return (ArbitraryColumn) super.primary();
    }

    @NotNull
    @Override
    public ArbitraryColumn unique() {
        return (ArbitraryColumn) super.unique();
    }
}
