/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.*;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.object.ObjectController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.controller.player.PlayerResearchControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.creature.Manufacture;
import toniarts.openkeeper.game.task.creature.Research;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;

/** Real task and object-controller interactions using only synthetic game definitions. */
class FurnitureUseTest {
    private static final short PLAYER = 3;
    private final EntityData data = new DefaultEntityData();
    private final Map<EntityId, GameObject> definitions = new HashMap<>();
    private final Set<EntityId> furniture = new HashSet<>();
    private short roomOwner = PLAYER;
    private final IObjectsController objects = stub(IObjectsController.class, (p, m, a) -> {
        if (m.getName().equals("createController")) {
            EntityId id = (EntityId) a[0];
            if (data.getComponent(id, ObjectComponent.class) == null) {
                throw new IllegalArgumentException("Removed object " + id);
            }
            return object(id);
        }
        return defaultValue(p, m, a);
    });
    private final IRoomController room = stub(IRoomController.class, (p, m, a) -> switch (m.getName()) {
        case "getFloorFurniture" -> furniture;
        case "getOwnerId" -> roomOwner;
        default -> defaultValue(p, m, a);
    });
    private final IMapDataInformation<?> map = stub(IMapDataInformation.class, FurnitureUseTest::defaultValue);
    private final IMapController mapController = stub(IMapController.class, (p, m, a) ->
            m.getName().equals("getMapData") ? map : defaultValue(p, m, a));
    private final TaskManager tasks = new TaskManager(data, stub(IGameWorldController.class, FurnitureUseTest::defaultValue), mapController, objects, null, null,
            List.of(), null, null, Map.of());

    @AfterEach void close() {
        tasks.stop();
        data.close();
    }

    @Test void sawStartsOnFirstExecutionBeforeTheFirstManufacturingSecondAndStopsOnUnassign() {
        EntityId bench = furniture((short) 46, 4, 4);
        EntityId other = furniture((short) 46, 8, 4);
        ICreatureController worker = worker();
        Manufacture task = manufacture();
        task.assign(worker, false);
        assertEquals(GameObject.State.NONE, state(bench));
        assertNull(data.getComponent(worker.getEntityId(), FurnitureUse.class));

        task.executeTask(worker, 0.25f);
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        assertEquals(GameObject.State.NONE, state(other));
        assertEquals(bench, data.getComponent(worker.getEntityId(), FurnitureUse.class).objectEntityId);
        FurnitureUse use = data.getComponent(worker.getEntityId(), FurnitureUse.class);
        ObjectViewState view = data.getComponent(bench, ObjectViewState.class);
        task.executeTask(worker, 0.5f);
        assertSame(use, data.getComponent(worker.getEntityId(), FurnitureUse.class));
        assertSame(view, data.getComponent(bench, ObjectViewState.class));

        task.unassign(worker);
        task.unassign(worker);
        assertEquals(GameObject.State.NONE, state(bench));
        assertNull(data.getComponent(worker.getEntityId(), FurnitureUse.class));
        task.assign(worker, false);
        task.executeTask(worker, 0.25f);
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
    }

    @Test void sharedUsersAndFreshControllersPreserveActivityUntilTheLastUserLeaves() {
        EntityId bench = furniture((short) 46, 4, 4);
        ICreatureController a = worker();
        ICreatureController b = worker();
        object(bench).beginUse(a.getEntityId());
        object(bench).beginUse(b.getEntityId());
        object(bench).endUse(a.getEntityId());
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        object(bench).endUse(a.getEntityId());
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        object(bench).endUse(b.getEntityId());
        assertEquals(GameObject.State.NONE, state(bench));
    }

