package me.insidezhou.southernquiet.instep.dao;

import instep.dao.sql.*;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.ArbitraryColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.BinaryColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.BooleanColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.DateTimeColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.FloatingColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.IntegerColumn;
import me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn.StringColumn;
import org.jetbrains.annotations.NotNull;

public abstract class TableWithColumnConditionGeneration extends Table {
    public TableWithColumnConditionGeneration(@NotNull String tableName, @NotNull String tableComment, @NotNull Dialect dialect) {
        super(tableName, tableComment, dialect);
    }

    public TableWithColumnConditionGeneration(@NotNull String tableName, @NotNull String tableComment) {
        super(tableName, tableComment);
    }

    public TableWithColumnConditionGeneration(@NotNull String tableName) {
        super(tableName);
    }

    @NotNull
    @Override
    public IntegerColumn autoIncrement(@NotNull String name) {
        IntegerColumn column = new IntegerColumn(name, this, IntegerColumnType.Int);
        column.autoIncrement();
        return column;
    }

    @NotNull
    @Override
    public IntegerColumn autoIncrementLong(@NotNull String name) {
        IntegerColumn column = new IntegerColumn(name, this, IntegerColumnType.Long);
        column.autoIncrement();
        return column;
    }

    @NotNull
    @Override
    public BooleanColumn bool(@NotNull String name) {
        return new BooleanColumn(name, this);
    }

    @NotNull
    @Override
    public StringColumn json(@NotNull String name) {
        return new StringColumn(name, this, StringColumnType.JSON);
    }

    @NotNull
    @Override
    public StringColumn text(@NotNull String name, int length) {
        return new StringColumn(name, this, StringColumnType.Text, length);
    }

    @NotNull
    @Override
    public StringColumn varchar(@NotNull String name, int length) {
        return new StringColumn(name, this, StringColumnType.Varchar, length);
    }

    @NotNull
    @Override
    public StringColumn charColumn(@NotNull String name, int length) {
        return new StringColumn(name, this, StringColumnType.Char, length);
    }

    @NotNull
    @Override
    public StringColumn uuid(@NotNull String name) {
        return new StringColumn(name, this, StringColumnType.UUID);
    }

    @NotNull
    @Override
    public IntegerColumn integer(@NotNull String name) {
        return new IntegerColumn(name, this, IntegerColumnType.Int);
    }

    @NotNull
    @Override
    public IntegerColumn longColumn(@NotNull String name) {
        return new IntegerColumn(name, this, IntegerColumnType.Long);
    }

    @NotNull
    @Override
    public IntegerColumn smallInt(@NotNull String name) {
        return new IntegerColumn(name, this, IntegerColumnType.Small);
    }

    @NotNull
    @Override
    public IntegerColumn tinyInt(@NotNull String name) {
        return new IntegerColumn(name, this, IntegerColumnType.Tiny);
    }

    @NotNull
    @Override
    public FloatingColumn doubleColumn(@NotNull String name) {
        return new FloatingColumn(name, this, FloatingColumnType.Double);
    }

    @NotNull
    @Override
    public FloatingColumn floatColumn(@NotNull String name) {
        return new FloatingColumn(name, this, FloatingColumnType.Float);
    }

    @NotNull
    @Override
    public FloatingColumn numeric(@NotNull String name, int precision, int scale) {
        return new FloatingColumn(name, this, FloatingColumnType.Numeric, precision, scale);
    }

    @NotNull
    @Override
    public DateTimeColumn date(@NotNull String name) {
        return new DateTimeColumn(name, this, DateTimeColumnType.Date);
    }

    @NotNull
    @Override
    public DateTimeColumn datetime(@NotNull String name) {
        return new DateTimeColumn(name, this, DateTimeColumnType.DateTime);
    }

    @NotNull
    @Override
    public DateTimeColumn instant(@NotNull String name) {
        return new DateTimeColumn(name, this, DateTimeColumnType.Instant);
    }

    @NotNull
    @Override
    public DateTimeColumn offsetDateTime(@NotNull String name) {
        return new DateTimeColumn(name, this, DateTimeColumnType.OffsetDateTime);
    }

    @NotNull
    @Override
    public DateTimeColumn time(@NotNull String name) {
        return new DateTimeColumn(name, this, DateTimeColumnType.Time);
    }

    @NotNull
    @Override
    public BinaryColumn bytes(@NotNull String name, int length) {
        return new BinaryColumn(name, this, BinaryColumnType.Varying, length);
    }

    @NotNull
    @Override
    public BinaryColumn lob(@NotNull String name, int length) {
        return new BinaryColumn(name, this, BinaryColumnType.BLOB, length);
    }

    @NotNull
    @Override
    public ArbitraryColumn arbitrary(@NotNull String name, @NotNull String definition) {
        return new ArbitraryColumn(name, this, definition);
    }
}
