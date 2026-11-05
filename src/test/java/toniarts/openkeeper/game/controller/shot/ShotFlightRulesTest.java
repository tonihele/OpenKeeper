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
package toniarts.openkeeper.game.controller.shot;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotFlightRulesTest {

    private static final float DELTA = 0.0001f;

    @Test
    void fireballFlies() {
        assertTrue(ShotFlightRules.isFlying(ShotFixtures.fireball()));
    }

    @Test
    void thunderboltStrikesAtOnce() {
        assertFalse(ShotFlightRules.isFlying(ShotFixtures.thunderbolt()));
    }

    @Test
    void aimedShotFliesItsSpeedTowardsTheTarget() {
        Vector3f velocity = ShotFlightRules.aim(new Vector3f(1, 1, 1), new Vector3f(4, 1, 5), 0.9f);

        assertEquals(0.9f, velocity.length(), DELTA);
        assertEquals(0.54f, velocity.x, DELTA);
        assertEquals(0.72f, velocity.z, DELTA);
    }

    @Test
    void shotAimedAtItselfStaysPut() {
        assertEquals(Vector3f.ZERO, ShotFlightRules.aim(new Vector3f(1, 1, 1), new Vector3f(1, 1, 1), 0.9f));
    }

    @Test
    void moveIsSplitIntoQuarterTiles() {
        assertEquals(1, ShotFlightRules.getSteps(new Vector3f(0.2f, 0, 0)));
        assertEquals(4, ShotFlightRules.getSteps(new Vector3f(0.9f, 0, 0)));
        assertEquals(16, ShotFlightRules.getSteps(new Vector3f(10, 0, 0)));
    }

    @Test
    void shotTouchesCreatureWithinBothWidths() {
        Vector3f creature = new Vector3f(2, 1, 2);

        // Fireball radius 0.2 and an Imp 0.14 wide reach 0.27 to the side
        assertTrue(ShotFlightRules.touches(new Vector3f(2.26f, 1.3f, 2), 0.2f, creature, 0.14f, 0.5f));
        assertFalse(ShotFlightRules.touches(new Vector3f(2.28f, 1.3f, 2), 0.2f, creature, 0.14f, 0.5f));
    }

    @Test
    void shotFlyingOverTheCreatureMissesIt() {
        assertFalse(ShotFlightRules.touches(new Vector3f(2, 2, 2), 0.2f, new Vector3f(2, 1, 2), 0.14f, 0.5f));
    }

    @Test
    void shotIsOutOfRangeOnlyOverTheFloor() {
        Vector3f start = new Vector3f(1, 1, 1);

        assertFalse(ShotFlightRules.isOutOfRange(start, new Vector3f(4, 3, 1), 4));
        assertTrue(ShotFlightRules.isOutOfRange(start, new Vector3f(5.1f, 1, 1), 4));
        assertFalse(ShotFlightRules.isOutOfRange(start, new Vector3f(50, 1, 1), 0));
    }

}
