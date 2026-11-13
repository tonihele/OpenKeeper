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

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DropArea;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An imp dropped next to a knocked out creature carries it off: its own to a
 * lair, an enemy to the prison
 */
class DroppedWorkerPicksUpBodyTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final int SIZE = 30;
    private static final Object NO_ANSWER = new Object();

    private final EntityData entityData = new DefaultEntityData();
    private final Map<EntityId, ICreatureController> creatures = new HashMap<>();
    private final Map<ICreatureController, Task> work = new HashMap<>();
    private final List<ICreatureController> released = new ArrayList<>();

    @Test
    void droppedImpTakesTheBodiesOverFromImpsFurtherAway() {

        // DK II (0x46D810): an imp closer to a body than the imp that has the job takes it over, the other one goes
        // back to looking for work
        TaskManager taskManager = bodiesNextTo(new Point(10, 10));
        List<ICreatureController> far = List.of(imp(new Point(25, 25)), imp(new Point(25, 26)));
        lookForWork(taskManager, far);
        assertEquals(Set.of(TaskType.RESCUE_CREATURE, TaskType.CAPTURE_ENEMY_CREATURE), workOf(far));
        taskManager.processTick(0.25f);

        List<ICreatureController> dropped = List.of(droppedImp(new Point(10, 10)), droppedImp(new Point(10, 10)));
        lookForWork(taskManager, dropped);

        assertEquals(Set.of(TaskType.RESCUE_CREATURE, TaskType.CAPTURE_ENEMY_CREATURE), workOf(dropped));
        assertTrue(released.containsAll(far), "The imps further away let go");
    }

    @Test
    void impFurtherAwayLeavesTheBodyToTheImpThatHasIt() {
        TaskManager taskManager = bodiesNextTo(new Point(10, 10));
        lookForWork(taskManager, List.of(imp(new Point(10, 12)), imp(new Point(12, 10))));
        taskManager.processTick(0.25f);

        ICreatureController farther = imp(new Point(20, 20));
        lookForWork(taskManager, List.of(farther));

        assertNull(work.get(farther));
        assertEquals(List.of(), released);
    }

    private Set<TaskType> workOf(List<ICreatureController> imps) {
        Set<TaskType> result = new HashSet<>();
        imps.forEach(imp -> result.add(work.containsKey(imp) ? work.get(imp).getTaskType() : null));
        return result;
    }

    private static void lookForWork(TaskManager taskManager, List<ICreatureController> imps) {
        imps.forEach(imp -> taskManager.addToUnemployedWorkerQueue(imp, gotWork -> {
        }));
        taskManager.processUnemployedWorkerQueue();
    }

    /**
     * A knocked out creature of the keeper's own, with a lair, and an enemy
     * one next to the given tile. The keeper has a prison with room
     */
    private TaskManager bodiesNextTo(Point tile) {
        TaskManager taskManager = taskManager();
        taskManager.start();
        body(new Point(tile.x + 1, tile.y), PLAYER_ID);
        body(new Point(tile.x, tile.y + 1), ENEMY_ID);
        taskManager.processTick(0.25f);
        return taskManager;
    }

    private void body(Point location, short ownerId) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Health(), new Owner(ownerId, ownerId), new Unconscious());
        creatures.put(entityId, stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getEntityId" -> entityId;
            case "getPosition" -> new Vector3f(location.x, 0, location.y);
            case "getCreatureCoordinates" -> location;
            case "getOwnerId" -> ownerId;
            case "isUnconscious", "hasLair" -> true;
            case "getLairLocation" -> new Point(1, 1);
            default -> NO_ANSWER;
        }));
    }

    private ICreatureController droppedImp(Point location) {
        ICreatureController imp = imp(location);
        entityData.setComponent(imp.getEntityId(), new DropArea(location));
        return imp;
    }

    private ICreatureController imp(Point location) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new CreatureComponent());
        var stateMachine = new DefaultStateMachine<ICreatureController, CreatureState>(null, CreatureState.IDLE);
        ICreatureController[] imp = new ICreatureController[1];
        imp[0] = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getEntityId" -> entityId;
            case "getPosition" -> new Vector3f(location.x, 0, location.y);
            case "getCreatureCoordinates" -> location;
            case "getOwnerId" -> PLAYER_ID;
            case "getStateMachine" -> stateMachine;
            case "setAssignedTask" -> {
                Task task = (Task) args[0];
                work.put(imp[0], task);
                entityData.setComponent(entityId, new TaskComponent(task.getId(), null, task.getTaskLocation(), task.getTaskType(), 0, false));
                yield null;
            }
            case "unassingCurrentTask" -> {
                released.add(imp[0]);
                work.remove(imp[0]).unassign(imp[0]);
                entityData.removeComponent(entityId, TaskComponent.class);
                yield null;
            }
            default -> NO_ANSWER;
        });
        creatures.put(entityId, imp[0]);
        return imp[0];
    }

    private TaskManager taskManager() {
        IMapDataInformation<?> map = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getTile" -> args.length == 1 ? tile((Point) args[0]) : tile(new Point((int) args[0], (int) args[1]));
            default -> NO_ANSWER;
        });
        IRoomController prison = stub(IRoomController.class, (name, args) -> NO_ANSWER);
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> map;
            case "getRoomsByFunction" -> List.of(prison);
            default -> NO_ANSWER;
        });
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> true;
            case "findPath" -> new DefaultGraphPath<>();
            default -> NO_ANSWER;
        });
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "createController" -> creatures.get((EntityId) args[0]);
            default -> NO_ANSWER;
        });
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> switch (name) {
            case "getKeeper" -> keeper;
            default -> NO_ANSWER;
        });
        IKwdFile levelData = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getGameLevel" -> new GameLevel();
            default -> NO_ANSWER;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> levelData;
            default -> NO_ANSWER;
        });
        return new TaskManager(entityData, stub(IGameWorldController.class, (name, args) -> NO_ANSWER), mapController, null,
                creaturesController, navigationService, List.of(player), levelInfo, null, Map.of());
    }

    private static IMapTileController tile(Point location) {
        if (location.x < 0 || location.y < 0 || location.x >= SIZE || location.y >= SIZE) {
            return null;
        }
        return stub(IMapTileController.class, (name, args) -> switch (name) {
            case "getLocation" -> location;
            case "getX" -> location.x;
            case "getY" -> location.y;
            case "getOwnerId" -> PLAYER_ID;
            default -> NO_ANSWER;
        });
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    /**
     * A stub of the interface. What isn't answered returns false, zero, an
     * empty collection or null
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> type.getSimpleName() + " stub";
                default -> answer.answer(method.getName(), args);
            };
            if (result != NO_ANSWER) {
                return result;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            } else if (returnType == int.class) {
                return 0;
            } else if (returnType == short.class) {
                return (short) 0;
            } else if (returnType == float.class) {
                return 0f;
            } else if (returnType == double.class) {
                return 0d;
            } else if (returnType == long.class) {
                return 0L;
            } else if (Set.class.isAssignableFrom(returnType)) {
                return Set.of();
            } else if (Collection.class.isAssignableFrom(returnType)) {
                return List.of();
            } else if (Map.class.isAssignableFrom(returnType)) {
                return Map.of();
            }
            return null;
        });
    }

}
