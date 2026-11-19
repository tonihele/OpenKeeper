/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.state.loading;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * One background load and its render-thread publications, owned by a single-use state.
 * Cancellation never joins the worker. Completion means the worker actually returned,
 * including when an asset decoder does not respond to interruption. Close and publication
 * run on the same render executor; workers may only prepare detached data.
 */
public final class LoadingScope implements AutoCloseable {
    private final Executor renderExecutor;
    private final Consumer<Throwable> failureHandler;
    private final CompletableFuture<Void> stopped = new CompletableFuture<>();
    private volatile boolean closed;
    private Thread worker;
    private boolean started;

    public LoadingScope(Executor renderExecutor, Consumer<Throwable> failureHandler) {
        this.renderExecutor = Objects.requireNonNull(renderExecutor);
        this.failureHandler = Objects.requireNonNull(failureHandler);
    }

    /** Duplicate session notifications cannot start a second loader. */
    public synchronized boolean start(String name, Runnable load) {
        Objects.requireNonNull(load);
        if (closed || started) return false;
        started = true;
        worker = new Thread(() -> {
            try {
                if (!closed) load.run();
            } catch (Throwable failure) {
                reportFailure(failure);
            } finally {
                synchronized (LoadingScope.this) { worker = null; }
                stopped.complete(null);
            }
        }, name);
        try {
            worker.start();
        } catch (RuntimeException | Error failure) {
            worker = null;
            stopped.complete(null);
            throw failure;
        }
        return true;
    }

    /** Check both before enqueue and at execution: the owner may exit while work is queued. */
    public void publish(Runnable publication) {
        Objects.requireNonNull(publication);
        if (closed) return;
        renderExecutor.execute(() -> {
            if (!closed) {
                try { publication.run(); }
                catch (Throwable failure) { if (!closed) failureHandler.accept(failure); }
            }
        });
    }

    public boolean isClosed() { return closed; }

    private void reportFailure(Throwable failure) {
        if (!closed) renderExecutor.execute(() -> {
            if (!closed) failureHandler.accept(failure);
        });
    }

    /** A read-only completion signal; unlike Future.cancel(), it never reports an active decoder as stopped. */
    public CompletionStage<Void> whenStopped() { return stopped.minimalCompletionStage(); }

    @Override public void close() {
        Thread active;
        synchronized (this) {
            if (closed) return;
            closed = true;
            active = worker;
        }
        if (active != null) active.interrupt();
        else stopped.complete(null);
    }
}
