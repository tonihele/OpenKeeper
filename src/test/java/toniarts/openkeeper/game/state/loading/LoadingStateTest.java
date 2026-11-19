/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.state.loading;

import com.jme3.texture.Texture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.Main;
import static org.junit.jupiter.api.Assertions.*;

class LoadingStateTest {
    @Test void detachingBeforeInitializationCancelsTheWorkerWithoutCleanup() throws Exception {
        var constructor = Main.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var app = constructor.newInstance();
        var entered = new CountDownLatch(1);
        var returned = new CountDownLatch(1);
        var completed = new AtomicInteger();
        var cleaned = new AtomicInteger();
        var state = new LoadingState(app, "early-detach") {
            @Override protected Texture getLoadingScreenTexture() { return null; }
            @Override public void onLoad() {
                entered.countDown();
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException expected) { Thread.currentThread().interrupt(); }
                finally { returned.countDown(); }
            }
            @Override public void onLoadComplete() { completed.incrementAndGet(); }
            @Override public void cleanup() { cleaned.incrementAndGet(); super.cleanup(); }
        };
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertTrue(app.getStateManager().attach(state));
            assertFalse(state.isInitialized());
            assertTrue(app.getStateManager().detach(state));
            assertTrue(returned.await(2, TimeUnit.SECONDS), "Uninitialized state retained a loading thread");
            assertEquals(0, cleaned.get(), "jME skips cleanup for states still awaiting initialization");
            assertEquals(0, completed.get());
        } finally { state.stateDetached(app.getStateManager()); }
    }
}