    @Test void staleTaskCannotStopReplacementFurnitureOnTheSameTile() {
        EntityId bench = furniture((short) 46, 4, 4);
        ICreatureController a = worker();
        Manufacture old = manufacture();
        old.assign(a, false);
        old.executeTask(a, .25f);
        data.removeEntity(bench);
        furniture.remove(bench);
        EntityId replacement = furniture((short) 46, 4, 4);
        object(replacement).beginUse(a.getEntityId());
        old.unassign(a);
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(replacement));
        assertEquals(replacement, data.getComponent(a.getEntityId(), FurnitureUse.class).objectEntityId);
        assertNull(data.getComponent(bench, ObjectViewState.class));
    }

    @Test void captureStopsOldRoomUseWithoutActivatingAnotherOwnersFurniture() {
        EntityId bench = furniture((short) 46, 4, 4);
        ICreatureController worker = worker();
        Manufacture task = manufacture();
        task.assign(worker, false);
        task.executeTask(worker, .25f);
        roomOwner = 4;
        task.executeTask(worker, .5f);
        assertEquals(GameObject.State.NONE, state(bench));
        assertNull(data.getComponent(worker.getEntityId(), FurnitureUse.class));
    }

    @Test void researchUsesSameLifecycleWithoutOverridingUnverifiedPresentation() {
        EntityId bookcase = furniture((short) 77, 4, 4); // synthetic ID, no native asset
        ObjectViewState original = data.getComponent(bookcase, ObjectViewState.class);
        Keeper keeper = new Keeper(false, PLAYER);
        ResearchableEntity research = new ResearchableEntity((short) 1, ResearchableType.DOOR);
        keeper.setCurrentResearch(research);
        Door door = ResearchFixtures.door((short) 1, Integer.MAX_VALUE);
        IKwdFile kwd = stub(IKwdFile.class, (p, m, a) -> switch (m.getName()) {
            case "getDoorById" -> door;
            default -> m.getReturnType() == List.class ? List.of() : defaultValue(p, m, a);
        });
        Research task = new Research(null, null, new Point(4, 4), PLAYER, room, tasks,
                new PlayerResearchControl(keeper, kwd), objects);
        ICreatureController worker = worker();
        task.assign(worker, false);
        assertNull(data.getComponent(worker.getEntityId(), FurnitureUse.class));
        task.executeTask(worker, .25f);
        assertEquals(5, research.getResearch());
        assertEquals(bookcase, data.getComponent(worker.getEntityId(), FurnitureUse.class).objectEntityId);
        assertSame(original, data.getComponent(bookcase, ObjectViewState.class));
        task.unassign(worker);
        assertNull(data.getComponent(worker.getEntityId(), FurnitureUse.class));
        assertSame(original, data.getComponent(bookcase, ObjectViewState.class));
    }

    @Test void sleepingAndRecuperatingUseTheBedFromStateEntryAndSurviveMissingTask() {
        EntityId bed = furniture((short) 77, 4, 4);
        EntityId id = worker().getEntityId();
        data.setComponent(id, new CreatureSleep(bed, 0, 0, 0));
        CreatureController creature = creature(id);
        creature.getStateMachine().changeState(CreatureState.SLEEPING);
        assertNull(data.getComponent(id, TaskComponent.class));
        assertEquals(bed, data.getComponent(id, FurnitureUse.class).objectEntityId);
        creature.getStateMachine().changeState(CreatureState.RECUPERATING);
        assertEquals(bed, data.getComponent(id, FurnitureUse.class).objectEntityId);
        creature.getStateMachine().changeState(CreatureState.FALLEN);
        assertNull(data.getComponent(id, FurnitureUse.class));
    }

    @Test void lostAiAndRemovalReleaseUseEvenIfTheTaskComponentIsAlreadyGone() {
        EntityId bench = furniture((short) 46, 4, 4);
        EntityId id = worker().getEntityId();
        object(bench).beginUse(id);
        creature(id).getStateMachine().changeState(null);
        assertEquals(GameObject.State.NONE, state(bench));
        object(bench).beginUse(id);
        new EntityController(id, data, objects, null).remove();
        assertEquals(GameObject.State.NONE, state(bench));
    }

    @Test void deadOrCarriedCreaturesCannotActivateFurniture() {
        EntityId bench = furniture((short) 46, 4, 4);
        EntityId id = worker().getEntityId();
        data.setComponent(id, new InHand());
        object(bench).beginUse(id);
        assertNull(data.getComponent(id, FurnitureUse.class));
        data.removeComponent(id, InHand.class);
        data.setComponent(id, new Death(0));
        object(bench).beginUse(id);
        assertEquals(GameObject.State.NONE, state(bench));
        assertNull(data.getComponent(id, FurnitureUse.class));
    }

    @Test void cleanupAfterDirectEntityRemovalRecomputesRemainingUsers() {
        EntityId bench = furniture((short) 46, 4, 4);
        EntityId a = worker().getEntityId();
        EntityId b = worker().getEntityId();
        object(bench).beginUse(a);
        object(bench).beginUse(b);
        data.removeEntity(a);
        object(bench).endUse(a);
        assertEquals(GameObject.State.ACTIVATE_ANIM, state(bench));
        data.removeEntity(b);
        object(bench).endUse(b);
        assertEquals(GameObject.State.NONE, state(bench));
    }

    @Test void ambiguousFurnitureIsNotBoundByIterationOrder() {
        furniture((short) 46, 4, 4);
        furniture((short) 46, 4, 4);
        Manufacture task = manufacture();
        ICreatureController worker = worker();
        task.assign(worker, false);
        task.executeTask(worker, .25f);
        assertNull(data.getComponent(worker.getEntityId(), FurnitureUse.class));
    }

    private CreatureController creature(EntityId id) {
        return new CreatureController(id, data, null, null, null, null, Map.of(), null, null,
                null, null, objects, null, null);
    }

    private Manufacture manufacture() {
        PlayerManufactureControl manufacture = new PlayerManufactureControl();
        manufacture.setTrapToManufacture(TrapFixtures.trapToManufacture((short) 1, Integer.MAX_VALUE));
        return new Manufacture(null, null, new Point(4, 4), PLAYER, room, tasks, manufacture, objects);
    }

    private EntityId furniture(short type, int x, int y) {
        EntityId id = data.createEntity();
        GameObject definition = new GameObject();
        definition.setObjectId(type);
        definitions.put(id, definition);
        data.setComponents(id, new ObjectComponent(type, null), new Owner((short) 0, (short) 0),
                new Position(0, new Vector3f(x, 0, y)),
                new ObjectViewState(type, GameObject.State.NONE, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true));
        furniture.add(id);
        return id;
    }

    private IObjectController object(EntityId id) {
        return new ObjectController(id, data, definitions.get(id), objects, null);
    }

    private ICreatureController worker() {
        EntityId id = data.createEntity();
        data.setComponents(id, new Owner(PLAYER, PLAYER), new Position(0, new Vector3f(4.5f, 0, 4)));
        return stub(ICreatureController.class, (p, m, a) -> switch (m.getName()) {
            case "getEntityId" -> id;
            case "getOwnerId" -> PLAYER;
            case "getManufacturePerSecond" -> 60;
            case "getResearchPerTick" -> 5;
            default -> defaultValue(p, m, a);
        });
    }

    private GameObject.State state(EntityId id) {
        return data.getComponent(id, ObjectViewState.class).state;
    }

    private static Object defaultValue(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> {
                Class<?> type = method.getReturnType();
                if (type == boolean.class) yield false;
                if (type == short.class) yield (short) 0;
                if (type == int.class) yield 0;
                if (type == long.class) yield 0L;
                if (type == float.class) yield 0f;
                yield null;
            }
        };
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
