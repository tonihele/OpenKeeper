/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.state.loading;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoadingScopeTest {
    @Test void cancelledQueuedAndLatePublicationsCannotRun() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var calls = new AtomicInteger();
        var scope = new LoadingScope(queue::add, failure -> fail(failure));
        assertTrue(scope.start("test-load", () -> scope.publish(calls::incrementAndGet)));
        scope.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(1, queue.size());
        scope.close();
        queue.remove().run();
        scope.publish(calls::incrementAndGet);
        assertEquals(0, calls.get());
        assertTrue(queue.isEmpty());
        assertFalse(scope.start("duplicate", calls::incrementAndGet));
    }

    @Test void closeDoesNotWaitOrPretendAnInterruptIgnoringDecoderHasStopped() throws Exception {
        var entered = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var scope = new LoadingScope(Runnable::run, failure -> fail(failure));
        try {
            scope.start("interrupt-ignoring-decoder", () -> {
                entered.countDown();
                while (release.getCount() != 0) {
                    try { release.await(); }
                    catch (InterruptedException expected) { interrupted.countDown(); }
                }
            });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var closeThread = new Thread(scope::close);
            closeThread.start(); closeThread.join(2000);
            assertFalse(closeThread.isAlive(), "Render cancellation blocked on decoding");
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
            assertFalse(scope.whenStopped().toCompletableFuture().isDone());
        } finally {
            release.countDown(); scope.close();
            scope.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
        }
    }

    @Test void duplicateNotificationsAndLoadFailuresHaveOneOwner() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var failures = new ArrayList<Throwable>();
        var expected = new IllegalStateException("decode failed");
        var scope = new LoadingScope(Runnable::run, failures::add);
        try {
            assertTrue(scope.start("failure", () -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException e) { throw new AssertionError(e); }
                throw expected;
            }));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertFalse(scope.start("duplicate", () -> fail("Duplicate loader")));
            release.countDown();
            scope.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertEquals(java.util.List.of(expected), failures);
        } finally { release.countDown(); scope.close(); }
    }

    @Test void publicationFailuresAreReportedAndEarlyClosePreventsStart() {
        var failures = new ArrayList<Throwable>();
        var scope = new LoadingScope(Runnable::run, failures::add);
        var expected = new IllegalArgumentException("publish failed");
        scope.publish(() -> { throw expected; });
        assertEquals(java.util.List.of(expected), failures);
        scope.close(); scope.close();
        assertTrue(scope.whenStopped().toCompletableFuture().isDone());
        assertFalse(scope.start("late", () -> fail("Started after exit")));
    }

    @Test void aQueuedFailureFromAnExitedSessionCannotAffectItsReplacement() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var scope = new LoadingScope(queue::add, failure -> fail("Stale failure reached a replacement session"));
        scope.start("failed-old-session", () -> { throw new IllegalStateException("old decode"); });
        scope.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(1, queue.size());
        scope.close(); queue.remove().run();
    }
}
