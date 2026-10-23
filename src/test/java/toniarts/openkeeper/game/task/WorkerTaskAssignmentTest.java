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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Handing out tasks to idle workers (#671). Finding the spot to work from
 * means path finding, so it should only be done for the tasks the workers are
 * about to get, not for every task while picking the closest.
 */
class WorkerTaskAssignmentTest {

    private static final short PLAYER_ID = 3;
    private static final AtomicLong IDS = new AtomicLong();

    private final EntityData entityData = new DefaultEntityData();
    private final AtomicInteger targetLookups = new AtomicInteger();

    @Test
    void choosingTasksDoesNotPathFindToEveryTask() {
        TaskManager taskManager = taskManager();
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Task task = task(new Point(10 + i, 10), creature -> true);
            tasks.add(task);
            taskManager.addTask(PLAYER_ID, task);
        }
        Map<ICreatureController, Task> assigned = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            ICreatureController imp = imp(new Point(i, 10));
            taskManager.addToUnemployedWorkerQueue(imp, gotWork -> {
            });
            assigned.put(imp, null);
        }

        taskManager.processUnemployedWorkerQueue();

        assertEquals(0, targetLookups.get(), "Target lookups while scoring");
        assertEquals(3, assignedCount(tasks));
    }

    @Test
    void unreachableTaskIsSkippedForTheNextClosest() {
        TaskManager taskManager = taskManager();
        Task unreachable = task(new Point(2, 10), creature -> false);
        Task reachable = task(new Point(5, 10), creature -> true);
        taskManager.addTask(PLAYER_ID, unreachable);
        taskManager.addTask(PLAYER_ID, reachable);
        List<Boolean> results = new ArrayList<>();
        taskManager.addToUnemployedWorkerQueue(imp(new Point(1, 10)), results::add);

        taskManager.processUnemployedWorkerQueue();

        assertEquals(List.of(Boolean.TRUE), results);
        assertEquals(0, ((TestTask) Proxy.getInvocationHandler(unreachable)).assignees.get());
        assertEquals(1, ((TestTask) Proxy.getInvocationHandler(reachable)).assignees.get());
    }

    private static int assignedCount(List<Task> tasks) {
        int count = 0;
        for (Task task : tasks) {
            count += ((TestTask) Proxy.getInvocationHandler(task)).assignees.get();
        }
        return count;
    }

    private TaskManager taskManager() {
        IMapDataInformation<?> emptyMap = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth", "getHeight" -> 0;
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> emptyMap;
            default -> null;
        });
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> switch (name) {
            case "getKeeper" -> keeper;
            default -> null;
        });
        IGameWorldController gameWorldController = stub(IGameWorldController.class, (name, args) -> null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> null);
        return new TaskManager(entityData, gameWorldController, mapController, null, null, null, List.of(player), levelInfo, null, Map.of());
    }

    private ICreatureController imp(Point location) {
        var entityId = entityData.createEntity();
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> location;
            case "getOwnerId" -> PLAYER_ID;
            case "getEntityId" -> entityId;
            default -> null;
        });
    }

    private Task task(Point location, Predicate<ICreatureController> reachable) {
        TestTask handler = new TestTask(location, reachable);
        Task task = (Task) Proxy.newProxyInstance(Task.class.getClassLoader(), new Class<?>[]{Task.class}, handler);
        return task;
    }

    /**
     * A task that counts how often it is asked for its target and its
     * assignees
     */
    private final class TestTask implements java.lang.reflect.InvocationHandler {

        private final Point location;
        private final Predicate<ICreatureController> reachable;
        private final long id = IDS.incrementAndGet();
        private final Instant created = Instant.now();
        private final AtomicInteger assignees = new AtomicInteger();

        private TestTask(Point location, Predicate<ICreatureController> reachable) {
            this.location = location;
            this.reachable = reachable;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "getTarget" -> {
                    targetLookups.incrementAndGet();
                    yield reachable.test((ICreatureController) args[0]) ? new Vector2f(location.x, location.y) : null;
                }
                case "getTaskLocation" -> location;
                case "getPriority" -> 100;
                case "getId" -> id;
                case "getTaskCreated" -> created;
                case "getAssigneeCount" -> assignees.get();
                case "isFull" -> assignees.get() >= 3;
                case "canAssign" -> assignees.get() < 3 && reachable.test((ICreatureController) args[0]);
                case "assign" -> {
                    assignees.incrementAndGet();
                    yield null;
                }
                case "getTaskType" -> TaskType.DIG_TILE;
                case "equals" -> proxy == args[0];
                case "hashCode" -> Long.hashCode(id);
                case "toString" -> "Test task at " + location;
                default -> null;
            };
        }
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " stub";
                    default -> answer.answer(method.getName(), args);
                });
    }

}
