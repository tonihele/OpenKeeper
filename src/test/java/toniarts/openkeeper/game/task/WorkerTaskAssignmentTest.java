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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.JobPicked;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.worker.ClaimTileTask;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private final AtomicInteger reachabilityChecks = new AtomicInteger();
    private final Set<Point> land = new HashSet<>();
    private final Set<Point> gold = new HashSet<>();

    // No tiles to scan for tasks at the start, but every tile can be looked up
    private final IMapDataInformation<?> map = stub(IMapDataInformation.class, (name, args) -> switch (name) {
        case "getWidth", "getHeight" -> 0;
        case "getTile" -> args.length == 1 ? tile((Point) args[0]) : tile(new Point((int) args[0], (int) args[1]));
        default -> null;
    });
    private final IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
        case "getMapData" -> map;
        case "isClaimableTile" -> true;
        default -> null;
    });
    private final INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
        case "isAccessible" -> land.contains(((IMapTileInformation) args[1]).getLocation());
        case "isReachable" -> isOnLand((Point) args[0], (Point) args[1]);
        case "findPath" -> new DefaultGraphPath<>();
        default -> null;
    });

    @Test
    void choosingTasksDoesNotPathFindToEveryTask() {
        addLand(0, 20, 10);
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
        addLand(0, 20, 10);
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

    @Test
    void tasksOutOfTheWorkersReachAreNotPathFoundTo() {

        // The imp's land and, past the rock at 8, an island of the keeper's land it can't walk to. The island's walls
        // are closer than the job on the imp's own land
        addLand(0, 7, 10);
        addLand(9, 12, 10);
        TaskManager taskManager = taskManager();
        List<Task> islandTasks = new ArrayList<>();
        for (int x = 9; x <= 13; x++) {
            Task task = task(new Point(x, 10), creature -> false);
            islandTasks.add(task);
            taskManager.addTask(PLAYER_ID, task);
        }
        Task reachable = task(new Point(0, 10), creature -> true);
        taskManager.addTask(PLAYER_ID, reachable);
        List<Boolean> results = new ArrayList<>();
        taskManager.addToUnemployedWorkerQueue(imp(new Point(7, 10)), results::add);

        taskManager.processUnemployedWorkerQueue();

        assertEquals(List.of(Boolean.TRUE), results);
        assertEquals(1, ((TestTask) Proxy.getInvocationHandler(reachable)).assignees.get());
        assertEquals(1, reachabilityChecks.get(), "Reachability checks");
        assertEquals(0, assignedCount(islandTasks));
    }

    @Test
    void workerWithOnlyTasksOutOfReachStaysIdleWithoutPathFinding() {
        addLand(0, 7, 10);
        addLand(9, 12, 10);
        TaskManager taskManager = taskManager();
        for (int x = 10; x <= 13; x++) {
            taskManager.addTask(PLAYER_ID, task(new Point(x, 10), creature -> false));
        }
        List<Boolean> results = new ArrayList<>();
        taskManager.addToUnemployedWorkerQueue(imp(new Point(7, 10)), results::add);

        taskManager.processUnemployedWorkerQueue();

        assertEquals(List.of(Boolean.FALSE), results);
        assertEquals(0, reachabilityChecks.get(), "Reachability checks");
    }

    @Test
    void taskNextToTheWorkersLandIsConsidered() {
        addLand(0, 7, 10);
        TaskManager taskManager = taskManager();
        Task wall = task(new Point(8, 10), creature -> true);
        taskManager.addTask(PLAYER_ID, wall);
        List<Boolean> results = new ArrayList<>();
        taskManager.addToUnemployedWorkerQueue(imp(new Point(1, 10)), results::add);

        taskManager.processUnemployedWorkerQueue();

        assertEquals(List.of(Boolean.TRUE), results);
        assertEquals(1, ((TestTask) Proxy.getInvocationHandler(wall)).assignees.get());
    }

    @Test
    void rescueOfACreatureThatIsGoneIsNotHandedOut() {
        addLand(0, 7, 10);
        TaskManager taskManager = taskManager();
        EntityId knockedOut = entityData.createEntity();
        entityData.setComponents(knockedOut, new CreatureComponent(), new Health(0, 100), new Owner(PLAYER_ID, PLAYER_ID),
                new Unconscious(0), new Position(0, new Vector3f(5, 0, 10)));
        taskManager.processTick(0.25f);

        // Nobody came, it died and its body is gone. The rescue has no place to go to anymore
        entityData.removeEntity(knockedOut);
        taskManager.processTick(0.25f);
        List<Boolean> results = new ArrayList<>();
        taskManager.addToUnemployedWorkerQueue(imp(new Point(1, 10)), results::add);

        taskManager.processUnemployedWorkerQueue();

        assertEquals(List.of(Boolean.FALSE), results);
    }

    @Test
    void fullTaskGoesToAWorkerItMakesRoomFor() {
        addLand(0, 20, 10);
        TaskManager taskManager = taskManager();
        Task task = task(new Point(5, 10), creature -> true);
        TestTask handler = (TestTask) Proxy.getInvocationHandler(task);
        handler.assignees.set(3);
        handler.makesRoom = true;
        taskManager.addTask(PLAYER_ID, task);
        List<Boolean> results = new ArrayList<>();
        taskManager.addToUnemployedWorkerQueue(imp(new Point(4, 10)), results::add);

        taskManager.processUnemployedWorkerQueue();

        assertEquals(List.of(Boolean.TRUE), results);
        assertEquals(1, handler.assignees.get());
    }

    @Test
    void blueprintJobIsTakenFromAnotherWorkerOnlyWhenNoneIsFree() {
        addLand(0, 20, 10);
        TaskManager taskManager = taskManager();
        Task held = task(new Point(5, 10), creature -> true);
        TestTask heldHandler = (TestTask) Proxy.getInvocationHandler(held);
        heldHandler.assignees.set(3);
        heldHandler.makesRoom = true;
        heldHandler.taskType = TaskType.FETCH_TRAP_CRATE;
        Task free = task(new Point(15, 10), creature -> true);
        ((TestTask) Proxy.getInvocationHandler(free)).taskType = TaskType.FETCH_DOOR_CRATE;
        taskManager.addTask(PLAYER_ID, held);
        taskManager.addTask(PLAYER_ID, free);
        taskManager.addToUnemployedWorkerQueue(imp(new Point(4, 10)), result -> {
        });

        taskManager.processUnemployedWorkerQueue();

        // DK II looks for a free blueprint job first, however far (0x464A80)
        assertEquals(3, heldHandler.assignees.get());
        assertEquals(1, free.getAssigneeCount());
    }

    @Test
    void workerOnItsWayClaimsTheFloorThatDiggingHasLeft() {

        // An imp walking to its digging steps on dug floor: it claims it there and lets go of the digging, which
        // another imp can take (DKII.EXE 0x464170, 0x469600)
        addLand(0, 20, 10);
        TaskManager taskManager = taskManager();
        ClaimTileTask dugFloor = new ClaimTileTask(navigationService, mapController, new Point(5, 10), PLAYER_ID, true);
        taskManager.addTask(PLAYER_ID, dugFloor);
        AtomicReference<Task> work = new AtomicReference<>(work(TaskType.DIG_TILE, new Point(12, 10)));
        ICreatureController imp = walkingImp(new Point(5, 10), work);

        assertTrue(taskManager.assignDugFloorOnTheWay(imp));
        assertEquals(dugFloor, work.get());
        assertEquals(1, dugFloor.getAssigneeCount());
        assertEquals(TaskType.CLAIM_TILE, entityData.getComponent(imp.getEntityId(), JobPicked.class).work, "It carries on claiming");
    }

    @Test
    void dugFloorWithinTwoTilesOfTheWorkIsLeftForLater() {
        addLand(0, 20, 10);
        TaskManager taskManager = taskManager();
        taskManager.addTask(PLAYER_ID, new ClaimTileTask(navigationService, mapController, new Point(5, 10), PLAYER_ID, true));

        assertFalse(taskManager.assignDugFloorOnTheWay(walkingImp(new Point(5, 10),
                new AtomicReference<>(work(TaskType.DIG_TILE, new Point(7, 9))))));
        assertTrue(taskManager.assignDugFloorOnTheWay(walkingImp(new Point(5, 10),
                new AtomicReference<>(work(TaskType.CLAIM_WALL, new Point(8, 9))))));
    }

    @Test
    void onlyDugFloorIsClaimedOnTheWayAndOnlyToDiggingClaimingWallsOrGold() {
        addLand(0, 20, 10);
        TaskManager taskManager = taskManager();
        taskManager.addTask(PLAYER_ID, new ClaimTileTask(navigationService, mapController, new Point(5, 10), PLAYER_ID, false));
        taskManager.addTask(PLAYER_ID, new ClaimTileTask(navigationService, mapController, new Point(6, 10), PLAYER_ID, true));

        assertFalse(taskManager.assignDugFloorOnTheWay(walkingImp(new Point(5, 10),
                new AtomicReference<>(work(TaskType.DIG_TILE, new Point(12, 10))))), "Other floor is claimed in its turn");
        assertFalse(taskManager.assignDugFloorOnTheWay(walkingImp(new Point(6, 10),
                new AtomicReference<>(work(TaskType.FETCH_TRAP_CRATE, new Point(12, 10))))), "Not on the way to a crate");
    }

    @Test
    void minerBackFromTheTreasuryGoesOnAlongTheGold() {

        // The imp mined the gold at 3,10 and stands at the treasury at 20,10. The gold next to its last gold tile comes
        // before the gold next to it, the other digging is measured from where it stands (DKII.EXE 0x469AF0)
        addLand(0, 20, 10);
        TaskManager taskManager = taskManager();
        Task alongTheSeam = task(new Point(6, 10), creature -> true);
        Task nearTheTreasury = task(new Point(16, 10), creature -> true);
        gold.addAll(List.of(new Point(6, 10), new Point(16, 10)));
        taskManager.addTask(PLAYER_ID, alongTheSeam);
        taskManager.addTask(PLAYER_ID, nearTheTreasury);
        ICreatureController miner = imp(new Point(20, 10));
        JobPicked mined = new JobPicked(TaskType.DIG_TILE, null);
        mined.goldTile = new Point(3, 10);
        entityData.setComponent(miner.getEntityId(), mined);
        taskManager.addToUnemployedWorkerQueue(miner, gotWork -> {
        });

        taskManager.processUnemployedWorkerQueue();

        assertEquals(1, ((TestTask) Proxy.getInvocationHandler(alongTheSeam)).assignees.get());
        assertEquals(new Point(6, 10), entityData.getComponent(miner.getEntityId(), JobPicked.class).goldTile);

        // Dirt two tiles from the miner comes before the gold three tiles along the seam
        taskManager = taskManager();
        Task dirt = task(new Point(18, 10), creature -> true);
        taskManager.addTask(PLAYER_ID, task(new Point(9, 10), creature -> true));
        taskManager.addTask(PLAYER_ID, dirt);
        gold.add(new Point(9, 10));
        taskManager.addToUnemployedWorkerQueue(miner, gotWork -> {
        });

        taskManager.processUnemployedWorkerQueue();

        assertEquals(1, ((TestTask) Proxy.getInvocationHandler(dirt)).assignees.get(), "Dirt next to the miner");
        assertNull(entityData.getComponent(miner.getEntityId(), JobPicked.class).goldTile);
    }

    private void addLand(int fromX, int toX, int y) {
        for (int x = fromX; x <= toX; x++) {
            land.add(new Point(x, y));
        }
    }

    /**
     * The land is a row, walking goes along it tile by tile
     */
    private boolean isOnLand(Point start, Point end) {
        for (int x = Math.min(start.x, end.x); x <= Math.max(start.x, end.x); x++) {
            if (start.y != end.y || !land.contains(new Point(x, start.y))) {
                return false;
            }
        }
        return true;
    }

    private static int assignedCount(List<Task> tasks) {
        int count = 0;
        for (Task task : tasks) {
            count += ((TestTask) Proxy.getInvocationHandler(task)).assignees.get();
        }
        return count;
    }

    private TaskManager taskManager() {
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> switch (name) {
            case "getKeeper" -> keeper;
            default -> null;
        });
        IGameWorldController gameWorldController = stub(IGameWorldController.class, (name, args) -> null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "createController" -> knockedOutCreature((EntityId) args[0]);
            default -> null;
        });
        return new TaskManager(entityData, gameWorldController, mapController, null, creaturesController, navigationService, List.of(player), levelInfo, null, Map.of());
    }

    private IMapTileInformation tile(Point location) {
        if (location.x < 0 || location.y < 0) {
            return null;
        }
        return stub(IMapTileController.class, (name, args) -> switch (name) {
            case "getLocation" -> location;
            case "getX" -> location.x;
            case "getY" -> location.y;
            case "isSelected" -> false;
            case "getGold" -> gold.contains(location) ? 1000 : 0;
            default -> null;
        });
    }

    private ICreatureController imp(Point location) {
        var entityId = entityData.createEntity();
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> location;
            case "getOwnerId" -> PLAYER_ID;
            case "getEntityId" -> entityId;
            case "getGold" -> 0;
            default -> null;
        });
    }

    /**
     * An imp on its way to its work, that takes the task it is given
     */
    private ICreatureController walkingImp(Point location, AtomicReference<Task> work) {
        var entityId = entityData.createEntity();
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> location;
            case "getPosition" -> new Vector3f(location.x + 0.3f, 0, location.y);
            case "getOwnerId" -> PLAYER_ID;
            case "getEntityId" -> entityId;
            case "getAssignedTask" -> work.get();
            case "setAssignedTask" -> {
                work.set((Task) args[0]);
                yield null;
            }
            default -> null;
        });
    }

    private static Task work(TaskType type, Point location) {
        return stub(Task.class, (name, args) -> switch (name) {
            case "getTaskType" -> type;
            case "getTaskLocation" -> location;
            default -> null;
        });
    }

    private ICreatureController knockedOutCreature(EntityId entityId) {
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getPosition" -> EntityController.getPosition(entityData, entityId);
            case "isUnconscious" -> entityData.getComponent(entityId, Unconscious.class) != null;
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
        private boolean makesRoom;
        private TaskType taskType = TaskType.DIG_TILE;

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
                case "canAssign" -> {
                    reachabilityChecks.incrementAndGet();
                    yield assignees.get() < 3 && reachable.test((ICreatureController) args[0]);
                }
                case "makeRoomFor" -> {
                    if (makesRoom) {
                        assignees.set(0);
                    }
                    yield makesRoom;
                }
                case "assign" -> {
                    assignees.incrementAndGet();
                    yield null;
                }
                case "getTaskType" -> taskType;
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
