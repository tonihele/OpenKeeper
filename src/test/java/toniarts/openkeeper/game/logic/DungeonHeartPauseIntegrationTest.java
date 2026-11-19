/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.DungeonHeartIntroduction;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.utils.GameLoop;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class DungeonHeartPauseIntegrationTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pauseWaitsForReconstructionOrDestructionBeforePublishing(boolean destroy) throws Exception {
        var data = new DefaultEntityData();
        EntityId room = data.createEntity();
        EntityId replacement = data.createEntity();
        data.setComponents(room, new RoomComponent((short) 5, false, new Point(1, 1)),
                new DungeonHeartIntroduction(null, data.createEntity(), data.createEntity(),
                        data.createEntity(), data.createEntity(), data.createEntity()));
        var construction = new DungeonHeartConstruction(data, 0, 4);
        var enteredTick = new CountDownLatch(1);
        var releaseTick = new CountDownLatch(1);
        IGameLogicUpdatable reconstruction = new IGameLogicUpdatable() {
            @Override public void start() { }
            @Override public void stop() { }
            @Override public void processTick(float tpf) {
                enteredTick.countDown();
                try {
                    if (!releaseTick.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Test did not release the active tick");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
                if (destroy) {
                    data.removeComponent(room, DungeonHeartIntroduction.class);
                } else {
                    data.setComponent(room, new DungeonHeartIntroduction(
                            data.getComponent(room, DungeonHeartIntroduction.class), replacement,
                            data.createEntity(), data.createEntity(), data.createEntity(), data.createEntity()));
                }
            }
        };
        var loop = new GameLoop(new GameLogicManager(construction, reconstruction), 1_000_000);
        try (var requester = Executors.newSingleThreadExecutor()) {
            loop.start();
            assertTrue(enteredTick.await(5, TimeUnit.SECONDS));
            var requestedPause = new CountDownLatch(1);
            var paused = requester.submit(() -> {
                requestedPause.countDown();
                loop.pause();
                construction.pause();
            });
            assertTrue(requestedPause.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> paused.get(50, TimeUnit.MILLISECONDS));
            releaseTick.countDown();
            paused.get(5, TimeUnit.SECONDS);
            var intro = data.getComponent(room, DungeonHeartIntroduction.class);
            if (destroy) {
                assertNull(intro, "Pause must not resurrect a removed introduction");
            } else {
                assertEquals(replacement, intro.heart);
                assertNotEquals(0, intro.pausedAt);
            }
        } finally {
            releaseTick.countDown();
            loop.stop();
            data.close();
        }
    }
}
