package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;


import instep.dao.sql.*;

@SuppressWarnings("unchecked")
public interface Column<T extends instep.dao.sql.Column<?>> {

    default ColumnCondition eq(Object value) {
        return ColumnExtensionsKt.eq((T) this, value);
    }

    default ColumnCondition notEQ(Object value) {
        return ColumnExtensionsKt.notEQ((T) this, value);
    }

    default ColumnCondition gt(Object value) {
        return ColumnExtensionsKt.gt((T) this, value);
    }

    default ColumnCondition lt(Object value) {
        return ColumnExtensionsKt.lt((T) this, value);
    }

    default ColumnCondition gte(Object value) {
        return ColumnExtensionsKt.gte((T) this, value);
    }

    default ColumnCondition lte(Object value) {
        return ColumnExtensionsKt.lte((T) this, value);
    }

    default ColumnCondition isNull() {
        return ColumnExtensionsKt.isNull((T) this);
    }

    default ColumnCondition notNull() {
        return ColumnExtensionsKt.notNull((T) this);
    }

    default ColumnCondition inArray(Object[] values) {
        return ColumnExtensionsKt.inArray((T) this, values);
    }

    default ColumnCondition inValues(Object... values) {
        return ColumnExtensionsKt.inArray((T) this, values);
    }

    default OrderBy asc(boolean nullsFirst) {
        return ColumnExtensionsKt.asc((T) this, nullsFirst);
    }

    default OrderBy asc() {
        return ColumnExtensionsKt.asc((T) this, false);
    }

    default OrderBy desc(boolean nullsFirst) {
        return ColumnExtensionsKt.desc((T) this, nullsFirst);
    }

    default OrderBy desc() {
        return ColumnExtensionsKt.desc((T) this, false);
    }

    default ColumnSelectExpression alias(String alias) {
        return ColumnExtensionsKt.alias((T) this, alias);
    }

    default ColumnSelectExpression alias() {
        return ColumnExtensionsKt.alias((T) this, "");
    }

    default SelectExpression count(String alias) {
        return ColumnExtensionsKt.count((T) this, alias);
    }

    default SelectExpression count() {
        return ColumnExtensionsKt.count((T) this, "");
    }
}
