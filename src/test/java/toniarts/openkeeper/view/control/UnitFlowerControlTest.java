package toniarts.openkeeper.view.control;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitFlowerControlTest {

    @Test
    void fullHealthShowsAllSegments() {
        assertEquals(0, UnitFlowerControl.getHealthIndex(500, 500));
    }

    @Test
    void healthOnSegmentBoundaryIsNotRoundedUp() {

        // An imp has 500 health and takes 100 damage per slap
        assertEquals(1, UnitFlowerControl.getHealthIndex(400, 500));
        assertEquals(2, UnitFlowerControl.getHealthIndex(300, 500));
        assertEquals(3, UnitFlowerControl.getHealthIndex(200, 500));
        assertEquals(4, UnitFlowerControl.getHealthIndex(100, 500));
    }

    @Test
    void partialSegmentStillShowsTheSegment() {
        assertEquals(0, UnitFlowerControl.getHealthIndex(499, 500));
        assertEquals(4, UnitFlowerControl.getHealthIndex(1, 500));
    }

    @Test
    void noHealthShowsNoSegments() {
        assertEquals(5, UnitFlowerControl.getHealthIndex(0, 500));
        assertEquals(5, UnitFlowerControl.getHealthIndex(-10, 500));
    }

    @Test
    void healthAboveMaximumShowsAllSegments() {
        assertEquals(0, UnitFlowerControl.getHealthIndex(600, 500));
    }

    @Test
    void invalidMaximumShowsNoSegments() {
        assertEquals(5, UnitFlowerControl.getHealthIndex(100, 0));
    }

    @Test
    void creatureBeingRemovedHasNoPlayerColor() {

        // A removed creature has lost its owner, which reads as player 0
        assertFalse(UnitFlowerControl.hasPlayerColor((short) 0));
    }

    @Test
    void heroesNeutralAndKeepersHavePlayerColors() {
        for (short playerId : Utils.getPlayerIds()) {
            assertTrue(UnitFlowerControl.hasPlayerColor(playerId), "player " + playerId);
        }
    }

}
