/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.lighting;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the {@code PLAYER_COLOURED} rule.
 */
class PlayerColouredLightTest {

    private static LightSource whiteThingLight(Short owner) {
        LightRules rules = new LightRules();
        return rules.addThingLight(new Object(), Vector3f.ZERO, Vector3f.ZERO,
                255, 255, 255, false, false, false,
                true, LightConstants.getPlayerLightColour(owner),
                3f, 1.0f, false, false);
    }

    @Test
    void redKeeperLightTakesTheSlotThreeColour() {
        // Slot 3 (red): (230,50,50) -- the floor of 32 is not hit here
        LightSource light = whiteThingLight((short) 3);
        assertEquals(230, light.getRed());
        assertEquals(50, light.getGreen());
        assertEquals(50, light.getBlue());
    }

    @Test
    void channelsAreFlooredAtThirtyTwo() {
        LightRules rules = new LightRules();
        // A dim base colour times a dark player channel falls under 32
        LightSource light = rules.addThingLight(new Object(), Vector3f.ZERO, Vector3f.ZERO,
                100, 100, 100, false, false, false,
                true, LightConstants.getPlayerLightColour((short) 3),
                3f, 1.0f, false, false);
        assertEquals(90, light.getRed());   // 100 * 230 / 255
        assertEquals(32, light.getGreen()); // 100 * 50 / 255 = 19 -> floored
        assertEquals(32, light.getBlue());
    }

    @Test
    void absentOrUnknownPlayerFallsBackToSlotOneNotBlack() {
        assertArrayEquals(new int[]{191, 213, 224}, LightConstants.getPlayerLightColour(null));
        assertArrayEquals(new int[]{191, 213, 224}, LightConstants.getPlayerLightColour((short) 42));
        assertArrayEquals(new int[]{191, 213, 224}, LightConstants.getPlayerLightColour((short) -1));
    }

    @Test
    void tableHasTheExpectedColours() {
        assertArrayEquals(new int[]{0, 0, 0}, LightConstants.getPlayerLightColour((short) 0));
        assertArrayEquals(new int[]{255, 255, 255}, LightConstants.getPlayerLightColour((short) 2));
        assertArrayEquals(new int[]{50, 78, 230}, LightConstants.getPlayerLightColour((short) 4));
        assertArrayEquals(new int[]{230, 50, 212}, LightConstants.getPlayerLightColour((short) 7));
    }

    @Test
    void lightOffsetHeightLandsOnJmeUpAxis() {
        // The imp's light block is (0, 0, 0.5): half a tile UP, not sideways
        Vector3f offset = LightConstants.toJmeOffset(new javax.vecmath.Vector3f(0f, 0f, 0.5f));
        assertEquals(0f, offset.x);
        assertEquals(0.5f, offset.y);
        assertEquals(0f, offset.z);

        // And the map's y goes to jME's z
        Vector3f other = LightConstants.toJmeOffset(new javax.vecmath.Vector3f(1f, 2f, 3f));
        assertEquals(new Vector3f(1f, 3f, 2f), other);
    }

    @Test
    void playerColouredLightsHaveASmallRadius() {
        // The imp's data radius is 2 tiles (x 2/3 = 1.33): capped for player-coloured lights
        LightRules rules = new LightRules();
        LightSource coloured = rules.addThingLight(new Object(), Vector3f.ZERO, Vector3f.ZERO,
                255, 255, 255, false, false, false,
                true, LightConstants.getPlayerLightColour((short) 3), 2f, 1.0f, false, false);
        assertEquals(LightConstants.PLAYER_COLOURED_LIGHT_MAX_RADIUS_TILES, coloured.getRadius());

        // Other thing lights are untouched
        LightSource plain = rules.addThingLight(new Object(), Vector3f.ZERO, Vector3f.ZERO,
                255, 200, 100, false, false, false,
                false, null, 2f, 1.0f, false, false);
        assertEquals(2f * LightConstants.THING_LIGHT_RADIUS_SCALE, plain.getRadius());
    }

    @Test
    void returnedColourIsACopy() {
        int[] colour = LightConstants.getPlayerLightColour((short) 3);
        colour[0] = 0;
        assertEquals(230, LightConstants.getPlayerLightColour((short) 3)[0]);
    }

    @Test
    void overBrightBitsApplyBeforeThePlayerColour() {
        LightRules rules = new LightRules();
        // COLOR_RED adds 256 first: (0 + 256) * 230 / 255 = 230
        LightSource light = rules.addThingLight(new Object(), Vector3f.ZERO, Vector3f.ZERO,
                0, 0, 0, true, false, false,
                true, LightConstants.getPlayerLightColour((short) 3),
                3f, 1.0f, false, false);
        assertEquals(230, light.getRed());
    }
}
