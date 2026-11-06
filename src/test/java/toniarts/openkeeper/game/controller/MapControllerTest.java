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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MapControllerTest {

    private static final short ROCK = 2;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short NEUTRAL = Player.NEUTRAL_PLAYER_ID;

    @Test
    void finishingAWallFinishesTheOuterCornerItClosesWithItsDiagonalWall() {

        // The claimed path in the middle has reinforced walls left and right, rock above. The rock corners touch the
        // path only diagonally
        MapController mapController = mapController(TerrainFixtures.map(new short[][]{
            {ROCK, ROCK, ROCK},
            {REINFORCED_WALL, CLAIMED_PATH, REINFORCED_WALL},
            {ROCK, ROCK, ROCK}
        }, new short[][]{
            {NEUTRAL, NEUTRAL, NEUTRAL},
            {KEEPER, KEEPER, KEEPER},
            {NEUTRAL, NEUTRAL, NEUTRAL}
        }));

        mapController.healTile(new Point(1, 0), KEEPER, null);

        assertWall(mapController, 1, 0, REINFORCED_WALL, KEEPER);
        assertWall(mapController, 0, 0, REINFORCED_WALL, KEEPER);
        assertWall(mapController, 2, 0, REINFORCED_WALL, KEEPER);
        assertWall(mapController, 0, 2, ROCK, NEUTRAL);
        assertWall(mapController, 2, 2, ROCK, NEUTRAL);
    }

    private static void assertWall(MapController mapController, int x, int y, short terrainId, short ownerId) {
        IMapTileController tile = mapController.getMapData().getTile(x, y);
        assertEquals(terrainId, tile.getTerrainId(), "terrain at " + x + "," + y);
        assertEquals(ownerId, tile.getOwnerId(), "owner at " + x + "," + y);
    }

    private static MapController mapController(GameMap map) {
        Map<Short, Terrain> terrains = Map.of(
                ROCK, TerrainFixtures.terrain(ROCK, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.TAGGABLE),
                        0, 165, 660, REINFORCED_WALL),
                CLAIMED_PATH, TerrainFixtures.terrain(CLAIMED_PATH, TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE),
                        0, 660, 660, ROCK),
                REINFORCED_WALL, TerrainFixtures.terrain(REINFORCED_WALL, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID,
                        Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.TAGGABLE), 0, 900, 900, REINFORCED_WALL));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getMap" -> map;
            case "getTerrain" -> terrains.get((Short) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        Map<Short, Keeper> players = Map.of(NEUTRAL, new Keeper(false, NEUTRAL), KEEPER, new Keeper(false, KEEPER));
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getPlayers" -> players;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        return new MapController(kwdFile, null, Map.of(MiscType.REINFORCE_WALL_HEALTH,
                CreatureSleepFixtures.variable(MiscType.REINFORCE_WALL_HEALTH, 1000)), null, new DefaultEntityData(), levelInfo, null);
    }

}
