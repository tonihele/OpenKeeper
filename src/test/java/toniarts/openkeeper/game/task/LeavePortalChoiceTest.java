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
package toniarts.openkeeper.game.task;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.GameTimeController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.ThreeByThreeController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which portal a creature that leaves the dungeon walks to. DK II takes the
 * one nearest in a straight line, whoever owns it, and doesn't ask whether
 * the creature can get there (0x459DE0). A rebel only looks at its own
 * keeper's portals (0x475170).
 */
class LeavePortalChoiceTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final GameTimeController timer = new GameTimeController();
    private final List<IRoomController> portals = new ArrayList<>();
    private final List<Point> unreachable = new ArrayList<>();
    private Task assignedTask;
    private final ICreatureController creature = stub(ICreatureController.class, (name, args) -> switch (name) {
        case "getOwnerId" -> PLAYER_ID;
        case "getPosition" -> new Vector3f(5, 0, 5);
        case "getCreatureCoordinates" -> new Point(5, 5);
        case "setAssignedTask" -> assignedTask = (Task) args[0];
        default -> null;
    });

    @Test
    void leavingCreatureHeadsForThePortalNearestInAStraightLineEvenWhenItCannotGetThere() {
        portal(OTHER_PLAYER_ID, 9, 5);
        portal(PLAYER_ID, 5, 12);
        unreachable.add(new Point(9, 4));

        assertTrue(taskManager().assignTask(creature, Creature.JobType.LEAVE));

        assertEquals(new Vector2f(9, 4), assignedTask.getTarget(creature));
    }

    @Test
    void rebelHeadsForTheNearestPortalOfItsOwnKeeper() {
        portal(OTHER_PLAYER_ID, 9, 5);
        portal(PLAYER_ID, 5, 12);
        portal(PLAYER_ID, 5, 14);

        assertTrue(taskManager().assignTask(creature, Creature.JobType.REBEL));

        assertEquals(new Vector2f(5, 11), assignedTask.getTarget(creature));
    }

    @Test
    void leavingCreatureWalksToATileOfThePortalThatCanBeWalkedOn() {
        portal(PLAYER_ID, 5, 12);

        assertTrue(taskManager().assignTask(creature, Creature.JobType.LEAVE));

        // The middle can't be walked onto, a walk there would never start
        Vector2f target = assignedTask.getTarget(creature);
        assertTrue(portals.getFirst().isTileAccessible(null, new Point((int) target.x, (int) target.y)));
    }

    private void portal(short ownerId, int centerX, int centerY) {
        RoomInstance room = new RoomInstance(new Room());
        room.setOwnerId(ownerId);
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                room.addCoordinate(new Point(x, y));
            }
        }
        portals.add(new ThreeByThreeController(entityData.createEntity(), entityData, null, room, null, timer));
    }

    private TaskManager taskManager() {

        // No tiles to scan for tasks at the start, but every tile can be looked up
        IMapDataInformation<?> map = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth", "getHeight" -> 0;
            case "getTile" -> args.length == 1 ? tile((Point) args[0]) : tile(new Point((int) args[0], (int) args[1]));
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> map;
            case "getRoomControllers" -> portals;
            default -> null;
        });

        // Every tile can be walked on, but some places can't be got to
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> true;
            case "findPath" -> unreachable.contains((Point) args[1]) ? null : new DefaultGraphPath<IMapTileInformation>();
            case "isReachable" -> !unreachable.contains((Point) args[1]);
            default -> null;
        });
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> "getKeeper".equals(name) ? keeper : null);
        return new TaskManager(entityData, stub(IGameWorldController.class, (name, args) -> null), mapController, null, null,
                navigationService, List.of(player), stub(ILevelInfo.class, (name, args) -> null),
                stub(IEntityPositionLookup.class, (name, args) -> null), Map.of());
    }

    private static IMapTileInformation tile(Point location) {
        return stub(IMapTileInformation.class, (name, args) -> "getLocation".equals(name) ? location : null);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> answer.answer(method.getName(), args);
        });
    }

}
