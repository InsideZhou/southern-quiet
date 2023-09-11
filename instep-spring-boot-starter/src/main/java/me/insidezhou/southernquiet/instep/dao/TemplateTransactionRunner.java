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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.sql.Connection;

public class TemplateTransactionRunner implements TransactionRunner {
    private final static SouthernQuietLogger logger = SouthernQuietLoggerFactory.getLogger(TemplateTransactionRunner.class);

    private final ThreadLocal<TemplateTransactionContext> transactionContextThreadLocal = new ThreadLocal<>();

    private final PlatformTransactionManager transactionManager;

    public TemplateTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public <R> R with(@Nullable Integer level, @NotNull Function1<? super TransactionContext, ? extends R> func) {
        TransactionStatus status = transactionManager.getTransaction(null);
        TemplateTransactionContext transactionContext = transactionContextThreadLocal.get();

        if (null == transactionContext) {
            transactionContext = new TemplateTransactionContext(null == level ? TransactionDefinition.ISOLATION_DEFAULT : level);
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

        try {
            return func.invoke(transactionContext);
        }
        catch (TransactionAbortException e) {
            transactionManager.rollback(status);

            if (null == e.getCause()) {
                return null;
            }
            else {
                throw e;
            }
        }
        catch (Exception e) {
            transactionManager.rollback(status);
            throw new TransactionAbortException(e);
        }
        finally {
            transactionManager.commit(status);

            if (status.isCompleted()) {
                transactionContextThreadLocal.set(null);
            }
        }
    }
}
