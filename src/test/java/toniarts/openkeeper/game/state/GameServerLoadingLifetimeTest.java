/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.state;

import com.jme3.app.AppTask;
import com.jme3.app.LegacyApplication;
import com.jme3.math.Vector2f;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.loading.LoadingScope;
import toniarts.openkeeper.game.state.loop.IGameLoopManager;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;
import static org.junit.jupiter.api.Assertions.*;

class GameServerLoadingLifetimeTest {
    private static final class Fixture {
        final Main app;
        final AtomicInteger starts = new AtomicInteger(), stops = new AtomicInteger(), sends = new AtomicInteger();
        final AtomicInteger mapListeners = new AtomicInteger(), worldListeners = new AtomicInteger(), players = new AtomicInteger();
        final AtomicInteger selections = new AtomicInteger();
        volatile GameSessionServiceListener listener;
        Runnable onStop = () -> { }, onSelect = () -> { }, onRemove = () -> { };
        final IMapController map = proxy(IMapController.class, (method, args) -> {
            if (method.equals("addListener")) mapListeners.incrementAndGet();
            if (method.equals("removeListener")) mapListeners.decrementAndGet();
            if (method.equals("selectTiles")) { selections.incrementAndGet(); onSelect.run(); }
            return null;
        });
        final IGameWorldController world = proxy(IGameWorldController.class, (method, args) -> {
            if (method.equals("getMapController")) return map;
            if (method.equals("addListener") || method.equals("addSpecialListener")) worldListeners.incrementAndGet();
            if (method.equals("removeListener") || method.equals("removeSpecialListener")) worldListeners.decrementAndGet();
            return null;
        });
        final IPlayerController player = proxy(IPlayerController.class, (method, args) -> {
            if (method.equals("addListener")) players.incrementAndGet();
            if (method.equals("removeListener")) players.decrementAndGet();
            return null;
        });
        final ILevelInfo level = proxy(ILevelInfo.class, (method, args) -> method.equals("getPlayers") ? Map.of() : null);
        final IGameController controller = proxy(IGameController.class, (method, args) -> switch (method) {
            case "getGameWorldController" -> world;
            case "getLevelInfo" -> level;
            case "getPlayerControllers" -> Map.of((short) 3, player);
            default -> null;
        });
        final IGameLoopManager game = new IGameLoopManager() {
            @Override public void start() { starts.incrementAndGet(); }
            @Override public void pause() { }
            @Override public void resume() { }
            @Override public void stop() { stops.incrementAndGet(); onStop.run(); }
            @Override public IGameController getGameController() { return controller; }
        };
        final GameSessionServerService service = proxy(GameSessionServerService.class, (method, args) -> {
            if (method.equals("addGameSessionServiceListener")) listener = (GameSessionServiceListener) args[0];
            if (method.equals("removeGameSessionServiceListener")) { listener = null; onRemove.run(); }
            if (method.equals("sendGameData")) sends.incrementAndGet();
            return null;
        });

        Fixture() throws Exception {
            var ctor = Main.class.getDeclaredConstructor(); ctor.setAccessible(true); app = ctor.newInstance();
        }

        GameServerState state(Supplier<? extends IGameLoopManager> factory) {
            return new GameServerState(null, List.of(new Keeper(false, (short) 3), new Keeper(false, (short) 4)),
                    false, service, null, factory);
        }

        void ready(GameServerState state) throws Exception {
            app.getStateManager().attach(state); state.initialize(app.getStateManager(), app);
            workerReturned(state); drain(app); assertEquals(1, sends.get());
        }
    }

