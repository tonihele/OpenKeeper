/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class AssetPrewarmLifecycleTest {
    private static class Assets extends DesktopAssetManager {
        final AtomicInteger loads = new AtomicInteger();
        Runnable onLoad = () -> { };
        Assets() { super(true); }
        @Override public Spatial loadModel(ModelKey key) {
            loads.incrementAndGet(); onLoad.run(); return new Node(key.getName());
        }
    }
    private static IKwdFile level(String... names) {
        var creature = new Creature();
        for (int i = 0; i < names.length; i++) {
            var art = new ArtResource(names[i], ArtResource.ArtResourceType.ANIMATING_MESH);
            art.getFlags().add(ArtResource.ArtResourceFlag.PRELOAD);
            creature.getAnimations().put(Creature.AnimationType.values()[i], art);
        }
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getCreatureList") ? List.of(creature) : List.of());
    }

    @Test void cancelledBeforePreparationDoesNoWorkAndReleasesOwnership() {
        var assets = new Assets();
        var result = AssetUtils.prewarmAssets(level("cancelled"), assets, () -> false,
                work -> fail("Queued cancelled prewarm"), model -> fail("Uploaded cancelled prewarm"));
        assertFalse(result.toCompletableFuture().join());
        assertEquals(0, assets.loads.get());
        AssetUtils.releaseCaches(assets);
    }

    @Test void cancellationBetweenPreparationAndUploadCannotPoisonTheReadyFlag() {
        var assets = new Assets();
        var active = new AtomicBoolean(true);
        var queue = new LinkedBlockingQueue<Runnable>();
        var uploaded = new ArrayList<String>();
        var result = AssetUtils.prewarmAssets(level("walk"), assets, active::get,
                queue::add, model -> uploaded.add(model.getName()));
        assertFalse(result.toCompletableFuture().isDone());
        assertThrows(IllegalStateException.class, () -> AssetUtils.releaseCaches(assets));
        active.set(false); queue.remove().run();
        assertFalse(result.toCompletableFuture().join());
        assertTrue(uploaded.isEmpty());
        active.set(true);
        var retry = AssetUtils.prewarmAssets(level("walk"), assets, active::get,
                queue::add, model -> uploaded.add(model.getName()));
        queue.remove().run();
        assertTrue(retry.toCompletableFuture().join());
        assertEquals(1, uploaded.size());
        assertEquals(1, assets.loads.get(), "Retry should reuse completed CPU preparation");
        assertTrue(AssetUtils.prewarmAssets(level("walk"), assets, active::get, queue::add,
                model -> fail("Repeated completed upload")).toCompletableFuture().join());
        assertTrue(queue.isEmpty());
        AssetUtils.releaseCaches(assets);
    }

    @Test void cancellationDuringDecodingStopsTheRemainingCatalog() {
        var assets = new Assets();
        var active = new AtomicBoolean(true);
        assets.onLoad = () -> {
            assertThrows(IllegalStateException.class, () -> AssetUtils.releaseCaches(assets));
            active.set(false);
        };
        var result = AssetUtils.prewarmAssets(level("one", "two", "three"), assets, active::get,
                task -> fail("Queued an interrupted catalog"), model -> fail("Uploaded an interrupted catalog"));
        assertFalse(result.toCompletableFuture().join());
        assertEquals(1, assets.loads.get());
        AssetUtils.releaseCaches(assets);
    }

    @Test void uploadFailureAndExecutorRejectionReleaseTheContextAndAllowRetry() {
        var assets = new Assets();
        var expected = new IllegalStateException("renderer failure");
        var failed = AssetUtils.prewarmAssets(level("retry"), assets, () -> true, Runnable::run,
                model -> { throw expected; });
        assertSame(expected, assertThrows(CompletionException.class,
                () -> failed.toCompletableFuture().join()).getCause());
        var rejected = AssetUtils.prewarmAssets(level("retry"), assets, () -> true,
                work -> { throw new RejectedExecutionException("renderer closed"); }, model -> fail("Rejected upload"));
        assertInstanceOf(RejectedExecutionException.class, assertThrows(CompletionException.class,
                () -> rejected.toCompletableFuture().join()).getCause());
        var uploaded = new AtomicInteger();
        var retry = AssetUtils.prewarmAssets(level("retry"), assets, () -> true, Runnable::run,
                model -> uploaded.incrementAndGet());
        // Completion callbacks may establish the next root boundary immediately.
        retry.thenRun(() -> AssetUtils.releaseCaches(assets)).toCompletableFuture().join();
        assertTrue(retry.toCompletableFuture().join());
        assertEquals(1, uploaded.get());
    }

    @Test void oneAssetManagersDecoderCannotLockAnotherManagersPrewarm() throws Exception {
        var a = new Assets(); var b = new Assets();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        a.onLoad = () -> {
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        };
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> AssetUtils.prewarmAssets(level("a"), a, () -> true,
                    Runnable::run, model -> { }).toCompletableFuture().join());
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var second = workers.submit(() -> AssetUtils.prewarmAssets(level("b"), b, () -> true,
                    Runnable::run, model -> { }).toCompletableFuture().join());
            assertTrue(second.get(2, TimeUnit.SECONDS));
            assertFalse(first.isDone());
            release.countDown(); assertTrue(first.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS));
        }
        AssetUtils.releaseCaches(a); AssetUtils.releaseCaches(b);
    }
}
