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
package toniarts.openkeeper.game.map;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.terrain;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.withMaxManaGain;

/**
 * The terrains as in the shipped DK II data. Rooms can't be built on a claimed
 * Mana Vault (#576)
 */
class BuildableLandTest {

    @Test
    void roomsCanBeBuiltOnClaimedPath() {
        Terrain claimedPath = withMaxManaGain(terrain((short) 8, flags(TerrainFlag.OWNABLE, TerrainFlag.ATTACKABLE,
                TerrainFlag.PLAYER_COLOURED_PATH, TerrainFlag.CONSTRUCTION_TYPE_QUAD, TerrainFlag.FILL_INABLE,
                TerrainFlag.TERRAIN_LIGHT), 0, 0, 0, (short) 0), 0);

        assertTrue(MapInformation.isBuildableLand(claimedPath));
    }

    @Test
    void roomsCantBeBuiltOnClaimedManaVault() {
        Terrain claimedVault = withMaxManaGain(terrain((short) 32, flags(TerrainFlag.OWNABLE, TerrainFlag.ATTACKABLE,
                TerrainFlag.FILL_INABLE, TerrainFlag.TERRAIN_LIGHT), 0, 0, 0, (short) 0), 100);

        assertFalse(MapInformation.isBuildableLand(claimedVault));
    }

    @Test
    void roomsCantBeBuiltOnWallsOrRooms() {
        Terrain wall = terrain((short) 4, flags(TerrainFlag.SOLID, TerrainFlag.OWNABLE), 0, 0, 0, (short) 0);
        Terrain room = terrain((short) 14, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM), 0, 0, 0, (short) 0);

        assertFalse(MapInformation.isBuildableLand(wall));
        assertFalse(MapInformation.isBuildableLand(room));
    }

}
