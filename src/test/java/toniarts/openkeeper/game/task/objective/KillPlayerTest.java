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
package toniarts.openkeeper.game.task.objective;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Heroes sent to kill a keeper. They walk to the heart if they can. If they
 * can't, a hero worker, such as a Dwarf, tunnels there the way DK II does,
 * even when it is on its own
 */
class KillPlayerTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final short FLOOR = 1;
    private static final short ROCK = 2;
    private static final short HEART_ROOM = 3;
    private static final short IMPENETRABLE = 4;

    /**
     * The hero ('h') has rock ('#') between it and the heart ('H')
     */
    private static final String[] ROCK_ALL_ROUND = {
        "XXXXXXXX",
        "Xh#####X",
        "X######X",
        "X####H#X",
        "X######X",
        "XXXXXXXX"};

    /**
     * Impenetrable rock ('X') across the line to the heart
     */
    private static final String[] WALL_ON_THE_LINE = {
        "XXXXXXXXX",
        "X#######X",
        "X###X###X",
        "Xh##X##HX",
        "X###X###X",
        "X#######X",
        "XXXXXXXXX"};

    private final Map<Short, Terrain> terrains = Map.of(
            FLOOR, TerrainFixtures.terrain(FLOOR, TerrainFixtures.flags(), 0, 0, 0, (short) 0),
            ROCK, TerrainFixtures.terrain(ROCK, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID,
                    Terrain.TerrainFlag.DWARF_CAN_DIG_THROUGH), 0, 0, 0, (short) 0),
            HEART_ROOM, TerrainFixtures.terrain(HEART_ROOM, TerrainFixtures.flags(Terrain.TerrainFlag.ROOM), 0, 0, 0, (short) 0),
            IMPENETRABLE, TerrainFixtures.terrain(IMPENETRABLE, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID,
                    Terrain.TerrainFlag.IMPENETRABLE), 0, 0, 0, (short) 0));

    @Test
    void loneWorkerTunnelsItsWayToTheHeart() {
        KillPlayer task = createTask(ROCK_ALL_ROUND, true);

        assertEquals(TaskType.DIG_TILE, task.getCurrentTask().getTaskType());
        assertEquals(new Point(2, 1), task.getCurrentTask().getTaskLocation());
    }

    @Test
    void loneFighterCanNotTunnel() {
        KillPlayer task = createTask(ROCK_ALL_ROUND, false);

        assertEquals(TaskType.KILL_PLAYER, task.getCurrentTask().getTaskType());
    }

    @Test
    void heroWalksWhenItCan() {
        KillPlayer task = createTask(new String[]{
            "XXXXXX",
            "Xh..HX",
            "XXXXXX"}, true);

        assertTrue(task.getTaskQueue().isEmpty());
    }

    /**
     * Each step is on the axis with more of the way left, on the y axis when
     * they are even
     */
    @Test
    void tunnelGoesStraightForTheHeart() {
        KillPlayer task = createTask(ROCK_ALL_ROUND, true);

        assertEquals(List.of(new Point(2, 1), new Point(3, 1), new Point(3, 2), new Point(4, 2), new Point(4, 3)), getDigs(task));
    }

    /**
     * Both ways round are as long, the walker keeping the wall on its left
     * gets back to the line first
     */
    @Test
    void tunnelFollowsTheWallRoundImpenetrableRock() {
        KillPlayer task = createTask(WALL_ON_THE_LINE, true);

        assertEquals(List.of(new Point(2, 3), new Point(3, 3), new Point(3, 4), new Point(3, 5), new Point(4, 5),
                new Point(5, 5), new Point(5, 4), new Point(5, 3), new Point(6, 3)), getDigs(task));
    }

    private static List<Point> getDigs(KillPlayer task) {
        List<Point> digs = new ArrayList<>();
        for (ObjectiveTask subTask : task.getTaskQueue()) {
            if (subTask.getTaskType() == TaskType.DIG_TILE) {
                digs.add(subTask.getTaskLocation());
            }
        }
        return digs;
    }

    private KillPlayer createTask(String[] map, boolean worker) {
        Map<Point, IMapTileController> tiles = new HashMap<>();
        Point heroTile = null;
        Point heartTile = null;
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length(); x++) {
                Point p = new Point(x, y);
                char c = map[y].charAt(x);
                short terrainId = switch (c) {
                    case '#' -> ROCK;
                    case 'X' -> IMPENETRABLE;
                    case 'H' -> HEART_ROOM;
                    default -> FLOOR;
                };
                if (c == 'h') {
                    heroTile = p;
                } else if (c == 'H') {
                    heartTile = p;
                }
                tiles.put(p, stub(IMapTileController.class, (name, args) -> switch (name) {
                    case "getLocation" -> p;
                    case "getX" -> p.x;
                    case "getY" -> p.y;
                    case "getTerrainId" -> terrainId;
                    case "getOwnerId" -> (short) 0;
                    default -> null;
                }));
            }
        }
        Point heroLocation = heroTile;
        Point heart = heartTile;

        Keeper keeper = new Keeper(false, KEEPER);
        keeper.setDungeonHeartLocation(heart);
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getTerrain".equals(name) ? terrains.get((Short) args[0]) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getPlayer" -> Map.of(KEEPER, keeper).get((Short) args[0]);
            case "getLevelData" -> kwdFile;
            default -> null;
        });

        // The heart room is the one tile here
        RoomInstance heartRoom = new RoomInstance(new Room());
        heartRoom.addCoordinate(heart);
        IRoomController roomController = stub(IRoomController.class, (name, args) -> switch (name) {
            case "getRoomInstance" -> heartRoom;
            case "isTileAccessible" -> true;
            default -> null;
        });
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth" -> map[0].length();
            case "getHeight" -> map.length;
            case "getTile" -> args.length == 2 ? tiles.get(new Point((Integer) args[0], (Integer) args[1])) : null;
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getRoomControllerByCoordinates" -> roomController;
            case "getMapData" -> mapData;
            case "getTerrain" -> terrains.get(((IMapTileInformation) args[0]).getTerrainId());
            default -> null;
        });
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> !isSolid((IMapTileInformation) args[0]) && !isSolid((IMapTileInformation) args[1]);
            default -> null;
        });
        ICreatureController hero = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> heroLocation;
            case "getOwnerId" -> HERO;
            case "isWorker" -> worker;
            default -> null;
        });

        return new KillPlayer(navigationService, mapController, levelInfo, KEEPER, hero);
    }

    private boolean isSolid(IMapTileInformation tile) {
        return terrains.get(tile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    /**
     * A stub that answers what it is told and otherwise runs the default
     * methods of the interface
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
