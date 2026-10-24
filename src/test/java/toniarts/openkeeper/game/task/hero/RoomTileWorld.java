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
package toniarts.openkeeper.game.task.hero;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;

/**
 * A one tile world for the tests of the heroes that destroy rooms: the tile,
 * the map and game world controllers over it, and the level variables
 */
public final class RoomTileWorld {

    public static final Point TILE = new Point(4, 5);
    public static final short LAIR_TERRAIN_ID = 11;
    public static final short CLAIMED_PATH_TERRAIN_ID = 8;
    public static final int ROOM_TILE_HEALTH = 10000;
    public static final int CLAIMED_PATH_HEALTH = 660;
    public static final int TICKS_PER_SEC = 4;

    public short ownerId = Player.KEEPER1_ID;
    public int health = ROOM_TILE_HEALTH;
    public Terrain terrain = DestroyRoomsFixtures.destroyRoomsTerrain(LAIR_TERRAIN_ID, CLAIMED_PATH_TERRAIN_ID, ROOM_TILE_HEALTH,
            Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.ROOM, Terrain.TerrainFlag.ATTACKABLE);
    public Room room = DestroyRoomsFixtures.destroyRoomsRoom("Lair", LAIR_TERRAIN_ID,
            Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.HAS_WALLS, Room.RoomFlag.BUILDABLE, Room.RoomFlag.ATTACKABLE);
    public final List<Point> destroyedTiles = new ArrayList<>();
    public final List<Integer> mentorMessages = new ArrayList<>();
    public double gameTime = 0;
    public final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new EnumMap<>(Variable.MiscVariable.MiscType.class);

    public final IMapTileController tile = stub(IMapTileController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getOwnerId" ->
            ownerId;
        case "getHealth" ->
            health;
        case "setHealth" -> {
            health = (int) args[0];
            yield null;
        }
        case "getTerrainId" ->
            terrain.getTerrainId();
        case "getLocation" ->
            TILE;
        default ->
            defaultValue(proxy, method);
    });
    private final IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, method, args) -> {
        if (method.getName().equals("getTile")) {
            Point p = args.length == 1 ? (Point) args[0] : new Point((int) args[0], (int) args[1]);
            return TILE.equals(p) ? tile : null;
        }
        return defaultValue(proxy, method);
    });
    private final IRoomController roomController = stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getRoom" ->
            room;
        case "getOwnerId" ->
            ownerId;
        default ->
            defaultValue(proxy, method);
    });
    public final IMapController mapController = stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getMapData" ->
            mapData;
        case "getTerrain" ->
            terrain;
        case "getRoomControllerByCoordinates" ->
            isRoom() && TILE.equals(args[0]) ? roomController : null;
        case "getRoomControllers" ->
            isRoom() ? List.of(roomController) : List.of();
        default ->
            defaultValue(proxy, method);
    });
    private final PlayerService playerService = stub(PlayerService.class, (proxy, method, args) -> {
        if (method.getName().equals("showMentorMessage")) {
            mentorMessages.add((Integer) args[0]);
        }
        return defaultValue(proxy, method);
    });
    private final IGameTimer gameTimer = stub(IGameTimer.class, (proxy, method, args) -> method.getName().equals("getGameTime") ? gameTime : defaultValue(proxy, method));
    private final RoomAttackMessages roomAttackMessages = new RoomAttackMessages(playerService, gameTimer);
    @SuppressWarnings("unchecked")
    public final IGameWorldController gameWorldController = stub(IGameWorldController.class, (proxy, method, args) -> {
        if (method.getName().equals("destroyRoomTiles")) {
            destroyedTiles.addAll((Collection<Point>) args[0]);

            // The room tile turns to the claimed path of its owner
            terrain = claimedPath();
            return null;
        } else if (method.getName().equals("getRoomAttackMessages")) {
            return roomAttackMessages;
        }
        return defaultValue(proxy, method);
    });

    public RoomTileWorld() {
        DestroyRoomsFixtures.setRoomId(room, (short) 2);
        setAttackRoomHealth(-1500);
    }

    private boolean isRoom() {
        return terrain.getFlags().contains(Terrain.TerrainFlag.ROOM);
    }

    /**
     * The claimed path terrain, it has no room
     *
     * @return the terrain
     */
    public static Terrain claimedPath() {
        return DestroyRoomsFixtures.destroyRoomsTerrain(CLAIMED_PATH_TERRAIN_ID, (short) 3, CLAIMED_PATH_HEALTH,
                Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.ATTACKABLE);
    }

    /**
     * Set the ATTACK_ROOM_HEALTH level variable
     *
     * @param value the value, negative as in the level files
     */
    public void setAttackRoomHealth(int value) {
        gameSettings.put(Variable.MiscVariable.MiscType.ATTACK_ROOM_HEALTH,
                DestroyRoomsFixtures.destroyRoomsVariable(Variable.MiscVariable.MiscType.ATTACK_ROOM_HEALTH, value));
    }

    /**
     * A creature of the given player
     *
     * @param playerId the owner of the creature
     * @return the creature
     */
    public static ICreatureController creature(short playerId) {
        return stub(ICreatureController.class, (proxy, method, args) -> method.getName().equals("getOwnerId") ? playerId : defaultValue(proxy, method));
    }

    /**
     * Default answers for the stubs
     *
     * @param proxy the stub
     * @param method the method called
     * @return a zero, false or null value of the method's type
     */
    public static Object defaultValue(Object proxy, Method method) {
        return switch (method.getName()) {
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                false;
            case "toString" ->
                "stub";
            default ->
                primitiveDefault(method.getReturnType());
        };
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) {
            return false;
        } else if (type == int.class) {
            return 0;
        } else if (type == long.class) {
            return 0L;
        } else if (type == float.class) {
            return 0f;
        } else if (type == short.class) {
            return (short) 0;
        }
        return null;
    }

    /**
     * Stub an interface
     *
     * @param <T> the interface type
     * @param type the interface
     * @param handler answers the calls
     * @return the stub
     */
    @SuppressWarnings("unchecked")
    public static <T> T stub(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
