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
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Thing;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A task made for one creature, such as a hero's objective, is forgotten once
 * the creature lets go of it. Before, every such task was kept for the whole
 * game and a long game ran out of memory
 */
class TaskManagerUnusedTaskTest {

    private static final short PLAYER_ID = 3;
    private static final long QUEUED_TASK_ID = -1;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId creatureId = entityData.createEntity();
    private final ICreatureController creature = createCreature();
    private final TaskManager taskManager = createTaskManager();

    @Test
    void taskIsForgottenWhenItsCreatureLetsGo() {
        assertTrue(taskManager.assignObjectiveTask(creature, Thing.HeroParty.Objective.SEND_TO_ACTION_POINT));
        long taskId = entityData.getComponent(creatureId, TaskComponent.class).taskId;
        taskManager.processTick(0.25f);
        assertNotNull(taskManager.getTaskById(taskId));

        entityData.removeComponent(creatureId, TaskComponent.class);
        taskManager.processTick(0.25f);

        assertNull(taskManager.getTaskById(taskId));
    }

    @Test
    void queuedTaskIsKept() {
        assertTrue(taskManager.assignObjectiveTask(creature, Thing.HeroParty.Objective.SEND_TO_ACTION_POINT));
        taskManager.processTick(0.25f);
        entityData.removeComponent(creatureId, TaskComponent.class);
        taskManager.processTick(0.25f);

        // Nobody has it yet, but it waits in the queue for a worker
        assertNotNull(taskManager.getTaskById(QUEUED_TASK_ID));
    }

    private ICreatureController createCreature() {
        entityData.setComponent(creatureId, new CreatureComponent());
        return stub(ICreatureController.class, (proxy, method, args) -> {
            if (method.getName().equals("setAssignedTask")) {
                Task task = (Task) args[0];
                entityData.setComponent(creatureId, new TaskComponent(task.getId(), null, null, task.getTaskType(), 0f, false));
                return null;
            }
            return defaultValue(proxy, method);
        });
    }

    private TaskManager createTaskManager() {
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (proxy, method, args) -> method.getName().equals("getKeeper") ? keeper : defaultValue(proxy, method));
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, TaskManagerUnusedTaskTest::defaultHandler);
        IMapController mapController = stub(IMapController.class, (proxy, method, args) -> method.getName().equals("getMapData") ? mapData : defaultValue(proxy, method));
        ICreaturesController creaturesController = stub(ICreaturesController.class, (proxy, method, args) -> method.getName().equals("createController") ? creature : defaultValue(proxy, method));
        ActionPoint actionPoint = new ActionPoint(new Thing.ActionPoint());
        ILevelInfo levelInfo = stub(ILevelInfo.class, (proxy, method, args) -> method.getName().equals("getActionPoint") ? actionPoint : defaultValue(proxy, method));
        TaskManager manager = new TaskManager(entityData, stub(IGameWorldController.class, TaskManagerUnusedTaskTest::defaultHandler),
                mapController, stub(IObjectsController.class, TaskManagerUnusedTaskTest::defaultHandler), creaturesController,
                stub(INavigationService.class, TaskManagerUnusedTaskTest::defaultHandler), List.of(player), levelInfo,
                stub(IEntityPositionLookup.class, TaskManagerUnusedTaskTest::defaultHandler), Map.of());
        manager.addTask(PLAYER_ID, stub(Task.class, (proxy, method, args) -> method.getName().equals("getId") ? QUEUED_TASK_ID : defaultValue(proxy, method)));
        return manager;
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
