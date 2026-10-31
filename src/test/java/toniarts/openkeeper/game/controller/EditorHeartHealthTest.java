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

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.ImpGenerator;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Dungeon Heart set to 0 % in the level editor starts destroyed, like Keeper
 * Dante's heart on level8. One set to anything above that is a working heart.
 */
class EditorHeartHealthTest {

    private static final short ROCK = 2;
    private static final short HEART_TERRAIN = 14;
    private static final short KEEPER = Player.KEEPER2_ID;

    @ParameterizedTest
    @CsvSource({"0, true", "1, false", "100, false"})
    void heartAtZeroPercentStartsDestroyed(int initialHealth, boolean destroyed) {
        EntityData entityData = new DefaultEntityData();
        IRoomController heart = loadHeart(entityData, initialHealth);

        assertEquals(destroyed, heart.isDestroyed());
        assertEquals(!destroyed, entityData.getComponent(heart.getEntityId(), ImpGenerator.class) != null);
    }

    private static IRoomController loadHeart(EntityData entityData, int initialHealth) {
        Room room = RoomFixtures.room("Dungeon Heart", Room.TileConstruction._5_BY_5_ROTATED);
        Map<Short, Terrain> terrains = Map.of(
                ROCK, TerrainFixtures.terrain(ROCK, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 100, 100, ROCK),
                HEART_TERRAIN, TerrainFixtures.terrain(HEART_TERRAIN, TerrainFixtures.flags(Terrain.TerrainFlag.ROOM,
                        Terrain.TerrainFlag.OWNABLE), 0, 100, 100, HEART_TERRAIN));

        // A 5 x 5 heart in the middle of rock
        GameMap map = new GameMap(7, 7);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                boolean heart = x > 0 && x < 6 && y > 0 && y < 6;
                Tile tile = TerrainFixtures.tile(heart ? HEART_TERRAIN : ROCK, heart ? KEEPER : Player.NEUTRAL_PLAYER_ID);
                map.getTiles()[x][y] = tile;
            }
        }
        Thing.Room thing = RoomFixtures.roomThing(initialHealth, 3, 3);

        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getMap" -> map;
            case "getTerrain" -> terrains.get((Short) args[0]);
            case "getRoomByTerrain", "getDungeonHeart" -> room;
            case "getThings" -> List.of(thing);
            default -> throw new UnsupportedOperationException(method);
        });
        Map<Short, Keeper> players = Map.of(KEEPER, new Keeper(false, KEEPER));
        ILevelInfo levelInfo = stub(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayers" -> players;
            case "getPlayer" -> players.get((Short) args[0]);
            default -> throw new UnsupportedOperationException(method);
        });
        IObjectsController objectsController = stub(IObjectsController.class, (method, args) -> entityData.createEntity());
        IGameTimer gameTimer = stub(IGameTimer.class, (method, args) -> 0.0);

        MapController mapController = new MapController(kwdFile, objectsController,
                Map.of(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, CreatureSleepFixtures.variable(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, 100)),
                gameTimer, entityData, levelInfo, null);
        return mapController.getRoomControllers().iterator().next();
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, java.util.function.BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args));
    }
}
