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
package toniarts.openkeeper.game.logic;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovementSettingsTest {

    @Test
    void defaultsExposeTheSharedSimulationAndPresentationTuning() {
        assertEquals(0.1f, MovementSettings.DEFAULT.waypointTolerance());
        assertEquals(20f, MovementSettings.DEFAULT.turnRate());
        assertEquals(0.1d, MovementSettings.DEFAULT.interpolationDelay());
        assertEquals(0.1d, MovementSettings.DEFAULT.maxExtrapolation());
    }

    @Test
    void collisionParametersCannotTunnelAcrossOrBackOutOfATile() {
        assertThrows(IllegalArgumentException.class, () -> new MovementSettings(
                0.1f, 0.1f, 20f, WorldUtils.TILE_WIDTH / 2f, WorldUtils.TILE_WIDTH / 4f,
                0.1d, 0.05d, WorldUtils.TILE_WIDTH * 2f));
        assertThrows(IllegalArgumentException.class, () -> new MovementSettings(
                0.1f, 0.1f, 20f, WorldUtils.TILE_WIDTH / 6f, WorldUtils.TILE_WIDTH,
                0.1d, 0.05d, WorldUtils.TILE_WIDTH * 2f));
    }
}
