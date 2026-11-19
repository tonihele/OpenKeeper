/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view;

import com.jme3.app.AppTask;
import com.jme3.app.LegacyApplication;
import com.jme3.scene.Node;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.loading.LoadingScope;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.view.text.GuardPostCounter;
import static org.junit.jupiter.api.Assertions.*;

class ViewLoadingLifetimeTest {
    private static final class Queries implements AutoCloseable {
        final DefaultEntityData delegate = new DefaultEntityData();
        final AtomicInteger live = new AtomicInteger();
        final EntityData data = (EntityData) Proxy.newProxyInstance(EntityData.class.getClassLoader(),
                new Class<?>[]{EntityData.class}, (proxy, method, args) -> {
                    Object value;
                    try { value = method.invoke(delegate, args); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                    if (!(value instanceof EntitySet set)) return value;
                    live.incrementAndGet();
                    var released = new AtomicBoolean();
                    return Proxy.newProxyInstance(EntitySet.class.getClassLoader(), new Class<?>[]{EntitySet.class},
                            (p, m, a) -> {
                                if (m.getName().equals("release")) {
                                    assertFalse(released.getAndSet(true), "Query released twice");
                                    live.decrementAndGet();
                                }
                                try { return m.invoke(set, a); }
                                catch (InvocationTargetException error) { throw error.getCause(); }
                            });
                });
        @Override public void close() { delegate.close(); }
    }
    private static Main app() throws Exception {
        var constructor = Main.class.getDeclaredConstructor(); constructor.setAccessible(true);
        return constructor.newInstance();
    }
    private static IKwdFile level() {
        var map = new GameMap(1, 1);
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (p, m, a) -> m.getName().equals("getMap") ? map : null);
    }
    private static PlayerMapViewState map(Main app, EntityData data) {
        return new PlayerMapViewState(app, level(), null, List.of(), data, (short) 3, false,
                () -> fail("Incomplete synthetic map reported ready"), id -> { }) {
            @Override protected void updateProgress(float progress) { }
        };
    }

    @Test void entityAndCounterQueriesAreReleasedWhenDetachedBeforeInitialization() throws Exception {
        var app = app();
        try (var queries = new Queries()) {
            var view = new PlayerEntityViewState(level(), null, queries.data, (short) 3, null, new Node());
            var counter = new GuardPostCounter(queries.data, (short) 3, id -> false);
            assertTrue(queries.live.get() >= 2);
            app.getStateManager().attach(view); app.getStateManager().attach(counter);
            app.getStateManager().detach(view); app.getStateManager().detach(counter);
            assertEquals(0, queries.live.get());
            view.cleanup(); counter.cleanup();
            assertEquals(0, queries.live.get());
        }
    }

    @Test void uninitializedMapRetiresAllConstructorOwnedSubscriptions() throws Exception {
        var app = app();
        try (var queries = new Queries()) {
            var view = map(app, queries.data);
            assertTrue(queries.live.get() >= 9);
            app.getStateManager().attach(view); app.getStateManager().detach(view);
            view.whenLoadingStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertEquals(0, queries.live.get());
            view.cleanup(); assertEquals(0, queries.live.get());
        }
    }

    @Test void cancelledDecoderKeepsItsSourcesUntilReturnAndCannotPublishAfterDetach() throws Exception {
        var app = app();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var queries = new Queries()) {
            var view = map(app, queries.data);
            var field = PlayerMapViewState.class.getDeclaredField("loading"); field.setAccessible(true);
            var scope = (LoadingScope) field.get(view);
            var target = new Node();
            scope.start("test-map-decoder", () -> {
                scope.publish(() -> target.attachChild(new Node("stale-map")));
                entered.countDown();
                while (release.getCount() != 0) {
                    try { release.await(); } catch (InterruptedException ignored) { }
                }
            });
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                app.getStateManager().attach(view); app.getStateManager().detach(view);
                assertFalse(view.whenLoadingStopped().toCompletableFuture().isDone());
                assertTrue(queries.live.get() >= 9, "Source queries released under an active decoder");
            } finally { release.countDown(); }
            view.whenLoadingStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertEquals(0, queries.live.get());
            var queueField = LegacyApplication.class.getDeclaredField("taskQueue"); queueField.setAccessible(true);
            @SuppressWarnings("unchecked") var queue = (Queue<AppTask<?>>) queueField.get(app);
            while (!queue.isEmpty()) { var task = queue.remove(); task.invoke(); assertTrue(task.isDone()); task.get(); }
            assertEquals(0, target.getQuantity());
        } finally { release.countDown(); }
    }
}
