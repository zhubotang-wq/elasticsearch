/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.vThreadpool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.common.util.concurrent.ThreadContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.common.util.concurrent.EsThreadPoolExecutor.WORKER_PROBE;
import static org.elasticsearch.core.Strings.format;

public class VThreadPoolExecutor extends AbstractExecutorService {

    private static final Logger logger = LogManager.getLogger(VThreadPoolExecutor.class);
    private final ExecutorService virtualExecutorService;
    private final ThreadContext contextHolder;
    private final String name;

    public VThreadPoolExecutor(String name, ExecutorService virtualExecutorService, ThreadContext contextHolder) {
        this.virtualExecutorService = virtualExecutorService;
        this.contextHolder = contextHolder;
        this.name = name;
    }

    protected void beforeExecute(Thread t, Runnable r) {}

    protected void afterExecute(Runnable r, Throwable t) {
        rethrowErrors(ThreadContext.unwrap(r));
        assert assertDefaultContext(r);
    }

    @Override
    public void execute(Runnable command) {
        Runnable wrapped = () -> {
            beforeExecute(Thread.currentThread(), command);
            try {
                command.run();
            } catch (Throwable ex) {
                afterExecute(command, ex);
                throw ex;
            }
        };

        final Runnable wrappedRunnable = command != WORKER_PROBE ? contextHolder.preserveContext(wrapped) : WORKER_PROBE;
        try {
            virtualExecutorService.execute(wrapped);
        } catch (Exception e) {
            if (wrappedRunnable instanceof AbstractRunnable abstractRunnable) {
                try {
                    // If we are an abstract runnable we can handle the exception
                    // directly and don't need to rethrow it, but we log and assert
                    // any unexpected exception first.
                    if (e instanceof EsRejectedExecutionException == false) {
                        logException(abstractRunnable, e);
                    }
                    abstractRunnable.onRejection(e);
                } finally {
                    abstractRunnable.onAfter();
                }
            } else {
                throw e;
            }
        }
    }

    public static Throwable rethrowErrors(Runnable runnable) {
        if (runnable instanceof RunnableFuture<?> runnableFuture) {
            assert runnableFuture.isDone();
            try {
                runnableFuture.get();
            } catch (final Exception e) {
                /*
                 * In theory, Future#get can only throw a cancellation exception, an interrupted exception, or an execution
                 * exception. We want to ignore cancellation exceptions, restore the interrupt status on interrupted exceptions, and
                 * inspect the cause of an execution. We are going to be extra paranoid here though and completely unwrap the
                 * exception to ensure that there is not a buried error anywhere. We assume that a general exception has been
                 * handled by the executed task or the task submitter.
                 */
                assert e instanceof CancellationException || e instanceof InterruptedException || e instanceof ExecutionException : e;
                final Optional<Error> maybeError = ExceptionsHelper.maybeError(e);
                if (maybeError.isPresent()) {
                    // throw this error where it will propagate to the uncaught exception handler
                    throw maybeError.get();
                }
                if (e instanceof InterruptedException) {
                    // restore the interrupt status
                    Thread.currentThread().interrupt();
                }
                if (e instanceof ExecutionException) {
                    return e.getCause();
                }
            }
        }

        return null;
    }

    boolean assertDefaultContext(Runnable r) {
        assert contextHolder.isDefaultContext()
            : "the thread context is not the default context and the thread ["
                + Thread.currentThread().getName()
                + "] is being returned to the pool after executing ["
                + r
                + "]";
        return true;
    }

    void logException(AbstractRunnable r, Exception e) {
        logger.error(() -> format("[%s] unexpected exception when submitting task [%s] for execution", name, r), e);
        assert false : "executor throws an exception (not a rejected execution exception) before the task has been submitted " + e;
    }

    @Override
    public void shutdown() {
        virtualExecutorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return virtualExecutorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return virtualExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return virtualExecutorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return virtualExecutorService.awaitTermination(timeout, unit);
    }

    @Override
    public void close() {
        virtualExecutorService.close();
    }
}
