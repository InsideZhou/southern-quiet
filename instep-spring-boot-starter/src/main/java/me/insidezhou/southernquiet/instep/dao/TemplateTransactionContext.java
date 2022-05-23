package me.insidezhou.southernquiet.instep.dao;

import instep.dao.sql.TransactionAbortException;
import instep.dao.sql.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TemplateTransactionContext implements TransactionContext {
    private final Map<String, Object> kvStore = new HashMap<>();

    private final int isolationLevel;

    public TemplateTransactionContext(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public int getIsolationLevel() {
        return isolationLevel;
    }

    @Override
    public void abort() {
        throw new TransactionAbortException(null);
    }

    @Override
    public void abort(@NotNull Exception e) {
        throw new TransactionAbortException(e);
    }

    @Nullable
    @Override
    public Object get(@NotNull String s) {
        return kvStore.get(s);
    }

    @Override
    public void set(@NotNull String s, @Nullable Object o) {
        kvStore.put(s, o);
    }
}
