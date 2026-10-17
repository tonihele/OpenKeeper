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
 * Tests for the {@code PLAYER_COLOURED} rule of lighting_design.md &sect;6.1.
 */
public class PlayerColouredLightTest {

    private static LightSource whiteThingLight(Short owner) {
        LightRules rules = new LightRules();
        return rules.addThingLight(new Object(), Vector3f.ZERO, Vector3f.ZERO,
                255, 255, 255, false, false, false,
                true, LightConstants.getPlayerLightColour(owner),
                3f, 1.0f, false, false);
    }

    @Test
    public void redKeeperLightTakesTheSlotThreeColour() {
        // Slot 3 (red): (230,50,50) -- the floor of 32 is not hit here
        LightSource light = whiteThingLight((short) 3);
        assertEquals(230, light.getRed());
        assertEquals(50, light.getGreen());
        assertEquals(50, light.getBlue());
    }

    @Test
    public void channelsAreFlooredAtThirtyTwo() {
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
    public void absentOrUnknownPlayerFallsBackToSlotOneNotBlack() {
        assertArrayEquals(new int[]{191, 213, 224}, LightConstants.getPlayerLightColour(null));
        assertArrayEquals(new int[]{191, 213, 224}, LightConstants.getPlayerLightColour((short) 42));
        assertArrayEquals(new int[]{191, 213, 224}, LightConstants.getPlayerLightColour((short) -1));
    }

    @Test
    public void tableMatchesTheDesign() {
        assertArrayEquals(new int[]{0, 0, 0}, LightConstants.getPlayerLightColour((short) 0));
        assertArrayEquals(new int[]{255, 255, 255}, LightConstants.getPlayerLightColour((short) 2));
        assertArrayEquals(new int[]{50, 78, 230}, LightConstants.getPlayerLightColour((short) 4));
        assertArrayEquals(new int[]{230, 50, 212}, LightConstants.getPlayerLightColour((short) 7));
    }

    @Test
    public void returnedColourIsACopy() {
        int[] colour = LightConstants.getPlayerLightColour((short) 3);
        colour[0] = 0;
        assertEquals(230, LightConstants.getPlayerLightColour((short) 3)[0]);
    }

    @Test
    public void overBrightBitsApplyBeforeThePlayerColour() {
        LightRules rules = new LightRules();
        // COLOR_RED adds 256 first: (0 + 256) * 230 / 255 = 230
        LightSource light = rules.addThingLight(new Object(), Vector3f.ZERO, Vector3f.ZERO,
                0, 0, 0, true, false, false,
                true, LightConstants.getPlayerLightColour((short) 3),
                3f, 1.0f, false, false);
        assertEquals(230, light.getRed());
    }
}
