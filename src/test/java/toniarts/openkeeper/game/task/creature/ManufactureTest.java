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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ManufactureOrders;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.controller.room.storage.RoomCrateControl;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManufactureTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int BILE_DEMON = 12; // 50 a second in the data, divided by the 4 ticks a second

    @Test
    void creatureWorksOnEveryTick() {

        // A minute of logic ticks
        assertEquals(60 * TICKS_PER_SECOND * BILE_DEMON, manufactureFor(60 * TICKS_PER_SECOND));
    }

    @Test
    void firstTickAlreadyManufactures() {
        assertEquals(BILE_DEMON, manufactureFor(1));
    }

    private static int manufactureFor(int ticks) {
        PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
        Trap trap = TrapFixtures.trapToManufacture((short) 1, Integer.MAX_VALUE);
        manufactureControl.setOrder(new ManufactureOrders.Order(ResearchableType.TRAP, trap.getTrapId(), trap.getManufToBuild(),
                trap.getManufCrateObjectId()));
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getManufacturePerTick" -> BILE_DEMON;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Manufacture task = new Manufacture(null, null, new Point(3, 4), (short) 1, null, null, manufactureControl, null, null);
        task.assign(creature, false);

        for (int tick = 1; tick <= ticks; tick++) {
            task.executeTask(creature, (float) tick / TICKS_PER_SECOND);
        }
        return manufactureControl.getProgress();
    }

    @Test
    void taskManagerChecksTheTaskWithoutACreature() {
        assertFalse(manufactureInWorkshop(false).isRemovable());
        assertTrue(manufactureInWorkshop(true).isRemovable());
    }

    private static Manufacture manufactureInWorkshop(boolean full) {
        IRoomObjectControl<?> crates = (IRoomObjectControl<?>) Proxy.newProxyInstance(
                IRoomObjectControl.class.getClassLoader(), new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isFullCapacity" -> full;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomController workshop = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isRemoved", "isDestroyed" -> false;
                    case "getOwnerId" -> (short) 1;
                    case "getObjectControl" -> crates;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Trap trap = TrapFixtures.trapToManufacture((short) 1, Integer.MAX_VALUE);
        PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
        manufactureControl.setOrder(new ManufactureOrders.Order(ResearchableType.TRAP, trap.getTrapId(), trap.getManufToBuild(),
                trap.getManufCrateObjectId()));
        return new Manufacture(null, null, new Point(3, 4), (short) 1, workshop, null, manufactureControl, null, null);
    }

    @Test
    void crateWithoutRoomStaysOnTheFloorForNobody() {
        List<Short> crateOwners = new ArrayList<>();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "addRoomCrate" -> {
                        crateOwners.add((short) args[1]);
                        yield new EntityId(1);
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomObjectControl<?> fullWorkshop = (IRoomObjectControl<?>) Proxy.newProxyInstance(
                IRoomObjectControl.class.getClassLoader(), new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "addItem" -> args[0]; // No room for it
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomController workshop = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObjectControl" -> fullWorkshop;
                    case "getRoom" -> RoomFixtures.roomWithEffects(0, 0);
                    case "getRoomInstance" -> new RoomInstance(new Room());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Trap trap = TrapFixtures.trapToManufacture((short) 1, BILE_DEMON);
        PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
        manufactureControl.setOrder(new ManufactureOrders.Order(ResearchableType.TRAP, trap.getTrapId(), trap.getManufToBuild(),
                trap.getManufCrateObjectId()));
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getManufacturePerTick" -> BILE_DEMON;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Manufacture task = new Manufacture(null, null, new Point(3, 4), (short) 1, workshop, null, manufactureControl, objectsController,
                events(new ArrayList<>()));

        // Not removed, the objects controller would be asked for the crate's controller
        task.executeTask(creature, 1f / TICKS_PER_SECOND);

        assertEquals(List.of(Player.NEUTRAL_PLAYER_ID), crateOwners);
    }

    @Test
    void crateWithRoomIsGivenToTheWorkshopsKeeper() {
        short keeper = 3;
        EntityData entityData = new DefaultEntityData();
        EntityId roomEntity = entityData.createEntity();
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(new Point(3, 4));
        IRoomController[] room = new IRoomController[1];
        room[0] = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> keeper;
                    case "getRoomInstance" -> roomInstance;
                    case "isTileAccessible" -> true;
                    case "getFloorFurniture" -> Set.of();
                    case "getFloorFurnitureCount" -> 0;
                    case "getRoom" -> RoomFixtures.roomWithEffects(0, 0);
                    case "getObjectControl" -> new RoomCrateControl(null, room[0], entityData, () -> 0) {
                        @Override
                        protected int getNumberOfAccessibleTiles() {
                            return getCoordinates().size();
                        }
                    };
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        EntityId[] crate = new EntityId[1];
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "addRoomCrate" -> {
                        crate[0] = entityData.createEntity();
                        entityData.setComponent(crate[0], new Position(0, new Vector3f(3, WorldUtils.FLOOR_HEIGHT, 4)));
                        entityData.setComponent(crate[0], new Owner((short) args[1], (short) args[1]));
                        yield crate[0];
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Trap trap = TrapFixtures.trapToManufacture((short) 1, BILE_DEMON);
        PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
        manufactureControl.setOrder(new ManufactureOrders.Order(ResearchableType.TRAP, trap.getTrapId(), trap.getManufToBuild(),
                trap.getManufCrateObjectId()));
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getManufacturePerTick" -> BILE_DEMON;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Manufacture task = new Manufacture(null, null, new Point(3, 4), (short) 1, room[0], null, manufactureControl, objectsController,
                events(new ArrayList<>()));

        task.executeTask(creature, 1f / TICKS_PER_SECOND);

        // Made for nobody, the workshop's storage gives it to its keeper, whose crates the task manager looks for
        Owner owner = entityData.getComponent(crate[0], Owner.class);
        assertEquals(keeper, owner.ownerId);
        assertEquals(keeper, owner.controlId);
        assertNotNull(entityData.getComponent(crate[0], Stored.class));
    }

    @Test
    void madeItemSoundsTellsItsKeeperAndSparklesOverTheWorkshop() {
        IRoomObjectControl<?> fullWorkshop = (IRoomObjectControl<?>) Proxy.newProxyInstance(
                IRoomObjectControl.class.getClassLoader(), new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "addItem" -> args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        RoomInstance roomInstance = new RoomInstance(new Room());
        for (int x = 0; x < 20; x++) {
            roomInstance.addCoordinate(new Point(x, 4));
        }
        IRoomController workshop = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObjectControl" -> fullWorkshop;
                    case "getRoom" -> RoomFixtures.roomWithEffects(12, 49, 50, 51, 93, 0, 0, 0);
                    case "getRoomInstance" -> roomInstance;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> new EntityId(1));
        Trap trap = TrapFixtures.trapToManufacture((short) 4, BILE_DEMON);
        PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
        manufactureControl.setOrder(new ManufactureOrders.Order(ResearchableType.TRAP, trap.getTrapId(), trap.getManufToBuild(), (short) 5));
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getManufacturePerTick" -> BILE_DEMON;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        List<List<Object>> emitted = new ArrayList<>();
        Manufacture task = new Manufacture(null, null, new Point(3, 4), (short) 1, workshop, null, manufactureControl, objectsController,
                events(emitted));

        task.executeTask(creature, 1f / TICKS_PER_SECOND);

        // DK II (0x4F5240): the made sound of the trap at the crate, message 0x1A about the crate type to the workshop's
        // keeper, the room's first effect (Manufactured Item) once for every 9 of the 20 tiles and its second one (New
        // Manufacture Crate) at the crate
        Vector3f crate = new Vector3f(3, WorldUtils.FLOOR_HEIGHT, 4);
        assertEquals(List.of("emit", GameEventType.WORKSHOP_ITEM_MADE, ThingFlag.TRAP, 4, crate), emitted.get(0));
        assertEquals(List.of("emitToPlayer", GameEventType.WORKSHOP_ITEM_MADE_MESSAGE, 5, (short) 1), emitted.get(1));
        assertEquals(List.of("emitEffect", 12), emitted.get(2).subList(0, 2));
        assertEquals(WorldUtils.FLOOR_HEIGHT + 1.5f, ((Vector3f) emitted.get(2).get(2)).y);
        assertEquals(List.of("emitEffect", 12), emitted.get(3).subList(0, 2));
        assertEquals(List.of("emitEffect", 49, crate, 0f), emitted.get(4));
        assertEquals(5, emitted.size());
    }

    private static GameEventService events(List<List<Object>> emitted) {
        return (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class},
                (proxy, method, args) -> {
                    List<Object> call = new ArrayList<>(List.of(method.getName()));
                    call.addAll(List.of(args));
                    emitted.add(call);
                    return null;
                });
    }

}
