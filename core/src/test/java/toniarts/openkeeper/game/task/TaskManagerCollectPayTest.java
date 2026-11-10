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
import com.simsilica.es.base.DefaultEntityData;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature that is owed pay goes to the nearest gold room of its keeper
 * that holds gold and that it can reach
 */
class TaskManagerCollectPayTest {

    private static final short KEEPER = Player.KEEPER1_ID;

    private final AtomicReference<Task> assigned = new AtomicReference<>();
    private final ICreatureController creature = stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getCreatureCoordinates" -> new Point(0, 2);
        case "getOwnerId" -> KEEPER;
        case "setAssignedTask" -> {
            assigned.set((Task) args[0]);
            yield null;
        }
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == args[0];
        default -> null;
    });

    @Test
    void goesToTheNearestRoomThatHoldsGold() {
        IRoomController empty = goldRoom(0, 3, 4);
        IRoomController near = goldRoom(500, 6, 7);
        IRoomController far = goldRoom(500, 12, 13);

        assertTrue(taskManager(List.of(far, empty, near), Set.of()).assignCollectPayTask(creature));

        assertTrue(near.getRoomInstance().hasCoordinate(assigned.get().getTaskLocation()), assigned.get().getTaskLocation().toString());
    }

    @Test
    void skipsARoomItCannotReach() {
        IRoomController near = goldRoom(500, 6, 7);
        IRoomController far = goldRoom(500, 12, 13);

        assertTrue(taskManager(List.of(near, far), Set.copyOf(near.getRoomInstance().getCoordinates())).assignCollectPayTask(creature));

        assertTrue(far.getRoomInstance().hasCoordinate(assigned.get().getTaskLocation()), assigned.get().getTaskLocation().toString());
    }

    @Test
    void hasNowhereToGoWithoutGold() {
        assertFalse(taskManager(List.of(goldRoom(0, 6, 7)), Set.of()).assignCollectPayTask(creature));

        assertNull(assigned.get());
    }

    /**
     * A one tile high room from x {@code fromX} to {@code toX}
     */
    private static IRoomController goldRoom(int gold, int fromX, int toX) {
        RoomInstance instance = new RoomInstance(new Room());
        for (int x = fromX; x <= toX; x++) {
            instance.addCoordinate(new Point(x, 2));
        }
        IRoomObjectControl<?> goldControl = stub(IRoomObjectControl.class, (proxy, method, args) -> "getCurrentCapacity".equals(method.getName()) ? gold : null);
        return stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomInstance" -> instance;
            case "getObjectControl" -> goldControl;
            case "isTileAccessible" -> true;
            case "getOwnerId" -> KEEPER;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        });
    }

    private static TaskManager taskManager(List<IRoomController> rooms, Set<Point> walledIn) {
        Keeper keeper = new Keeper(false, KEEPER);
        IPlayerController player = stub(IPlayerController.class, (proxy, method, args) -> "getKeeper".equals(method.getName()) ? keeper : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, method, args) -> null);
        IMapController mapController = stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getRoomsByFunction" -> rooms;
            default -> null;
        });
        INavigationService navigation = stub(INavigationService.class, (proxy, method, args)
                -> "findPath".equals(method.getName()) && !walledIn.contains((Point) args[1]) ? new DefaultGraphPath<>() : null);
        return new TaskManager(new DefaultEntityData(), stub(IGameWorldController.class, (proxy, method, args) -> null),
                mapController, stub(IObjectsController.class, (proxy, method, args) -> null), stub(ICreaturesController.class, (proxy, method, args) -> null),
                navigation, List.of(player), stub(ILevelInfo.class, (proxy, method, args) -> null),
                stub(IEntityPositionLookup.class, (proxy, method, args) -> null), Map.of());
    }

    /**
     * A stub of the interface, the primitives it doesn't answer are 0 or false
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object value = handler.invoke(proxy, method, args);
            Class<?> returnType = method.getReturnType();
            if (value != null || !returnType.isPrimitive() || returnType == void.class) {
                return value;
            }
            return returnType == boolean.class ? false : MethodHandles.zero(returnType).invoke();
        });
    }

}
