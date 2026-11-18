/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomTortureeControl;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import static org.junit.jupiter.api.Assertions.*;

class TortureAdmissionTest {
    @Test
    void admissionPublishesTheDevicePositionStateAndOwnerTogether() {
        try (var f = new Fixture()) {
            EntityId victim = f.victim((short) 6, (short) 1);
            assertEquals(f.wheel, f.control.admit(victim, new Vector2f(1, 0)));
            assertEquals(f.wheel, f.data.getComponent(victim, CreatureTortured.class).device);
            assertEquals(CreatureState.TORTURED, f.data.getComponent(victim, CreatureAi.class).getCreatureState());
            assertEquals(new Vector3f(0, WorldUtils.FLOOR_HEIGHT, 0), f.data.getComponent(victim, Position.class).position);
            assertEquals(1.25f, f.data.getComponent(victim, Position.class).rotation);
            assertEquals(3, f.data.getComponent(victim, Owner.class).controlId);
            assertEquals(f.roomId, f.data.getComponent(victim, Stored.class).room);
            assertEquals(1, f.control.getCurrentCapacity());
            assertNull(f.control.admit(victim, new Vector2f()));
            assertEquals(1, f.control.getCurrentCapacity());
        }
    }

    @Test
    void nearestDeviceUsesStraightLineDistanceFromTheActualDropPoint() {
        try (var f = new Fixture()) {
            EntityId diagonal = f.device(2, 2);
            assertEquals(diagonal, f.control.admit(f.victim((short) 6, (short) 1), new Vector2f(3, 0)));
        }
    }

    @Test
    void freeDeviceOnTheDropTileWinsEvenIfAnotherIsCloser() {
        try (var f = new Fixture()) {
            f.data.setComponent(f.wheel, new Position(0, new Vector3f(-.49f, 0, 0)));
            f.device(.51f, 0);
            assertEquals(f.wheel, f.control.admit(f.victim((short) 6, (short) 1), new Vector2f(.49f, 0)));
        }
    }

    @Test
    void occupiedAndVoluntarilyUsedDevicesAreSkipped() {
        try (var f = new Fixture()) {
            EntityId voluntary = f.victim((short) 6, (short) 3);
            f.data.setComponent(voluntary, new TaskComponent(1, f.wheel, new Point(),
                    toniarts.openkeeper.game.task.TaskType.TORTURE, 0, true));
            EntityId other = f.device(2, 0);
            assertEquals(other, f.control.admit(f.victim((short) 6, (short) 1), new Vector2f()));
            EntityId refused = f.victim((short) 6, (short) 1);
            assertNull(f.control.admit(refused, new Vector2f()));
            assertNull(f.data.getComponent(refused, Stored.class));
            assertNull(f.data.getComponent(refused, CreatureTortured.class));
        }
    }

    @Test
    void ownImpsDeadCreaturesAndInvalidPositionsAreNotAdmitted() {
        try (var f = new Fixture()) {
            assertNull(f.control.admit(f.victim((short) 1, (short) 3), new Vector2f()));
            EntityId dead = f.victim((short) 6, (short) 1);
            f.data.removeComponent(dead, Health.class);
            assertNull(f.control.admit(dead, new Vector2f()));
            assertNull(f.control.admit(f.victim((short) 6, (short) 1), new Vector2f(Float.NaN, 0)));
            assertEquals(0, f.control.getCurrentCapacity());
        }
    }

    @Test
    void realSpawnUsesTheSameAdmissionAndKeepsTheSelectedPosition() {
        try (var f = new Fixture()) {
            var factory = new CreaturesController(f.kwd, f.data, Map.of(), () -> 20, null, f.map, null);
            EntityId victim = factory.spawnCreature((short) 6, (short) 1, 1, new Vector2f(1, 0), ICreaturesController.SpawnType.DROP);
            assertEquals(f.wheel, f.data.getComponent(victim, CreatureTortured.class).device);
            assertEquals(CreatureState.TORTURED, f.data.getComponent(victim, CreatureAi.class).getCreatureState());
            assertEquals(new Vector3f(0, WorldUtils.FLOOR_HEIGHT, 0), f.data.getComponent(victim, Position.class).position);
            assertEquals(3, f.data.getComponent(victim, Owner.class).controlId);
        }
    }

