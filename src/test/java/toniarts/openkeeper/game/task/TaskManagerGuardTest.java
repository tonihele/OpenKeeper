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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.GuardedRoom;
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
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature going on guard duty goes back to the Guard Room it guarded last
 * while that room is still its keeper's and has space. Otherwise it takes the
 * nearest Guard Room and remembers that one (DKII.EXE 0x462850)
 */
class TaskManagerGuardTest {

    private static final short KEEPER = Player.KEEPER1_ID;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId creatureId = entityData.createEntity();
    private final AtomicReference<Task> assigned = new AtomicReference<>();
    private final ICreatureController creature = stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getCreatureCoordinates" -> new Point(0, 2);
        case "getOwnerId" -> KEEPER;
        case "getEntityId" -> creatureId;
        case "setAssignedTask" -> {
            assigned.set((Task) args[0]);
            yield null;
        }
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == args[0];
        default -> null;
    });

    @Test
    void takesTheNearestGuardRoomAndRemembersIt() {
        IRoomController near = guardRoom(false, 6, 7);
        IRoomController far = guardRoom(false, 12, 13);

        assertTrue(taskManager(List.of(far, near)).assignTask(creature, Creature.JobType.GUARD));

        assertTrue(near.getRoomInstance().hasCoordinate(assigned.get().getTaskLocation()));
        assertEquals(near.getEntityId(), entityData.getComponent(creatureId, GuardedRoom.class).room);
    }

    @Test
    void goesBackToTheGuardRoomItGuardedLast() {
        IRoomController near = guardRoom(false, 6, 7);
        IRoomController far = guardRoom(false, 12, 13);
        entityData.setComponent(creatureId, new GuardedRoom(far.getEntityId()));

        assertTrue(taskManager(List.of(far, near)).assignTask(creature, Creature.JobType.GUARD));

        assertTrue(far.getRoomInstance().hasCoordinate(assigned.get().getTaskLocation()));
        assertEquals(far.getEntityId(), entityData.getComponent(creatureId, GuardedRoom.class).room);
    }

    @Test
    void takesTheNearestWhenTheOneItGuardedLastIsFullOrGone() {
        IRoomController near = guardRoom(false, 6, 7);
        IRoomController full = guardRoom(true, 12, 13);
        entityData.setComponent(creatureId, new GuardedRoom(full.getEntityId()));

        assertTrue(taskManager(List.of(full, near)).assignTask(creature, Creature.JobType.GUARD));

        assertTrue(near.getRoomInstance().hasCoordinate(assigned.get().getTaskLocation()));
        assertEquals(near.getEntityId(), entityData.getComponent(creatureId, GuardedRoom.class).room);

        // The room is sold or lost, it is not among the keeper's Guard Rooms anymore
        IRoomController other = guardRoom(false, 20, 21);

        assertTrue(taskManager(List.of(other)).assignTask(creature, Creature.JobType.GUARD));

        assertEquals(other.getEntityId(), entityData.getComponent(creatureId, GuardedRoom.class).room);
    }

    @Test
    void checkingForGuardDutyRemembersNothing() {
        assertTrue(taskManager(List.of(guardRoom(false, 6, 7))).isTaskAvailable(creature, Creature.JobType.GUARD));
        assertFalse(taskManager(List.of(guardRoom(true, 6, 7))).isTaskAvailable(creature, Creature.JobType.GUARD));

        assertNull(assigned.get());
        assertNull(entityData.getComponent(creatureId, GuardedRoom.class));
    }

    @Test
    void prefersAGuardRoomWithoutCreaturesOfTheOtherSideEvenWhenItIsFurtherAway() {
        IRoomController near = guardRoom(false, 6, 7);
        IRoomController far = guardRoom(false, 12, 13);
        TaskManager taskManager = taskManager(List.of(far, near));
        AtomicReference<Task> evilsTask = new AtomicReference<>();
        AtomicReference<Task> goodsTask = new AtomicReference<>();
        AtomicReference<Task> otherEvilsTask = new AtomicReference<>();

        // The first one takes the nearest room, a good creature then stays away from it and an evil one joins it
        assertTrue(taskManager.assignTask(guard(Creature.CreatureFlag.IS_EVIL, evilsTask), Creature.JobType.GUARD));
        assertTrue(taskManager.assignTask(guard(Creature.CreatureFlag.IS_GOOD, goodsTask), Creature.JobType.GUARD));
        assertTrue(taskManager.assignTask(guard(Creature.CreatureFlag.IS_EVIL, otherEvilsTask), Creature.JobType.GUARD));

        assertTrue(near.getRoomInstance().hasCoordinate(evilsTask.get().getTaskLocation()));
        assertTrue(far.getRoomInstance().hasCoordinate(goodsTask.get().getTaskLocation()));
        assertTrue(near.getRoomInstance().hasCoordinate(otherEvilsTask.get().getTaskLocation()));
    }

    @Test
    void sharesARoomWithTheOtherSideWhenThereIsNoOther() {
        IRoomController only = guardRoom(false, 6, 7);
        TaskManager taskManager = taskManager(List.of(only));
        AtomicReference<Task> goodsTask = new AtomicReference<>();

        assertTrue(taskManager.assignTask(guard(Creature.CreatureFlag.IS_EVIL, new AtomicReference<>()), Creature.JobType.GUARD));
        assertTrue(taskManager.assignTask(guard(Creature.CreatureFlag.IS_GOOD, goodsTask), Creature.JobType.GUARD));

        assertTrue(only.getRoomInstance().hasCoordinate(goodsTask.get().getTaskLocation()));
    }

    private ICreatureController guard(Creature.CreatureFlag side, AtomicReference<Task> task) {
        EntityId id = entityData.createEntity();
        Creature type = TrapFiringFixtures.creature((short) 1, side);
        return stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getCreatureCoordinates" -> new Point(0, 2);
            case "getOwnerId" -> KEEPER;
            case "getEntityId" -> id;
            case "getCreature" -> type;
            case "setAssignedTask" -> {
                task.set((Task) args[0]);
                yield null;
            }
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        });
    }

    /**
     * A one tile high Guard Room from x {@code fromX} to {@code toX}
     */
    private IRoomController guardRoom(boolean full, int fromX, int toX) {
        RoomInstance instance = new RoomInstance(new Room());
        for (int x = fromX; x <= toX; x++) {
            instance.addCoordinate(new Point(x, 2));
        }
        EntityId roomId = entityData.createEntity();
        IRoomObjectControl<?> guardControl = stub(IRoomObjectControl.class, (proxy, method, args) -> switch (method.getName()) {
            case "isFullCapacity" -> full;
            case "getAvailableCoordinates" -> instance.getCoordinates();
            default -> null;
        });
        return stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomInstance" -> instance;
            case "getObjectControl" -> guardControl;
            case "getEntityId" -> roomId;
            case "isTileAccessible" -> true;
            case "getOwnerId" -> KEEPER;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        });
    }

    private TaskManager taskManager(List<IRoomController> rooms) {
        Keeper keeper = new Keeper(false, KEEPER);
        IPlayerController player = stub(IPlayerController.class, (proxy, method, args) -> "getKeeper".equals(method.getName()) ? keeper : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, method, args) -> null);
        IMapController mapController = stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getRoomsByFunction" -> rooms;
            default -> null;
        });
        INavigationService navigation = stub(INavigationService.class, (proxy, method, args)
                -> "findPath".equals(method.getName()) ? new DefaultGraphPath<>() : null);
        IKwdFile kwdFile = stub(IKwdFile.class, (proxy, method, args) -> "getGameLevel".equals(method.getName()) ? CreatureSleepFixtures.level(4) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (proxy, method, args) -> "getLevelData".equals(method.getName()) ? kwdFile : null);
        return new TaskManager(entityData, stub(IGameWorldController.class, (proxy, method, args) -> null),
                mapController, stub(IObjectsController.class, (proxy, method, args) -> null), stub(ICreaturesController.class, (proxy, method, args) -> null),
                navigation, List.of(player), levelInfo, stub(IEntityPositionLookup.class, (proxy, method, args) -> null),
                Map.of(MiscType.MAX_TIME_IN_GUARD_ROOM_BEFORE_WANDERING_TO_GUARD_POST_SECONDS,
                        CreatureSleepFixtures.variable(MiscType.MAX_TIME_IN_GUARD_ROOM_BEFORE_WANDERING_TO_GUARD_POST_SECONDS, 5)));
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
