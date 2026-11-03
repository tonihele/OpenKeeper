/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

/**
 * The wall furniture of a room, laid out like DK II does (0x4EDFC0)
 */
class RoomWallFurnitureTest {

    private static final short KEEPER_ID = 3;
    private static final short PATH_ID = 0;
    private static final short WALL_ID = 9;
    private static final short TORTURE_CHAMBER_TERRAIN_ID = 20;
    private static final short WHEEL = 8;
    private static final short COALS = 62;
    private static final short WATER = 63;

    private final EntityData entityData = new DefaultEntityData();
    private final Room tortureChamber = tortureChamber();
    private final Terrain wall = TerrainFixtures.terrain(WALL_ID,
            flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.ALLOW_ROOM_WALLS), 0, 10, 10, WALL_ID);
    private final Terrain path = TerrainFixtures.terrain(PATH_ID, flags(), 0, 10, 10, PATH_ID);
    private final Terrain roomTerrain = TerrainFixtures.terrain(TORTURE_CHAMBER_TERRAIN_ID,
            flags(Terrain.TerrainFlag.ROOM, Terrain.TerrainFlag.OWNABLE), 0, 10, 10, TORTURE_CHAMBER_TERRAIN_ID);

    @Test
    void everySecondInnerWallTileGetsAnObjectPickedFromTheTile() {

        // A 5x5 room, the walls have 3 inner tiles each, the 1st and 3rd get an object
        MapController mapController = mapController();
        IRoomController room = mapController.getRoomControllerByCoordinates(new Point(3, 3));

        assertEquals(List.of(
                new Placed(WATER, 2, 1), new Placed(WHEEL, 2, 1),
                new Placed(COALS, 4, 1), new Placed(WHEEL, 4, 1),
                new Placed(COALS, 1, 2), new Placed(WHEEL, 1, 2),
                new Placed(COALS, 5, 2), new Placed(WHEEL, 5, 2),
                new Placed(WHEEL, 1, 4),
                new Placed(COALS, 5, 4), new Placed(WHEEL, 5, 4),
                new Placed(COALS, 2, 5), new Placed(WHEEL, 2, 5),
                new Placed(WHEEL, 4, 5)), placed(room.getWallFurniture()));

        // One victim on each wheel and floor device
        assertEquals(8 + room.getFloorFurnitureCount(), room.getObjectControl(ObjectType.TORTUREE).getMaxCapacity());
    }

    @Test
    void wallObjectsStandAgainstTheWallFacingTheRoom() {
        MapController mapController = mapController();
        IRoomController room = mapController.getRoomControllerByCoordinates(new Point(3, 3));

        // The water trough on the north wall, with a wheel on the wall half a tile up
        assertPosition(FastMath.PI, new Vector3f(2, WorldUtils.FLOOR_HEIGHT, 1 - 0x480 / 4096f), room, WATER, 2, 1);
        assertPosition(FastMath.PI, new Vector3f(2, WorldUtils.FLOOR_HEIGHT + 0.5f, 1 - 0x7C0 / 4096f), room, WHEEL, 2, 1);

        // The lone wheel on the west wall faces east
        assertPosition(-FastMath.HALF_PI, new Vector3f(1 - 0x7C0 / 4096f, WorldUtils.FLOOR_HEIGHT + 0.5f, 4), room, WHEEL, 1, 4);
    }

    @Test
    void diggingAWallOutRemovesTheFurnitureOfThatWall() {
        MapController mapController = mapController();

        // The north wall is cut in two, neither part has inner tiles
        mapController.alterTerrain(new Point(3, 0), PATH_ID, KEEPER_ID);

        IRoomController room = mapController.getRoomControllerByCoordinates(new Point(3, 3));
        assertEquals(List.of(
                new Placed(COALS, 1, 2), new Placed(WHEEL, 1, 2),
                new Placed(COALS, 5, 2), new Placed(WHEEL, 5, 2),
                new Placed(WHEEL, 1, 4),
                new Placed(COALS, 5, 4), new Placed(WHEEL, 5, 4),
                new Placed(COALS, 2, 5), new Placed(WHEEL, 2, 5),
                new Placed(WHEEL, 4, 5)), placed(room.getWallFurniture()));
    }

    private record Placed(short objectId, int x, int y) {

    }

    private List<Placed> placed(Iterable<EntityId> objects) {
        List<Placed> placed = new ArrayList<>();
        for (EntityId object : objects) {
            Point p = WorldUtils.vectorToPoint(entityData.getComponent(object, Position.class).position);
            placed.add(new Placed(entityData.getComponent(object, ObjectComponent.class).objectId, p.x, p.y));
        }
        placed.sort((a, b) -> a.y != b.y ? Integer.compare(a.y, b.y)
                : a.x != b.x ? Integer.compare(a.x, b.x) : Integer.compare(b.objectId, a.objectId));
        return placed;
    }

    private void assertPosition(float rotation, Vector3f expected, IRoomController room, short objectId, int x, int y) {
        for (EntityId object : room.getWallFurniture()) {
            Position position = entityData.getComponent(object, Position.class);
            if (entityData.getComponent(object, ObjectComponent.class).objectId == objectId
                    && WorldUtils.vectorToPoint(position.position).equals(new Point(x, y))) {
                assertEquals(rotation, position.rotation);
                assertEquals(expected, position.position);
                return;
            }
        }
        fail("No object " + objectId + " at " + x + ", " + y);
    }

    /**
     * A 7x7 map: a 5x5 Torture Chamber from (1, 1) to (5, 5) in reinforced
     * walls
     */
    private MapController mapController() {
        GameMap map = new GameMap(7, 7);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                boolean inRoom = x > 0 && x < 6 && y > 0 && y < 6;
                map.getTiles()[x][y] = new Tile() {

                    @Override
                    public short getTerrainId() {
                        return inRoom ? TORTURE_CHAMBER_TERRAIN_ID : WALL_ID;
                    }

                    @Override
                    public short getPlayerId() {
                        return KEEPER_ID;
                    }
                };
            }
        }
        IKwdFile kwdFile = proxy(IKwdFile.class, (name, args) -> switch (name) {
            case "getMap" ->
                map;
            case "getTerrain" ->
                (short) args[0] == WALL_ID ? wall : (short) args[0] == PATH_ID ? path : roomTerrain;
            case "getRoomByTerrain" ->
                tortureChamber;
            case "getThings" ->
                List.of();
            default ->
                null;
        });
        Keeper keeper = new Keeper(false, KEEPER_ID);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (name, args) -> switch (name) {
            case "getPlayers" ->
                Map.of(KEEPER_ID, keeper);
            default ->
                keeper;
        });
        return new MapController(kwdFile, objectsController(), Map.of(), null, entityData, levelInfo,
                proxy(GameEventService.class, (name, args) -> null));
    }

    private IObjectsController objectsController() {
        return proxy(IObjectsController.class, (name, args) -> {
            if (!name.equals("loadObject")) {
                return null;
            }
            Vector3f position = args[2] instanceof Vector3f vector ? vector.setY(WorldUtils.FLOOR_HEIGHT)
                    : new Vector3f((int) args[2], WorldUtils.FLOOR_HEIGHT, (int) args[3]);
            float rotation = args[2] instanceof Vector3f ? (float) args[3] : args.length > 4 ? (float) args[4] : 0;
            EntityId object = entityData.createEntity();
            entityData.setComponents(object, new ObjectComponent((short) args[0], null), new Position(rotation, position));
            return object;
        });
    }

    private static Room tortureChamber() {
        Room room = RoomFixtures.room((short) 12, 0, Room.RoomFlag.HAS_WALLS, Room.RoomFlag.SPECIAL_TILES, Room.RoomFlag.SPECIAL_WALLS);
        set(room, "name", "Torture Chamber");
        set(room, "tileConstruction", Room.TileConstruction.NORMAL);
        set(room, "objects", List.of((short) 8, (short) 8, (short) 61, WHEEL, COALS, WATER, (short) 0, (short) 0));
        return room;
    }

    private static void set(Room room, String field, Object value) {
        try {
            Field f = Room.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(room, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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