    @Test void detachBeforeInitializationRetainsAnIgnoringDecoderUntilItsRealReturn() throws Exception {
        var fixture = new Fixture(); var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var state = fixture.state(() -> {
            entered.countDown();
            while (release.getCount() != 0) {
                try { release.await(); } catch (InterruptedException ignored) { }
            }
            return fixture.game;
        });
        fixture.app.getStateManager().attach(state);
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            fixture.app.getStateManager().detach(state);
            assertNull(fixture.listener); assertFalse(state.whenStopped().toCompletableFuture().isDone());
        } finally { release.countDown(); }
        state.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
        drain(fixture.app); state.cleanup();
        assertEquals(1, fixture.stops.get()); assertEquals(0, fixture.sends.get()); assertEquals(0, fixture.starts.get());
    }

    @Test void pendingStartNeverWaitsForLoadingAndDuplicateRequestsOnlyStartOnceAfterInitialization() throws Exception {
        var fixture = new Fixture(); var state = fixture.state(() -> fixture.game);
        var captured = fixture.listener;
        captured.onStartGame(); // Even before attachment: record intent, do not wait or dereference a null manager.
        fixture.app.getStateManager().attach(state); workerReturned(state); drain(fixture.app);
        assertEquals(0, fixture.starts.get());
        state.initialize(fixture.app.getStateManager(), fixture.app);
        captured.onStartGame(); captured.onStartGame(); drain(fixture.app);
        assertEquals(1, fixture.starts.get()); assertEquals(1, fixture.sends.get());
        state.detach(); state.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(0, fixture.mapListeners.get()); assertEquals(0, fixture.worldListeners.get()); assertEquals(0, fixture.players.get());
    }

    @Test void queuedReadinessAndStartsCannotAffectAClosedReplacement() throws Exception {
        var fixture = new Fixture(); var state = fixture.state(() -> fixture.game);
        var old = fixture.listener;
        fixture.app.getStateManager().attach(state); workerReturned(state); old.onStartGame();
        state.detach(); state.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS); drain(fixture.app);
        assertEquals(0, fixture.sends.get()); assertEquals(0, fixture.starts.get());
        old.onStartGame(); old.onSelectTiles(new Vector2f(), new Vector2f(), true, (short) 3); drain(fixture.app);
        assertEquals(0, fixture.selections.get()); assertEquals(1, fixture.stops.get());
    }

    @Test void retirementWaitsOffOwnerThreadForAnAdmittedCommandAndRejectsStaleCalls() throws Exception {
        var fixture = new Fixture(); var state = fixture.state(() -> fixture.game); fixture.ready(state);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        fixture.onSelect = () -> { entered.countDown(); await(release); };
        var captured = fixture.listener; var finished = new CompletableFuture<Void>();
        Thread.ofVirtual().start(() -> {
            try { captured.onSelectTiles(new Vector2f(), new Vector2f(), true, (short) 3); finished.complete(null); }
            catch (Throwable error) { finished.completeExceptionally(error); }
        });
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS)); state.detach();
            assertFalse(state.whenStopped().toCompletableFuture().isDone()); assertEquals(0, fixture.stops.get());
            captured.onSelectTiles(new Vector2f(), new Vector2f(), true, (short) 3);
            assertEquals(1, fixture.selections.get());
        } finally { release.countDown(); }
        finished.get(2, TimeUnit.SECONDS); state.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(1, fixture.stops.get()); assertEquals(0, fixture.mapListeners.get());
    }

    @Test void blockingOrFailingSimulationStopDoesNotBlockDetachOrSkipListenerRetirement() throws Exception {
        var fixture = new Fixture(); var state = fixture.state(() -> fixture.game); fixture.ready(state);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        fixture.onStop = () -> { entered.countDown(); await(release); throw new IllegalStateException("stop failure"); };
        try {
            state.detach(); assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertFalse(state.whenStopped().toCompletableFuture().isDone());
        } finally { release.countDown(); }
        var failure = assertThrows(ExecutionException.class, () -> state.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals(0, fixture.mapListeners.get()); assertEquals(0, fixture.worldListeners.get()); assertEquals(0, fixture.players.get());
        state.cleanup(); assertEquals(1, fixture.stops.get());
    }

    @Test void serviceUnsubscribeFailureIsReportedAfterSimulationStillRetires() throws Exception {
        var fixture = new Fixture(); var state = fixture.state(() -> fixture.game); fixture.ready(state);
        fixture.onRemove = () -> { throw new IllegalStateException("unsubscribe failure"); };
        state.detach();
        var failure = assertThrows(ExecutionException.class, () -> state.whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS));
        assertEquals("unsubscribe failure", failure.getCause().getMessage());
        assertEquals(1, fixture.stops.get()); assertEquals(0, fixture.mapListeners.get());
    }

    private static void workerReturned(GameServerState state) throws Exception {
        var field = GameServerState.class.getDeclaredField("loading"); field.setAccessible(true);
        ((LoadingScope) field.get(state)).whenStopped().toCompletableFuture().get(2, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(4, TimeUnit.SECONDS)) throw new AssertionError("Fixture release timed out"); }
        catch (InterruptedException error) { throw new AssertionError(error); }
    }

    private static void drain(Main app) throws Exception {
        var field = LegacyApplication.class.getDeclaredField("taskQueue"); field.setAccessible(true);
        @SuppressWarnings("unchecked") var tasks = (Queue<AppTask<?>>) field.get(app);
        while (!tasks.isEmpty()) { var task = tasks.remove(); task.invoke(); assertTrue(task.isDone()); task.get(); }
    }

    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> body) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, args) -> body.apply(method.getName(), args)));
    }
}
