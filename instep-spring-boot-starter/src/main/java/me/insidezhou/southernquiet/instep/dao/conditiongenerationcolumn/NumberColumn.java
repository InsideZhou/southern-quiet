package me.insidezhou.southernquiet.instep.dao.conditiongenerationcolumn;

import instep.dao.sql.ColumnExtensionsKt;
import instep.dao.sql.SelectExpression;

@SuppressWarnings("unchecked")
public interface NumberColumn<T extends instep.dao.sql.NumberColumn<?>> extends Column<T> {

    default SelectExpression sum(String alias) {
        return ColumnExtensionsKt.sum((T) this, alias);
    }

    default SelectExpression avg() {
        return ColumnExtensionsKt.avg((T) this, "");
    }

    default SelectExpression max(String alias) {
        return ColumnExtensionsKt.max((T) this, alias);
    }

    default SelectExpression min() {
        return ColumnExtensionsKt.min((T) this, "");
    }
}
