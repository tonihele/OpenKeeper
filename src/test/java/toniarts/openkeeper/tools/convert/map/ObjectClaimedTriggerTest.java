/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.AbstractRoomObjectControl;
import toniarts.openkeeper.game.trigger.object.ObjectTriggerLogicController;
import toniarts.openkeeper.utils.Point;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObjectClaimedTriggerTest {

    @ParameterizedTest
    @CsvSource({"2,3,1", "2,1,1", "2,2,0", "3,4,0", "3,2,0", "1,3,0"})
    void onlyClaimsFromNeutralRunTheObjectScriptBeforeItsOwnerChanges(int oldOwner, int newOwner, int events) {
        try (Fixture f = new Fixture((short) oldOwner, 255)) {
            f.objects.setOwner(f.entity, (short) newOwner);
            assertEquals(events, f.flags[0]);
            assertEquals(events == 1 ? List.of((short) 2) : List.of(), f.ownersAtClaim);
            assertEquals(newOwner, f.data.getComponent(f.entity, Owner.class).ownerId);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void returningToNeutralCanGenerateAnotherClaimSubjectToRepeatLimits(int repeat, int expected) {
        try (Fixture f = new Fixture((short) 2, repeat)) {
            for (int i = 0; i < 3; i++) {
                f.objects.setOwner(f.entity, (short) 3);
                f.objects.setOwner(f.entity, (short) 2);
            }
            assertEquals(expected, f.flags[0]);
        }
    }

    @Test
    void ordinaryTicksDoNotTreatOwnershipAsAPersistentClaimCondition() {
        try (Fixture f = new Fixture((short) 3, 255)) {
            f.ticks(12);
            assertEquals(0, f.flags[0]);
            f.objects.setOwner(f.entity, (short) 2);
            f.objects.setOwner(f.entity, (short) 3);
            f.ticks(12);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void controlOnlyAndUnchangedAssignmentsDoNotFire() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            f.data.setComponent(f.entity, new Owner((short) 2, (short) 4));
            f.objects.setOwner(f.entity, (short) 2);
            f.objects.setOwner(f.entity, (short) 2);
            assertEquals(0, f.flags[0]);
            assertEquals(2, f.data.getComponent(f.entity, Owner.class).controlId);
        }
    }

    @Test
    void nestedConditionsAreCheckedAtClaimTimeWithoutRetainingFailedClaims() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            f.records.put(1, condition(1, OBJECT_CLAIMED, 2, 0, 255));
            f.records.put(2, condition(2, FLAG, -1, 0, 255));
            f.bind();
            f.objects.setOwner(f.entity, (short) 3);
            assertEquals(0, f.flags[0]);
            f.flags[1] = 1;
            f.ticks(12);
            assertEquals(0, f.flags[0]);
            f.objects.setOwner(f.entity, (short) 2);
            f.objects.setOwner(f.entity, (short) 3);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void allMatchingRootBranchesRun() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            f.records.put(1, condition(1, OBJECT_CLAIMED, -1, 2, 255));
            f.records.put(2, condition(2, OBJECT_CLAIMED, -2, 0, 255));
            f.records.put(-2, action(-2, 1));
            f.bind();
            f.objects.setOwner(f.entity, (short) 3);
            assertEquals(1, f.flags[0]);
            assertEquals(1, f.flags[1]);
        }
    }

    @Test
    void claimDispatchPreservesTheOrdinaryTraversalCursor() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            f.records.put(1, condition(1, FLAG, -2, 2, 255));
            f.records.put(-2, action(-2, 1));
            f.records.put(2, condition(2, OBJECT_CLAIMED, -1, 0, 255));
            f.bind();
            f.flags[1] = 1;
            f.ticks(1);
            f.objects.setOwner(f.entity, (short) 3);
            assertEquals(1, f.flags[0]);
            assertEquals(1, f.flags[1]);
            f.ticks(1);
            assertEquals(2, f.flags[1]);
        }
    }

    @Test
    void unregisteredBindingsStillChangeOwnerWithoutFiringAnotherObjectsScript() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            EntityId other = f.object((short) 2, 999);
            f.objects.setOwner(other, (short) 3);
            assertEquals(0, f.flags[0]);
            assertEquals(3, f.data.getComponent(other, Owner.class).ownerId);
        }
    }

    @Test
    void newlyCreatedObjectsCanClaimBeforeTheNextBindingTick() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            EntityId spawned = f.object((short) 2, 1);
            f.objects.setOwner(spawned, (short) 3);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void aClaimActionThatRemovesItsObjectDoesNotResurrectIt() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            f.removeOnClaim = true;
            f.objects.setOwner(f.entity, (short) 3);
            assertEquals(1, f.flags[0]);
            assertNull(f.data.getComponent(f.entity, Owner.class));
            assertNull(f.data.getComponent(f.entity, ObjectComponent.class));
        }
    }

    @Test
    void objectsWithoutAnOwnerAndNonObjectsDoNotGenerateClaims() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            EntityId noOwner = f.object((short) 2, 1);
            f.data.removeComponent(noOwner, Owner.class);
            f.objects.setOwner(noOwner, (short) 3);
            EntityId creature = f.data.createEntity();
            f.data.setComponent(creature, new Owner((short) 2, (short) 2));
            f.objects.setOwner(creature, (short) 3);
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void depositingANeutralObjectIntoOwnedStorageFiresItsClaim() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            Storage storage = f.storage((short) 3);
            storage.addItem(f.entity, new Point(1, 1));
            assertEquals(1, f.flags[0]);
            assertEquals(3, f.data.getComponent(f.entity, Owner.class).ownerId);
            storage.destroy();
        }
    }

    @Test
    void capturingNeutralStorageFiresButLaterKeeperTransfersDoNot() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            Storage storage = f.storage((short) 2);
            storage.addItem(f.entity, new Point(1, 1));
            assertEquals(0, f.flags[0]);
            storage.captured((short) 3);
            assertEquals(1, f.flags[0]);
            storage.captured((short) 4);
            assertEquals(1, f.flags[0]);
            assertEquals(4, f.data.getComponent(f.entity, Owner.class).ownerId);
            storage.destroy();
        }
    }

    @Test
    void storageControlWithoutOwnershipIsNotAClaim() {
        try (Fixture f = new Fixture((short) 2, 255)) {
            Storage storage = f.storage((short) 3);
            storage.controlOnly(f.entity);
            assertEquals(0, f.flags[0]);
            assertEquals(2, f.data.getComponent(f.entity, Owner.class).ownerId);
            assertEquals(3, f.data.getComponent(f.entity, Owner.class).controlId);
            storage.destroy();
        }
    }

    private static final class Storage extends AbstractRoomObjectControl<EntityId> {
        Storage(IKwdFile kwd, IRoomController room, EntityData data) {
            super(kwd, room, data, () -> 0, ObjectType.SPELL_BOOK);
        }
        @Override protected int getObjectsPerTile() { return 2; }
        @Override protected int getNumberOfAccessibleTiles() { return 1; }
        @Override public EntityId addItem(EntityId item, Point point) {
            objectsByCoordinate.put(point, new ArrayList<>(List.of(item)));
            setRoomStorageToItem(item, true);
            return null;
        }
        void controlOnly(EntityId item) {
            setRoomStorageToItem(item, false);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final EntityId entity;
        final int[] flags = new int[2];
        final List<Short> ownersAtClaim = new ArrayList<>();
        final Map<Integer, Trigger> records = new HashMap<>();
        final IKwdFile kwd;
        final ILevelInfo level;
        final IGameController game;
        final ObjectsController objects;
        ObjectTriggerLogicController scripts;
        boolean loading = true;
        boolean removeOnClaim;

        Fixture(short owner, int repeat) {
            records.put(1, condition(1, OBJECT_CLAIMED, -1, 0, repeat));
            records.put(-1, action(-1, 0));
            entity = object(owner, 1);
            GameObject definition = new GameObject();
            definition.setFlags(EnumSet.noneOf(GameObject.ObjectFlag.class));
            Thing.Object thing = new Thing.Object();
            thing.setTriggerId(1);
            thing.setPlayerId(owner);
            kwd = proxy(IKwdFile.class, (name, args) -> switch (name) {
                case "getTriggers" -> records;
                case "getThings" -> loading ? List.of() : List.of(thing);
                case "getObject" -> definition;
                default -> throw new UnsupportedOperationException(name);
            });
            level = proxy(ILevelInfo.class, (name, args) -> switch (name) {
                case "getLevelData" -> kwd;
                case "getFlag" -> flags[((Number) args[0]).intValue()];
                case "setFlag" -> { flags[((Number) args[0]).intValue()] = (int) args[1]; yield null; }
                default -> throw new UnsupportedOperationException(name);
            });
            IGameWorldController world = proxy(IGameWorldController.class, (name, args) -> null);
            game = proxy(IGameController.class, (name, args) -> switch (name) {
                case "getGameWorldController" -> world;
                case "onObjectClaimed" -> {
                    EntityId id = (EntityId) args[0];
                    ownersAtClaim.add(data.getComponent(id, Owner.class).ownerId);
                    scripts.onClaimed(id);
                    if (removeOnClaim) {
                        data.removeEntity(id);
                    }
                    yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            objects = new ObjectsController(kwd, data, Map.of(), () -> 0, game, level);
            loading = false;
            bind();
        }

        EntityId object(short owner, int binding) {
            EntityId id = data.createEntity();
            data.setComponents(id, new ObjectComponent((short) 1, ObjectType.SPELL_BOOK),
                    new Owner(owner, owner), new toniarts.openkeeper.game.component.Trigger(binding));
            return id;
        }

        Storage storage(short owner) {
            EntityId roomId = data.createEntity();
            IRoomController room = proxy(IRoomController.class, (name, args) -> switch (name) {
                case "getEntityId" -> roomId;
                case "getOwnerId" -> owner;
                case "setObjectOwner" -> { objects.setOwner((EntityId) args[0], (Short) args[1]); yield null; }
                default -> throw new UnsupportedOperationException(name);
            });
            return new Storage(kwd, room, data);
        }

        void bind() {
            scripts = new ObjectTriggerLogicController(game, level, null, null, null, null, data, objects);
        }

        void ticks(int count) {
            for (int i = 0; i < count; i++) {
                scripts.processTick(.25f);
            }
        }

        @Override public void close() {
            scripts.stop();
            data.close();
        }
    }

    private static TriggerGeneric condition(int id, TriggerGeneric.TargetType type, int child, int next, int repeats) {
        TriggerGeneric condition = new TriggerGeneric(null);
        condition.setId(id);
        condition.setType(type);
        condition.setIdChild(child);
        condition.setIdNext(next);
        condition.setRepeatTimes((short) repeats);
        if (type == FLAG) {
            condition.setTargetValueComparison(TriggerGeneric.ComparisonType.EQUAL_TO);
            condition.setUserData("targetId", (short) 1);
            condition.setUserData("flag", (short) 1);
            condition.setUserData("value", 1);
        }
        return condition;
    }

    private static TriggerAction action(int id, int flag) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", (short) flag);
        action.setUserData("flag", (short) 18);
        action.setUserData("value", 1);
        return action;
    }

    @FunctionalInterface private interface Call { Object invoke(String name, Object[] args); }
    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> call.invoke(method.getName(), args)));
    }
}
