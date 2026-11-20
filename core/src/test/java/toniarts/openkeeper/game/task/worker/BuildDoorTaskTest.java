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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IDoorsController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildDoorTaskTest {

    private static final short WOODEN_DOOR = 1;
    private static final short PLAYER = 3;
    private static final Point CRATE_TILE = new Point(5, 5);
    private static final Point BLUEPRINT_TILE = new Point(12, 8);

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> built = new ArrayList<>();
    private final List<Task> queued = new ArrayList<>();
    private EntityId blueprint;
    private EntityId crate;
    private ICreatureController imp;
    private Task assigned;
    private BuildDoorTask task;

    @BeforeEach
    void setUp() {
        blueprint = entityData.createEntity();
        entityData.setComponent(blueprint, new DoorComponent(WOODEN_DOOR, false, true));
        crate = entityData.createEntity();
        entityData.setComponents(crate, new Crate(ResearchableType.DOOR, WOODEN_DOOR), at(CRATE_TILE));
        imp = creature(entityData.createEntity());
        task = new BuildDoorTask(taskManager(), null, null, entityData, doorsController(), objectsController(), blueprint,
                BLUEPRINT_TILE, crate, PLAYER);
        task.assign(imp, true);
    }

    @Test
    void impFetchesTheDoorCrateAndGetsTheDelivery() {
        assertEquals(CRATE_TILE, task.getTaskLocation());
        assertEquals(TaskType.FETCH_DOOR_CRATE, task.getTaskType());

        task.executeTask(imp, 0);

        assertEquals(imp.getEntityId(), entityData.getComponent(crate, HauledBy.class).entityId);
        assertEquals(List.of(assigned), queued);
        assertInstanceOf(DeliverDoorCrateTask.class, assigned);
        assertEquals(TaskType.DELIVER_DOOR_CRATE, assigned.getTaskType());
        assertEquals(BLUEPRINT_TILE, assigned.getTaskLocation());
        assertTrue(assigned.isValid(imp));

        // Nobody else fetches the crate while it is carried, but the blueprint still waits for it
        assertEquals(0, task.getAssigneeCount());
        assertFalse(task.isValid(creature(entityData.createEntity())));
        assertFalse(task.isRemovable());
    }

    @Test
    void doorIsBuiltAsSoonAsTheCrateArrives() {
        task.executeTask(imp, 0);
        assertTrue(built.isEmpty());

        assigned.executeTask(imp, 0);

        assertEquals(List.of(blueprint), built);
        assertNull(entityData.getComponent(crate, Crate.class));
        assertTrue(assigned.isRemovable());
        assertTrue(task.isRemovable());
    }

    @Test
    void droppedCrateWaitsToBeFetchedAgain() {
        task.executeTask(imp, 0);
        Task delivery = assigned;
        entityData.setComponent(crate, at(new Point(9, 7)));

        delivery.unassign(imp);

        assertNull(entityData.getComponent(crate, HauledBy.class));
        assertTrue(delivery.isRemovable());
        assertEquals(TaskType.FETCH_DOOR_CRATE, task.getTaskType());
        assertEquals(new Point(9, 7), task.getTaskLocation());
        assertTrue(task.isValid(imp));
        assertFalse(task.isRemovable());
    }

    @Test
    void soldBlueprintEndsTheTask() {
        entityData.removeEntity(blueprint);

        assertTrue(task.isRemovable());
        assertFalse(task.isValid(imp));
    }

    private static Position at(Point tile) {
        Vector3f position = WorldUtils.pointToVector3f(tile);
        position.y = WorldUtils.FLOOR_HEIGHT;
        return new Position(0, position);
    }

    private ICreatureController creature(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getEntityId" -> entityId;
            case "setAssignedTask" -> {

                // As the creature does, the task it had lets go of it
                if (assigned != null) {
                    assigned.unassign((ICreatureController) proxy);
                }
                assigned = (Task) args[0];
                yield null;
            }
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private ITaskManager taskManager() {
        return (ITaskManager) Proxy.newProxyInstance(ITaskManager.class.getClassLoader(), new Class<?>[]{ITaskManager.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "addTask" -> {
                queued.add((Task) args[1]);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IDoorsController doorsController() {
        return (IDoorsController) Proxy.newProxyInstance(IDoorsController.class.getClassLoader(), new Class<?>[]{IDoorsController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "buildDoor" -> {
                built.add((EntityId) args[0]);
                entityData.setComponent((EntityId) args[0], new DoorComponent(WOODEN_DOOR, false, false));
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "createController" -> objectController((EntityId) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IObjectController objectController(EntityId entityId) {
        return (IObjectController) Proxy.newProxyInstance(IObjectController.class.getClassLoader(), new Class<?>[]{IObjectController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "setHaulable" -> {
                if (args[0] == null) {
                    entityData.removeComponent(entityId, HauledBy.class);
                } else {
                    entityData.setComponent(entityId, new HauledBy(((ICreatureController) args[0]).getEntityId()));
                }
                yield null;
            }
            case "remove" -> {
                entityData.removeEntity(entityId);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

}
