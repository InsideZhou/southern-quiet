package me.insidezhou.southernquiet.instep.dao;

import instep.dao.sql.TransactionAbortException;
import instep.dao.sql.TransactionContext;
import instep.dao.sql.TransactionRunner;
import kotlin.jvm.functions.Function1;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;

public class TemplateTransactionRunner implements TransactionRunner {
    private final static SouthernQuietLogger logger = SouthernQuietLoggerFactory.getLogger(TemplateTransactionRunner.class);

    private final ThreadLocal<TemplateTransactionContext> transactionContextThreadLocal = new ThreadLocal<>();

    private final PlatformTransactionManager transactionManager;

    public TemplateTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public <R> R run(@Nullable Integer level, @NotNull Function1<? super TransactionContext, ? extends R> func) {
        var transactionTemplate = new TransactionTemplate(transactionManager);
        if (null != level) {
            transactionTemplate.setIsolationLevel(level);
        }

        transactionTemplate.execute(status -> {
            var transactionContext = transactionContextThreadLocal.get();

            if (null == transactionContext) {
                transactionContext = new TemplateTransactionContext(transactionTemplate.getIsolationLevel());
            }
            else {
                if (null != level && level < transactionContext.getIsolationLevel()) {
                    logger.message("nested transaction isolation level is lesser than outer.")
                        .context("nested", level)
                        .context("outer", transactionContext.getIsolationLevel())
                        .warn();
                }
            }

            transactionContextThreadLocal.set(transactionContext);
            var sp = status.createSavepoint();

            try {
                var result = func.invoke(transactionContext);
                status.releaseSavepoint(sp);
                return result;
            }
            catch (TransactionAbortException e) {
                status.rollbackToSavepoint(sp);

                if (null == e.getCause()) {
                    return null;
                }
                else {
                    throw e;
                }
            }
            catch (Exception e) {
                status.rollbackToSavepoint(sp);
                throw new TransactionAbortException(e);
            }
            finally {
                if (!status.hasSavepoint()) {
                    transactionContextThreadLocal.set(null);
                }
            }
        });

        return null;
    }

    @Override
    public <R> R committed(@NotNull Function1<? super TransactionContext, ? extends R> func) {
        return run(Connection.TRANSACTION_READ_COMMITTED, func);
    }

    @Override
    public <R> R repeatable(@NotNull Function1<? super TransactionContext, ? extends R> func) {
        return run(Connection.TRANSACTION_REPEATABLE_READ, func);
    }

    @Override
    public <R> R run(@NotNull Function1<? super TransactionContext, ? extends R> func) {
        return run(null, func);
    }

    @Override
    public <R> R serializable(@NotNull Function1<? super TransactionContext, ? extends R> func) {
        return run(Connection.TRANSACTION_SERIALIZABLE, func);
    }

    @Override
    public <R> R uncommitted(@NotNull Function1<? super TransactionContext, ? extends R> func) {
        return run(Connection.TRANSACTION_READ_UNCOMMITTED, func);
    }
}
