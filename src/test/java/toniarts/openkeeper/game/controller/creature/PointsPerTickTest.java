/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.controller.creature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II turns a creature's points per second into points per tick when the
 * level loads, and scales those by the creature's level
 */
class PointsPerTickTest {

    @Test
    void perSecondValueIsDividedByTheTicksPerSecond() {
        assertEquals(1, CreatureController.getPointsPerTick(5, 4, 100)); // Warlock
        assertEquals(2, CreatureController.getPointsPerTick(10, 4, 100)); // Wizard
        assertEquals(4, CreatureController.getPointsPerTick(18, 4, 100)); // Unique Warlock
    }

    @Test
    void anyPointsMakeAtLeastOneATick() {
        assertEquals(1, CreatureController.getPointsPerTick(1, 4, 100)); // Skeleton
        assertEquals(1, CreatureController.getPointsPerTick(3, 4, 100)); // Monk
    }

    @Test
    void noPointsMakeNone() {
        assertEquals(0, CreatureController.getPointsPerTick(0, 4, 100));
    }

    @Test
    void levelScalesThePointsPerTick() {

        // The Warlock's 1 a tick at the levels 2 (125 %), 4 (250 %) and 10 (700 %)
        assertEquals(1, CreatureController.getPointsPerTick(5, 4, 125));
        assertEquals(2, CreatureController.getPointsPerTick(5, 4, 250));
        assertEquals(7, CreatureController.getPointsPerTick(5, 4, 700));
    }

}
