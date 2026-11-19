/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.state;

import com.jme3.app.AppTask;
import com.jme3.app.LegacyApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.view.text.GuardPostCounter;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import static org.junit.jupiter.api.Assertions.*;

class GameClientLoadingLifetimeTest {
    private static Main app() throws Exception {
        var constructor = Main.class.getDeclaredConstructor(); constructor.setAccessible(true);
        return constructor.newInstance();
    }
    @SuppressWarnings("unchecked")
    private static Queue<AppTask<?>> queue(Main app) throws Exception {
        var field = LegacyApplication.class.getDeclaredField("taskQueue"); field.setAccessible(true);
        return (Queue<AppTask<?>>) field.get(app);
    }
    private static void drain(Main app) throws Exception {
        var queue = queue(app);
        for (int count = 0; !queue.isEmpty(); count++) {
            assertTrue(count < 100, "Same-frame callback retry loop");
            var task = queue.remove(); task.invoke(); assertTrue(task.isDone());
            task.get(); // Pinned jME's timed get incorrectly rejects a completed null-valued Runnable.
        }
    }

    @Test void earlyDetachRemovesTheListenerAndDiscardsQueuedAndLateCallbacks() throws Exception {
        var app = app();
        var listener = new AtomicReference<GameSessionListener>();
        var removed = new AtomicInteger(); var exited = new AtomicInteger();
        var service = (GameSessionClientService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{GameSessionClientService.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "addGameSessionListener" -> listener.set((GameSessionListener) args[0]);
                        case "removeGameSessionListener" -> { assertSame(listener.get(), args[0]); removed.incrementAndGet(); }
                        case "exitGame" -> exited.incrementAndGet();
                        case "getEntityData" -> fail("Exited client accessed entity data");
                    }
                    return null;
                });
        var level = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> fail("Exited client accessed level data: " + method.getName()));
        var info = new ClientInfo(0, null, 0); info.setKeeper(new Keeper(false, (short) 3));
        var client = new GameClientState(level, (short) 3, List.of(info), service, app, null);
        app.getStateManager().attach(client);
        try {
            listener.get().onGameEnded(false);
            listener.get().onDoTransition((short) 1, new Vector3f());
            assertTrue(app.getStateManager().detach(client));
            client.whenLoadingStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
            listener.get().onGameDataLoaded(List.of());
            listener.get().onGameStarted();
            listener.get().onGamePaused();
            listener.get().onGoldChange((short) 3, 100);
            listener.get().onTilesChange(List.of());
            listener.get().onEffect(1, new Vector3f(), 0);
            listener.get().setPossession(null);
            drain(app);
            client.cleanup();
            assertEquals(1, removed.get()); assertEquals(1, exited.get());
        } finally { client.detach(); client.cleanup(); }
    }

    @Test void unavailableCameraRetriesAcrossUpdatesRatherThanRecursivelyDrainingTheQueue() throws Exception {
        var app = app();
        var listener = new AtomicReference<GameSessionListener>();
        var service = (GameSessionClientService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{GameSessionClientService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("addGameSessionListener")) listener.set((GameSessionListener) args[0]);
                    return null;
                });
        var level = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> null);
        var info = new ClientInfo(0, null, 0); info.setKeeper(new Keeper(false, (short) 3));
        var client = new GameClientState(level, (short) 3, List.of(info), service, app, null);
        try {
            var start = new Vector3f(1, 2, 3);
            listener.get().onDoTransition((short) 1, start); start.zero();
            drain(app);
            for (int i = 0; i < 5; i++) { client.update(1f / 60); drain(app); }
            var field = GameClientState.class.getDeclaredField("pendingCameraTransitions"); field.setAccessible(true);
            var pending = (Queue<?>) field.get(client);
            assertEquals(1, pending.size());
            var accessor = pending.element().getClass().getDeclaredMethod("start"); accessor.setAccessible(true);
            assertEquals(new Vector3f(1, 2, 3), accessor.invoke(pending.element()));
            client.detach();
            assertTrue(pending.isEmpty());
            client.whenLoadingStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
            drain(app);
        } finally { client.detach(); client.cleanup(); }
    }

    @Test void detachingDuringUpdateDisablesOwnedStatesInTheAlreadyCapturedSnapshot() throws Exception {
        var app = app(); var states = new AppStateManager(app);
        var service = (GameSessionClientService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{GameSessionClientService.class}, (p, m, a) -> null);
        var level = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (p, m, a) -> null);
        var info = new ClientInfo(0, null, 0); info.setKeeper(new Keeper(false, (short) 3));
        var client = new GameClientState(level, (short) 3, List.of(info), service, app, null);
        var data = new DefaultEntityData();
        try {
            var updates = new AtomicInteger();
            var queries = (EntityData) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityData.class},
                    (p, m, a) -> {
                        Object value = m.invoke(data, a);
                        if (!(value instanceof EntitySet set)) return value;
                        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntitySet.class}, (q, method, args) -> {
                            if (method.getName().equals("applyChanges")) updates.incrementAndGet();
                            return method.invoke(set, args);
                        });
                    });
            var counter = new GuardPostCounter(queries, (short) 3, id -> false);
            var field = GameClientState.class.getDeclaredField("guardPostCounter"); field.setAccessible(true); field.set(client, counter);
            states.attach(new AbstractAppState() {
                @Override public void update(float tpf) { client.detach(); }
            });
            states.attach(client); states.attach(counter);
            try {
                states.update(0); // Snapshot order: driver, client, counter. Detach occurs inside driver.update().
                assertFalse(counter.isEnabled());
                assertEquals(0, updates.get(), "Detached owner was still updated without its client");
                states.update(0); // Drain deferred cleanup without initializing a graphical loading screen.
                client.whenLoadingStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
                drain(app);
            } finally { client.detach(); client.cleanup(); counter.cleanup(); }
        } finally { data.close(); }
    }

    @Test void peerDependentPlayerStatesAreDisabledBeforeAnyPeerIsDetached() throws Exception {
        var states = new AppStateManager(app());
        var camera = new AbstractAppState() { };
        var calls = new AtomicInteger();
        var possession = new AbstractAppState() {
            @Override public void setEnabled(boolean enabled) {
                assertTrue(states.hasState(camera), "Camera detached before possession can disable it");
                calls.incrementAndGet(); super.setEnabled(enabled);
            }
        };
        var inactive = new AbstractAppState() {
            @Override public void setEnabled(boolean enabled) { fail("Redundant transition on inactive state"); }
            @Override public boolean isEnabled() { return false; }
        };
        var owned = new java.util.ArrayList<AbstractAppState>(List.of(camera, possession, inactive));
        owned.forEach(states::attach);
        PlayerState.detachPlayerStates(states, owned);
        assertEquals(1, calls.get()); assertTrue(owned.isEmpty());
        assertFalse(states.hasState(camera)); assertFalse(states.hasState(possession));
    }
}
