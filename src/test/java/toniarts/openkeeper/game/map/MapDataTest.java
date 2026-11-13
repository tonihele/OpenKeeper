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

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

class MapDataTest {

    private static final short KEEPER_ID = 3;
    private static final short ROCK_ID = 2;
    private static final short CLAIMED_PATH_ID = 8;

    @Test
    void terrainThatCannotBeOwnedIsNeutralAtLoad() {
        Terrain rock = TerrainFixtures.terrain(ROCK_ID, flags(Terrain.TerrainFlag.SOLID), 0, 100, 100, (short) 9);
        Terrain claimedPath = TerrainFixtures.terrain(CLAIMED_PATH_ID, flags(Terrain.TerrainFlag.OWNABLE), 0, 100, 100, CLAIMED_PATH_ID);
        GameMap map = new GameMap(2, 1);
        map.getTiles()[0][0] = tile(ROCK_ID, KEEPER_ID);
        map.getTiles()[1][0] = tile(CLAIMED_PATH_ID, KEEPER_ID);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getMap" ->
                map;
            case "getTerrain" ->
                (short) args[0] == ROCK_ID ? rock : claimedPath;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });

        MapData mapData = new MapData(kwdFile, new DefaultEntityData(), List.of(new Keeper(false, KEEPER_ID)));

        assertEquals(Player.NEUTRAL_PLAYER_ID, mapData.getTile(0, 0).getOwnerId());
        assertEquals(KEEPER_ID, mapData.getTile(1, 0).getOwnerId());
    }

    private static Tile tile(short terrainId, short playerId) {
        return new Tile() {

            @Override
            public short getTerrainId() {
                return terrainId;
            }

            @Override
            public short getPlayerId() {
                return playerId;
            }
        };
    }
}
