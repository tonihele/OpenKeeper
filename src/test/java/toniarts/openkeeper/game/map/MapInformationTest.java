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

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.terrain;

/**
 * Which walls the imps may reinforce.
 */
class MapInformationTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short OTHER_KEEPER = Player.KEEPER2_ID;

    private static final short ROCK_ID = 2;
    private static final short CLAIMED_PATH_ID = 8;
    private static final short REINFORCED_WALL_ID = 9;

    @Test
    void reinforcesRockNextToOwnFloor() {
        assertTrue(isClaimableWall(ROCK_ID, Player.NEUTRAL_PLAYER_ID));
    }

    @Test
    void doesNotReinforceOverAnotherKeepersWall() {
        assertFalse(isClaimableWall(REINFORCED_WALL_ID, OTHER_KEEPER));
    }

    private static boolean isClaimableWall(short wallTerrainId, short wallOwnerId) {
        Map<Short, Terrain> terrains = Map.of(
                ROCK_ID, terrain(ROCK_ID, flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.TAGGABLE), 0, 165, 660, REINFORCED_WALL_ID),
                CLAIMED_PATH_ID, terrain(CLAIMED_PATH_ID, flags(Terrain.TerrainFlag.OWNABLE), 0, 660, 660, CLAIMED_PATH_ID),
                REINFORCED_WALL_ID, terrain(REINFORCED_WALL_ID, flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.TAGGABLE), 0, 900, 900, REINFORCED_WALL_ID));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> "getTerrain".equals(method.getName()) ? terrains.get((Short) args[0]) : null);

        // The wall and the keeper's claimed floor next to it
        FakeMapData mapData = new FakeMapData(2, 1);
        FakeMapTile wall = mapData.getTile(0, 0);
        wall.terrainId = wallTerrainId;
        wall.ownerId = wallOwnerId;
        FakeMapTile floor = mapData.getTile(1, 0);
        floor.terrainId = CLAIMED_PATH_ID;
        floor.ownerId = KEEPER;

        MapInformation<FakeMapData, FakeMapTile> mapInformation = new MapInformation<>(mapData, kwdFile,
                List.of(new Keeper(false, KEEPER), new Keeper(false, OTHER_KEEPER)));
        return mapInformation.isClaimableWall(new Point(0, 0), KEEPER);
    }

}
