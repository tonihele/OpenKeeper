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
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

/**
 * Who owns a wall once it is dug out
 */
class DugOutWallTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = Player.GOOD_PLAYER_ID;
    private static final short WALL_ID = 9;

    @Test
    void wallDugOutByAnEnemyBelongsToNobody() {

        // A wall falls to its destroyed type, left at 0 here, which the level gives as rock
        Terrain wall = TerrainFixtures.terrain(WALL_ID, flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.OWNABLE), 0, 10, 10, WALL_ID);
        Terrain rock = TerrainFixtures.terrain((short) 0, flags(Terrain.TerrainFlag.SOLID), 0, 100, 100, WALL_ID);
        GameMap map = new GameMap(1, 1);
        map.getTiles()[0][0] = new Tile() {

            @Override
            public short getTerrainId() {
                return WALL_ID;
            }

            @Override
            public short getPlayerId() {
                return KEEPER_ID;
            }
        };
        IKwdFile kwdFile = proxy(IKwdFile.class, (name, args) -> switch (name) {
            case "getMap" ->
                map;
            case "getTerrain" ->
                (short) args[0] == WALL_ID ? wall : rock;
            default ->
                throw new UnsupportedOperationException(name);
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (name, args) -> Map.of(KEEPER_ID, new Keeper(false, KEEPER_ID)));
        MapController mapController = new MapController(kwdFile, null,
                Map.of(MiscType.DIG_ENEMY_WALL_HEALTH, CreatureSleepFixtures.variable(MiscType.DIG_ENEMY_WALL_HEALTH, 100)),
                null, new DefaultEntityData(), levelInfo, proxy(GameEventService.class, (name, args) -> null));

        mapController.damageTile(new Point(0, 0), HERO_ID, null);

        IMapTileController tile = mapController.getMapData().getTile(0, 0);
        assertEquals(0, tile.getTerrainId());
        assertEquals(Player.NEUTRAL_PLAYER_ID, tile.getOwnerId());
    }

    @FunctionalInterface
    private interface Handler {

        Object handle(String name, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.handle(method.getName(), args)));
    }
}
