/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.asset.*;
import com.jme3.scene.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class AssetModelCoalescingTest {
    private static final class Assets extends DesktopAssetManager {
        final CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        final AtomicInteger loads=new AtomicInteger();
        volatile RuntimeException failure;
        Assets() { super(true); }
        @Override public Spatial loadModel(ModelKey key) {
            loads.incrementAndGet(); entered.countDown();
            try { if (!release.await(5,TimeUnit.SECONDS)) throw new AssertionError("Test loader release timeout"); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssetLoadException("Test loader interrupted",interrupted); }
            if (failure!=null) throw failure;
            return new Node("prepared");
        }
    }
    private static void awaitWaiting(Thread thread) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        while (thread.getState()!=Thread.State.WAITING && thread.isAlive() && System.nanoTime()<deadline)
            Thread.sleep(1);
        assertEquals(Thread.State.WAITING,thread.getState(),"Requester must join the in-flight future before source publication");
    }
    @Test void concurrentStrongAndWeakRequestsShareOnePreparationButOwnDifferentClones() throws Exception {
        var assets=new Assets(); var result=new AtomicReference<Spatial>(); var failure=new AtomicReference<Throwable>();
        try (var executor=Executors.newSingleThreadExecutor()) {
            var first=executor.submit(()->AssetUtils.loadModel(assets,"coalesce",null,true,false));
            assertTrue(assets.entered.await(2,TimeUnit.SECONDS));
            var waiter=new Thread(()->{ try { result.set(AssetUtils.loadModel(assets,"coalesce",null,true,true)); } catch(Throwable error) { failure.set(error); } });
            waiter.start();
            try {
                awaitWaiting(waiter); assertEquals(1,assets.loads.get());
                assertThrows(IllegalStateException.class,()->AssetUtils.releaseCaches(assets));
            } finally { assets.release.countDown(); waiter.join(3000); }
            var a=first.get(3,TimeUnit.SECONDS); assertFalse(waiter.isAlive()); assertNull(failure.get());
            assertNotSame(a,result.get()); a.setName("changed"); assertEquals("prepared",result.get().getName());
            AssetUtils.loadModel(assets,"coalesce",null,true,true);
            AssetUtils.loadModel(assets,"coalesce",null,true,false); assertEquals(1,assets.loads.get());
            AssetUtils.releaseCaches(assets);
        } finally { assets.release.countDown(); }
    }
    @Test void failureReachesAllWaitersAndRetryDoesNotReuseAPoisonedFuture() throws Exception {
        var assets=new Assets(); var expected=new IllegalArgumentException("invalid source"); assets.failure=expected;
        var observed=new AtomicReference<Throwable>();
        try (var executor=Executors.newSingleThreadExecutor()) {
            var first=executor.submit(()->AssetUtils.loadModel(assets,"coalesce-failure",null));
            assertTrue(assets.entered.await(2,TimeUnit.SECONDS));
            var waiter=new Thread(()->{ try { AssetUtils.loadModel(assets,"coalesce-failure",null); } catch(Throwable error) { observed.set(error); } });
            waiter.start();
            try { awaitWaiting(waiter); } finally { assets.release.countDown(); waiter.join(3000); }
            assertSame(expected,assertThrows(ExecutionException.class,()->first.get(3,TimeUnit.SECONDS)).getCause());
            assertSame(expected,observed.get()); assertFalse(waiter.isAlive()); assertEquals(1,assets.loads.get());
            assets.failure=null; assertEquals("prepared",AssetUtils.loadModel(assets,"coalesce-failure",null).getName());
            assertEquals(2,assets.loads.get()); AssetUtils.releaseCaches(assets);
        } finally { assets.release.countDown(); }
    }
    @Test void interruptedWaitRestoresTheFlagWithoutCancellingOtherOwners() throws Exception {
        var assets=new Assets(); var restored=new AtomicBoolean();
        try (var executor=Executors.newSingleThreadExecutor()) {
            var first=executor.submit(()->AssetUtils.loadModel(assets,"coalesce-interrupt",null));
            assertTrue(assets.entered.await(2,TimeUnit.SECONDS));
            var waiter=new Thread(()->{
                Thread.currentThread().interrupt();
                try { AssetUtils.loadModel(assets,"coalesce-interrupt",null); }
                catch (AssetLoadException expected) { restored.set(Thread.currentThread().isInterrupted()); }
            });
            try { waiter.start(); waiter.join(3000); assertFalse(waiter.isAlive()); assertTrue(restored.get()); }
            finally { assets.release.countDown(); }
            assertEquals("prepared",first.get(3,TimeUnit.SECONDS).getName()); assertEquals(1,assets.loads.get());
            AssetUtils.releaseCaches(assets);
        } finally { assets.release.countDown(); }
    }
    @Test void concurrentDependencyCyclesFailInsteadOfDeadlockingAndCanRetry() throws Exception {
        var entered=new CountDownLatch(2); var recursive=new AtomicBoolean(true);
        class CyclicAssets extends DesktopAssetManager {
            CyclicAssets() { super(true); }
            @Override public Spatial loadModel(ModelKey key) {
                if (!recursive.get()) return new Node("recovered");
                entered.countDown();
                try { if (!entered.await(2,TimeUnit.SECONDS)) throw new AssertionError("Both sources must start"); }
                catch (InterruptedException interrupted) { throw new AssetLoadException("Test interrupted",interrupted); }
                return AssetUtils.loadModel(this,key.getName().contains("cycle-a") ? "cycle-b" : "cycle-a",null);
            }
        }
        var assets=new CyclicAssets();
        try (var executor=Executors.newFixedThreadPool(2)) {
            try {
                var a=executor.submit(()->AssetUtils.loadModel(assets,"cycle-a",null));
                var b=executor.submit(()->AssetUtils.loadModel(assets,"cycle-b",null));
                assertInstanceOf(AssetLoadException.class,assertThrows(ExecutionException.class,()->a.get(3,TimeUnit.SECONDS)).getCause());
                assertInstanceOf(AssetLoadException.class,assertThrows(ExecutionException.class,()->b.get(3,TimeUnit.SECONDS)).getCause());
                recursive.set(false); assertEquals("recovered",AssetUtils.loadModel(assets,"cycle-a",null).getName());
                AssetUtils.releaseCaches(assets);
            } finally { executor.shutdownNow(); }
        }
    }

    @Test void recursiveSourceOnTheSameThreadFailsAndCanRetry() {
        class RecursiveAssets extends DesktopAssetManager {
            boolean recursive=true;
            RecursiveAssets() { super(true); }
            @Override public Spatial loadModel(ModelKey key) {
                return recursive ? AssetUtils.loadModel(this,"recursive-source",null) : new Node("recovered");
            }
        }
        var assets=new RecursiveAssets();
        assertThrows(AssetLoadException.class,()->AssetUtils.loadModel(assets,"recursive-source",null));
        assets.recursive=false; assertEquals("recovered",AssetUtils.loadModel(assets,"recursive-source",null).getName());
        AssetUtils.releaseCaches(assets);
    }
}
