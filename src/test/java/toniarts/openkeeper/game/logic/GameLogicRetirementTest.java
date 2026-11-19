/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.logic;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameLogicRetirementTest {
    @Test void everyPreparedSystemIsRetiredDespiteEarlierFailures() {
        var order = new ArrayList<Integer>();
        var first = new IllegalStateException("first"); var second = new AssertionError("second");
        var systems = new GameLogicManager(system(order, 1, first), system(order, 2, second), system(order, 3, null));
        assertSame(first, assertThrows(IllegalStateException.class, systems::stop));
        assertEquals(List.of(1, 2, 3), order); assertArrayEquals(new Throwable[]{second}, first.getSuppressed());
    }

    @Test void errorsRetainTheirTypeAndRepeatedSharedExceptionsDoNotSuppressThemselves() {
        var order = new ArrayList<Integer>(); var failure = new AssertionError("shared failure");
        assertSame(failure, assertThrows(AssertionError.class, () -> GameLogicManager.stopAll(
                system(order, 1, failure), system(order, 2, failure), system(order, 3, null))));
        assertEquals(List.of(1, 2, 3), order); assertEquals(0, failure.getSuppressed().length);
    }

    private static IGameLogicUpdatable system(List<Integer> order, int id, Throwable failure) {
        return new IGameLogicUpdatable() {
            @Override public void start() { throw new AssertionError("Prepared system started during retirement"); }
            @Override public void processTick(float tpf) { throw new AssertionError("Retired system ticked"); }
            @Override public void stop() {
                order.add(id);
                if (failure instanceof RuntimeException error) throw error;
                if (failure instanceof Error error) throw error;
            }
        };
    }
}