    @Test
    void ordinaryPlacementDoesNotAdmitButOwnRoomDropDoes() {
        try (var f = new Fixture()) {
            var factory = new CreaturesController(f.kwd, f.data, Map.of(), () -> 20, null, f.map, null);
            EntityId placed = factory.spawnCreature((short) 6, (short) 1, 1, new Vector2f(), ICreaturesController.SpawnType.PLACE);
            assertNull(f.data.getComponent(placed, CreatureTortured.class));
            EntityId dropped = factory.spawnCreature((short) 6, (short) 3, 1, new Vector2f(), ICreaturesController.SpawnType.DROP);
            assertEquals(f.wheel, f.data.getComponent(dropped, CreatureTortured.class).device);
        }
    }

    @Test
    void concurrentAdmissionsCannotShareOneDevice() throws Exception {
        try (var f = new Fixture(); var threads = Executors.newFixedThreadPool(2)) {
            EntityId first = f.victim((short) 6, (short) 1), second = f.victim((short) 6, (short) 1);
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            var results = List.of(first, second).stream().map(id -> threads.submit(() -> {
                ready.countDown();
                start.await();
                return f.control.admit(id, new Vector2f());
            })).toList();
            ready.await(); start.countDown();
            int accepted = 0;
            for (var result : results) if (result.get() != null) accepted++;
            assertEquals(1, accepted);
            assertEquals(1, f.control.getCurrentCapacity());
            assertEquals(1, f.control.getItems().size());
        }
    }

    @Test
    void destroyedRoomRefusesNewAdmissionsWithoutWritingState() {
        try (var f = new Fixture()) {
            f.control.destroy();
            EntityId victim = f.victim((short) 6, (short) 1);
            assertNull(f.control.admit(victim, new Vector2f()));
            assertNull(f.data.getComponent(victim, Stored.class));
            assertNull(f.data.getComponent(victim, CreatureTortured.class));
        }
    }

    @Test
    void legacyAddItemKeepsItsDeviceReturnContractAndCountsEachVictimOnce() {
        try (var f = new Fixture()) {
            EntityId victim = f.victim((short) 6, (short) 1);
            assertEquals(f.wheel, f.control.addItem(victim, new Point()));
            assertNull(f.control.addItem(victim, new Point()));
            assertEquals(1, f.control.getCurrentCapacity());
            f.control.removeItem(victim);
            f.control.removeItem(victim);
            assertEquals(0, f.control.getCurrentCapacity());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        final EntityId roomId = data.createEntity();
        final Set<EntityId> devices = new LinkedHashSet<>();
        final EntityId wheel = device(0, 0);
        final RoomInstance instance = new RoomInstance(RoomFixtures.room((short) 12, 0));
        final Creature creature = definition((short) 6), imp = definition((short) 1);
        final IKwdFile kwd = proxy(IKwdFile.class, (name, args) -> switch (name) {
            case "getThings" -> List.of();
            case "getCreature" -> ((Number) args[0]).shortValue() == 1 ? imp : creature;
            case "getImp" -> imp;
            case "getCreatureStats" -> null;
            default -> null;
        });
        final IRoomController room = proxy(IRoomController.class, (name, args) -> switch (name) {
            case "getEntityId" -> roomId;
            case "getOwnerId" -> (short) 3;
            case "getRoomInstance" -> instance;
            case "getFloorFurniture" -> devices;
            case "getWallFurniture" -> Set.of();
            case "getFloorFurnitureCount" -> devices.size();
            case "getWallFurnitureCount" -> 0;
            case "hasObjectControl" -> args[0] == ObjectType.TORTUREE;
            case "getObjectControl" -> storage();
            default -> throw new UnsupportedOperationException(name);
        });
        final RoomTortureeControl control = new RoomTortureeControl(kwd, room, data, () -> 20) {
            @Override protected int getNumberOfAccessibleTiles() { return devices.size(); }
        };
        final IMapController map = proxy(IMapController.class, (name, args) -> room);
        Fixture() { instance.setOwnerId((short) 3); }
        RoomTortureeControl storage() { return control; }
        EntityId device(float x, float y) {
            EntityId id = data.createEntity();
            data.setComponents(id, new Position(1.25f, new Vector3f(x, 0, y)), new ObjectComponent((short) 8, ObjectType.TORTUREE));
            devices.add(id); return id;
        }
        EntityId victim(short type, short owner) {
            EntityId id = data.createEntity();
            var component = new CreatureComponent(); component.creatureId = type;
            data.setComponents(id, component, new Health(100, 100), new Owner(owner, owner)); return id;
        }
        @Override public void close() { data.close(); }
    }

    private static Creature definition(short id) {
        Creature creature = CreatureSleepFixtures.sleeper(1);
        creature.setCreatureId(id);
        creature.getAttributes().setHp(100);
        creature.setSpells(List.of());
        creature.setMeleeAttackType(Creature.AttackType.NONE);
        return creature;
    }
    private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> handler.apply(method.getName(), args)));
    }
}
