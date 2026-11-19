/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.state.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.utils.GameLoop;
import static org.junit.jupiter.api.Assertions.*;

class GameLoopRetirementTest {
    @Test void cleanupFailureStillRetiresEveryOtherLoopAndReleasesTheirReferences() {
        var stopped = new AtomicInteger();
        var loops = new ArrayList<>(List.of(loop(stopped, true), loop(stopped, false), loop(stopped, true)));
        var failure = assertThrows(CompletionException.class, () -> GameLoopManager.stopLoops(loops));
        assertEquals(3, stopped.get()); assertTrue(loops.isEmpty());
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals(1, failure.getSuppressed().length);
        GameLoopManager.stopLoops(loops); assertEquals(3, stopped.get());
    }

    private static GameLoop loop(AtomicInteger stopped, boolean fail) {
        return new GameLoop(new IGameLogicUpdatable() {
            @Override public void start() { throw new AssertionError("Retired prepared loop started"); }
            @Override public void processTick(float tpf) { throw new AssertionError("Retired loop ticked"); }
            @Override public void stop() {
                stopped.incrementAndGet();
                if (fail) throw new IllegalStateException("cleanup failure");
            }
        }, 10_000_000);
    }
}
