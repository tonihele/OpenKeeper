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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.terrain;

class ClaimableRoomTest {

    private static final short CLAIMED_PATH = 8;
    private static final short LAIR = 11;
    private static final short DUNGEON_HEART = 14;
    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final short ABSENT_ENEMY = 5;

    private final Room heart = room((short) 5, 0);
    private final Room lair = room((short) 2, 0);
    private final FakeMapData mapData = new FakeMapData(2, 1);
    private final MapInformation<FakeMapData, FakeMapTile> mapInformation = new MapInformation<>(mapData, kwdFile(),
            List.of(new Keeper(false, KEEPER), new Keeper(false, ENEMY)));

    @Test
    void impsCannotClaimALivingEnemyDungeonHeart() {
        enemyTileNextToOurPath(DUNGEON_HEART, ENEMY);

        assertFalse(mapInformation.isClaimableRoom(new Point(1, 0), KEEPER));
    }

    @Test
    void impsCanClaimTheHeartOfAKeeperWhoIsNotInTheGame() {
        enemyTileNextToOurPath(DUNGEON_HEART, ABSENT_ENEMY);

        assertTrue(mapInformation.isClaimableRoom(new Point(1, 0), KEEPER));
    }

    @Test
    void impsCanClaimOtherEnemyRooms() {
        enemyTileNextToOurPath(LAIR, ENEMY);

        assertTrue(mapInformation.isClaimableRoom(new Point(1, 0), KEEPER));
    }

    private void enemyTileNextToOurPath(short terrainId, short ownerId) {
        mapData.getTile(0, 0).terrainId = CLAIMED_PATH;
        mapData.getTile(0, 0).ownerId = KEEPER;
        mapData.getTile(1, 0).terrainId = terrainId;
        mapData.getTile(1, 0).ownerId = ownerId;
    }

    private IKwdFile kwdFile() {
        Terrain path = terrain(CLAIMED_PATH, flags(Terrain.TerrainFlag.OWNABLE), 0, 0, 0, CLAIMED_PATH);
        Terrain lairTerrain = terrain(LAIR, flags(Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.ROOM, Terrain.TerrainFlag.ATTACKABLE), 0, 0, 0, LAIR);
        Terrain heartTerrain = terrain(DUNGEON_HEART, flags(Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.ROOM, Terrain.TerrainFlag.ATTACKABLE), 0, 0, 0, DUNGEON_HEART);
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrain" -> switch ((short) args[0]) {
                        case CLAIMED_PATH -> path;
                        case LAIR -> lairTerrain;
                        default -> heartTerrain;
                    };
                    case "getRoomByTerrain" -> (short) args[0] == DUNGEON_HEART ? heart : lair;
                    case "getDungeonHeart" -> heart;
                    default -> null;
                });
    }
}
