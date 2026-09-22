/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.fogofwar;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FogStateTest {

    @Test
    void newStateIsFullyUnexplored() {
        FogState state = new FogState((short) 1, 4, 4);
        assertFalse(state.isExplored(0, 0));
        assertFalse(state.isPerceived(0, 0));
    }

    @Test
    void outOfBoundsIsNeverExplored() {
        FogState state = new FogState((short) 1, 4, 4);
        assertFalse(state.isExplored(-1, 0));
        assertFalse(state.isExplored(4, 0));
        assertFalse(state.isExplored(0, 4));
    }

    @Test
    void setExploredTracksDirtyTilesAndCoalesces() {
        FogState state = new FogState((short) 1, 4, 4);
        FogOfWarRules.explore(state, 1, 1);
        FogOfWarRules.explore(state, 1, 1); // idempotent, should not re-dirty
        FogOfWarRules.explore(state, 2, 2);

        Set<Point> dirty = state.drainDirtyTiles();
        assertEquals(Set.of(new Point(1, 1), new Point(2, 2)), dirty);

        // Draining clears it
        assertEquals(Set.of(), state.drainDirtyTiles());
    }

    @Test
    void revealAllSetsEveryTileExplored() {
        FogState state = new FogState((short) 1, 3, 3);
        state.revealAll();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                assertTrue(state.isExplored(x, y));
            }
        }
    }

    @Test
    void clearAllUnexploresAndUnperceivesEverything() {
        FogState state = new FogState((short) 1, 3, 3);
        state.revealAll();
        FogOfWarRules.perceive(state, 0, 0);
        state.drainDirtyTiles();

        state.clearAll();
        assertFalse(state.isExplored(1, 1));
        assertFalse(state.isPerceived(0, 0));
        assertFalse(state.drainDirtyTiles().isEmpty());
    }

    @Test
    void fogEnabledDefaultsToTrue() {
        FogState state = new FogState((short) 1, 2, 2);
        assertTrue(state.isFogEnabled());
        state.setFogEnabled(false);
        assertFalse(state.isFogEnabled());
    }
}
