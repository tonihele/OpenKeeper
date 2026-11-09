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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A creature that is killed while it has a task loses its entity outright. The
 * task manager has to let the task go all the same, and carry on with the
 * other tasks of the tick
 */
class TaskManagerDeletedTaskTest {

    private static final short PLAYER_ID = 3;
    private static final long TASK_ID = 77;
    private static final long OTHER_TASK_ID = 78;

    private final EntityData entityData = new DefaultEntityData();
    private final List<String> unassigned = new ArrayList<>();
    private final Map<EntityId, ICreatureController> controllers = new HashMap<>();
    private final TaskManager taskManager = createTaskManager();

    @Test
    void creatureRemovedWithItsTaskIsUnassigned() {
        EntityId creature = addCreatureWithTask(TASK_ID);
        taskManager.processTick(0.25f);

        entityData.removeEntity(creature);

        assertDoesNotThrow(() -> taskManager.processTick(0.25f));
        assertEquals(List.of(unassignment(TASK_ID, creature)), unassigned);
    }

    @Test
    void removalDoesNotSkipTheOtherTasksOfTheTick() {
        EntityId dead = addCreatureWithTask(TASK_ID);
        taskManager.processTick(0.25f);

        // In the same tick one creature dies and another one takes a task
        entityData.removeEntity(dead);
        EntityId other = addCreatureWithTask(OTHER_TASK_ID);
        assertDoesNotThrow(() -> taskManager.processTick(0.25f));

        // The new task was noted, so letting it go is seen later
        entityData.removeComponent(other, TaskComponent.class);
        taskManager.processTick(0.25f);
        assertEquals(List.of(unassignment(TASK_ID, dead), unassignment(OTHER_TASK_ID, other)), unassigned);
    }

    private static String unassignment(long taskId, EntityId creature) {
        return "task" + taskId + ":" + creature;
    }

    private EntityId addCreatureWithTask(long taskId) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id, new CreatureComponent(), new TaskComponent(taskId, null, new Point(1, 1), TaskType.GO_TO_LOCATION, 0f, false));
        controllers.put(id, stub(ICreatureController.class, (proxy, method, args) -> method.getName().equals("getEntityId") ? id : defaultValue(proxy, method)));
        return id;
    }

    private TaskManager createTaskManager() {
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (proxy, method, args) -> method.getName().equals("getKeeper") ? keeper : defaultValue(proxy, method));
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, method, args) -> defaultValue(proxy, method));
        IMapController mapController = stub(IMapController.class, (proxy, method, args) -> method.getName().equals("getMapData") ? mapData : defaultValue(proxy, method));
        ICreaturesController creaturesController = stub(ICreaturesController.class, (proxy, method, args) -> {
            if (method.getName().equals("createController")) {
                EntityId id = (EntityId) args[0];
                if (entityData.getComponent(id, CreatureComponent.class) == null) {
                    throw new RuntimeException("Entity " + id + " doesn't represent a creature!");
                }
                return controllers.get(id);
            }
            return defaultValue(proxy, method);
        });
        TaskManager manager = new TaskManager(entityData, stub(IGameWorldController.class, TaskManagerDeletedTaskTest::defaultHandler),
                mapController, stub(IObjectsController.class, TaskManagerDeletedTaskTest::defaultHandler), creaturesController,
                stub(INavigationService.class, TaskManagerDeletedTaskTest::defaultHandler), List.of(player),
                stub(ILevelInfo.class, TaskManagerDeletedTaskTest::defaultHandler),
                stub(IEntityPositionLookup.class, TaskManagerDeletedTaskTest::defaultHandler), Map.of());
        manager.addTask(PLAYER_ID, recordingTask(TASK_ID));
        manager.addTask(PLAYER_ID, recordingTask(OTHER_TASK_ID));
        return manager;
    }

    /**
     * A task that notes who it was taken away from
     */
    private Task recordingTask(long id) {
        return stub(Task.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" ->
                id;
            case "unassign" ->
                unassigned.add(unassignment(id, ((ICreatureController) args[0]).getEntityId()));
            case "toString" ->
                "task" + id;
            default ->
                defaultValue(proxy, method);
        });
    }

    private static Object defaultHandler(Object proxy, Method method, Object[] args) {
        return defaultValue(proxy, method);
    }

    private static Object defaultValue(Object proxy, Method method) {
        return switch (method.getName()) {
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                false;
            case "toString" ->
                "stub";
            default ->
                primitiveDefault(method.getReturnType());
        };
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) {
            return false;
        } else if (type == int.class) {
            return 0;
        } else if (type == long.class) {
            return 0L;
        } else if (type == float.class) {
            return 0f;
        } else if (type == short.class) {
            return (short) 0;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
