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
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ITrapsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmTrapTaskTest {

    private static final short SENTRY = 1;
    private static final short PLAYER = 3;
    private static final Point WORKSHOP_TILE = new Point(5, 5);
    private static final Point BLUEPRINT_TILE = new Point(12, 8);

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> armed = new ArrayList<>();
    private final List<EntityId> takenFromStorage = new ArrayList<>();
    private final List<TaskType> shownTasks = new ArrayList<>();
    private EntityId blueprint;
    private EntityId crate;
    private ICreatureController imp;
    private ArmTrapTask task;

    @BeforeEach
    void setUp() {
        blueprint = entityData.createEntity();
        entityData.setComponent(blueprint, new TrapViewState(SENTRY, true));
        crate = entityData.createEntity();
        entityData.setComponents(crate, new Crate(ResearchableType.TRAP, SENTRY), at(WORKSHOP_TILE),
                new Stored(entityData.createEntity(), ObjectType.CRATE));
        imp = creature(entityData.createEntity());
        task = new ArmTrapTask(null, mapController(), entityData, trapsController(), objectsController(), blueprint, BLUEPRINT_TILE,
                crate, PLAYER);
        task.assign(imp, false);
    }

    @Test
    void impFetchesTheCrateFromTheWorkshopAndDeliversIt() {
        assertEquals(WORKSHOP_TILE, task.getTaskLocation());
        assertEquals(TaskType.FETCH_TRAP_CRATE, task.getTaskType());

        task.executeTask(imp, 0);

        assertEquals(List.of(crate), takenFromStorage);
        assertEquals(imp.getEntityId(), entityData.getComponent(crate, HauledBy.class).entityId);
        assertEquals(BLUEPRINT_TILE, task.getTaskLocation());
        assertEquals(List.of(TaskType.DELIVER_TRAP_CRATE), shownTasks);
        assertTrue(task.isValid(imp));
    }

    @Test
    void trapIsArmedAsSoonAsTheCrateArrives() {
        task.executeTask(imp, 0);
        assertTrue(armed.isEmpty());

        task.executeTask(imp, 0);

        assertEquals(List.of(blueprint), armed);
        assertNull(entityData.getComponent(crate, Crate.class));
        assertTrue(task.isRemovable());
    }

    @Test
    void droppedCrateWaitsToBeFetchedAgain() {
        task.executeTask(imp, 0);
        entityData.setComponent(crate, at(new Point(9, 7)));

        // Picked up by the hand on the way
        task.unassign(imp);

        assertNull(entityData.getComponent(crate, HauledBy.class));
        assertEquals(TaskType.FETCH_TRAP_CRATE, task.getTaskType());
        assertEquals(new Point(9, 7), task.getTaskLocation());
        assertFalse(task.isRemovable());
    }

    @Test
    void crateLyingOnTheFloorIsPickedUpWithoutAStorage() {
        entityData.removeComponent(crate, Stored.class);
        entityData.setComponent(crate, at(new Point(2, 2)));

        task.executeTask(imp, 0);

        assertTrue(takenFromStorage.isEmpty());
        assertNotNull(entityData.getComponent(crate, HauledBy.class));
    }

    @Test
    void notValidWhileSomebodyElseCarriesTheCrate() {
        entityData.setComponent(crate, new HauledBy(entityData.createEntity()));

        assertFalse(task.isValid(imp));
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
            case "updateAssignedTask" -> {
                shownTasks.add(((ArmTrapTask) args[0]).getTaskType());
                yield null;
            }
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IMapController mapController() {
        IRoomObjectControl<?> crates = (IRoomObjectControl<?>) Proxy.newProxyInstance(IRoomObjectControl.class.getClassLoader(),
                new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> switch (method.getName()) {
            case "removeItem" -> {
                takenFromStorage.add((EntityId) args[0]);
                entityData.removeComponent((EntityId) args[0], Stored.class);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IRoomController workshop = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getObjectControl" -> args[0] == ObjectType.CRATE ? crates : null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getRoomControllerByCoordinates" -> WORKSHOP_TILE.equals(args[0]) ? workshop : null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private ITrapsController trapsController() {
        return (ITrapsController) Proxy.newProxyInstance(ITrapsController.class.getClassLoader(), new Class<?>[]{ITrapsController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "armTrap" -> {
                armed.add((EntityId) args[0]);
                entityData.setComponent((EntityId) args[0], new TrapViewState(SENTRY, false));
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
