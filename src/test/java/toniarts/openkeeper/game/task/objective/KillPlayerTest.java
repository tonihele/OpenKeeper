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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Heroes sent to kill a keeper whose dungeon they can't walk to. A hero worker,
 * such as a Dwarf, tunnels its own way there even when it is on its own
 */
class KillPlayerTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final Point HEART_CENTRE = new Point(10, 10);
    private static final short FLOOR = 1;
    private static final short ROCK = 2;
    private static final short HEART_ROOM = 3;

    /**
     * The hero stands at the top, the rock below it and the heart room below
     * the rock
     */
    private static final Point HERO_TILE = new Point(10, 6);
    private static final Point ROCK_TILE = new Point(10, 7);

    private final Map<Short, Terrain> terrains = Map.of(
            FLOOR, TerrainFixtures.terrain(FLOOR, TerrainFixtures.flags(), 0, 0, 0, (short) 0),
            ROCK, TerrainFixtures.terrain(ROCK, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID,
                    Terrain.TerrainFlag.DWARF_CAN_DIG_THROUGH), 0, 0, 0, (short) 0),
            HEART_ROOM, TerrainFixtures.terrain(HEART_ROOM, TerrainFixtures.flags(Terrain.TerrainFlag.ROOM), 0, 0, 0, (short) 0));
    private final Map<Point, IMapTileInformation> tiles = new HashMap<>();

    @Test
    void loneWorkerTunnelsItsWayToTheHeart() {
        KillPlayer task = createTask(true);

        assertEquals(TaskType.DIG_TILE, task.getCurrentTask().getTaskType());
        assertEquals(ROCK_TILE, task.getCurrentTask().getTaskLocation());
    }

    @Test
    void loneFighterCanNotTunnel() {
        KillPlayer task = createTask(false);

        assertEquals(TaskType.KILL_PLAYER, task.getCurrentTask().getTaskType());
    }

    private KillPlayer createTask(boolean worker) {
        Keeper keeper = new Keeper(false, KEEPER);
        keeper.setDungeonHeartLocation(HEART_CENTRE);
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getTerrain".equals(name) ? terrains.get((Short) args[0]) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getPlayer" -> Map.of(KEEPER, keeper).get((Short) args[0]);
            case "getLevelData" -> kwdFile;
            default -> null;
        });

        // The 5x5 heart room, the centre 3x3 of it can't be walked on
        RoomInstance heartRoom = new RoomInstance(new Room());
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                heartRoom.addCoordinate(new Point(HEART_CENTRE.x + x, HEART_CENTRE.y + y));
            }
        }
        IRoomController roomController = stub(IRoomController.class, (name, args) -> switch (name) {
            case "getRoomInstance" -> heartRoom;
            case "isTileAccessible" -> {
                Point to = (Point) args[1];
                yield Math.abs(to.x - HEART_CENTRE.x) == 2 || Math.abs(to.y - HEART_CENTRE.y) == 2;
            }
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getRoomControllerByCoordinates" -> roomController;
            case "getTerrain" -> terrains.get(((IMapTileInformation) args[0]).getTerrainId());
            default -> null;
        });
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class, (name, args) -> null);

        // The only way to the heart is straight down, through the rock
        List<IMapTileInformation> way = List.of(getTile(HERO_TILE, FLOOR), getTile(ROCK_TILE, ROCK),
                getTile(new Point(10, 8), HEART_ROOM));
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> ((INavigable) args[2]).getCost((IMapTileInformation) args[0], (IMapTileInformation) args[1],
                    mapController, positionLookup) != null;
            case "findPath" -> {
                INavigable navigable = (INavigable) args[2];
                DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
                IMapTileInformation from = null;
                for (IMapTileInformation tile : way) {
                    if (from != null && navigable.getCost(from, tile, mapController, positionLookup) == null) {
                        yield null;
                    }
                    path.add(tile);
                    from = tile;
                }
                yield path;
            }
            default -> null;
        });
        ICreatureController hero = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> HERO_TILE;
            case "getOwnerId" -> HERO;
            case "isWorker" -> worker;
            default -> null;
        });

        return new KillPlayer(navigationService, mapController, levelInfo, KEEPER, hero);
    }

    private IMapTileInformation getTile(Point location, short terrainId) {
        return tiles.computeIfAbsent(location, p -> stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getLocation" -> p;
            case "getX" -> p.x;
            case "getY" -> p.y;
            case "getTerrainId" -> terrainId;
            default -> null;
        }));
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
