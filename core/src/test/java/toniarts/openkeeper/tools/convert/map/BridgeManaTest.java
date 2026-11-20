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
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A bridge tile gives its owner 1 mana a second in DK II, other room tiles
 * give what the land under them gives
 */
class BridgeManaTest {

    private static final short WATER = 4;
    private static final short TREASURY = 10;
    private static final short STONE_BRIDGE = 26;

    @Test
    void bridgeOfTheLevelGivesItsOwnerMana() {
        var entities = new DefaultEntityData();
        try {
            var roomFlags = EnumSet.of(Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.ROOM);
            var water = TerrainFixtures.terrain(WATER, EnumSet.of(Terrain.TerrainFlag.WATER), 0, 100, 100, WATER);
            var treasuryTerrain = TerrainFixtures.terrain(TREASURY, roomFlags, 0, 100, 100, TREASURY);
            var bridgeTerrain = TerrainFixtures.terrain(STONE_BRIDGE, roomFlags, 0, 100, 100, STONE_BRIDGE);
            var treasury = RoomFixtures.room((short) 1, 0, Room.RoomFlag.PLACEABLE_ON_LAND);
            var bridge = RoomFixtures.room((short) 17, 0, Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA);
            short[] terrainIds = {WATER, STONE_BRIDGE, TREASURY};
            var map = new GameMap(3, 1);
            for (int x = 0; x < 3; x++) {
                short terrainId = terrainIds[x];
                map.getTiles()[x][0] = new Tile() {
                    @Override
                    public short getTerrainId() {
                        return terrainId;
                    }

                    @Override
                    public short getPlayerId() {
                        return Player.KEEPER1_ID;
                    }
                };
            }
            var kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                    (proxy, method, args) -> switch (method.getName()) {
                case "getMap" -> map;
                case "getTerrain" -> switch ((short) args[0]) {
                    case WATER -> water;
                    case TREASURY -> treasuryTerrain;
                    default -> bridgeTerrain;
                };
                case "getRoomByTerrain" -> switch ((short) args[0]) {
                    case TREASURY -> treasury;
                    case STONE_BRIDGE -> bridge;
                    default -> null;
                };
                default -> null;
            });

            var mapData = new MapData(kwdFile, entities, List.of(new Keeper(false, Player.KEEPER1_ID)));

            assertEquals(0, manaGain(entities, mapData, 0));
            assertEquals(1, manaGain(entities, mapData, 1));
            assertEquals(0, manaGain(entities, mapData, 2));
        } finally {
            entities.close();
        }
    }

    private static int manaGain(DefaultEntityData entities, MapData mapData, int x) {
        return entities.getComponent(mapData.getTile(new Point(x, 0)).getEntityId(), Mana.class).manaGeneration;
    }
}
