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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.ChickenSpawnFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Where a trainee trains: at a Training Dummy, or shooting at a wall target,
 * like DK II (0x4F2FF0, 0x4F3310)
 */
class TrainingSpotTest {

    private static final short PLAYER_ID = 3;

    /**
     * A Training Room one tile deep, from (1, 1) to (5, 1). The wall target
     * is on the wall of (3, 1), the dummy stands on (5, 1)
     */
    private static final Point WALL_TARGET_TILE = new Point(3, 1);
    private static final Point DUMMY_TILE = new Point(5, 1);

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId wallTarget = furniture(WALL_TARGET_TILE);
    private final EntityId dummy = furniture(DUMMY_TILE);
    private final List<Point> roomTiles = new ArrayList<>(List.of(new Point(1, 1), new Point(2, 1), WALL_TARGET_TILE,
            new Point(4, 1), DUMMY_TILE));
    private final List<EntityId> casts = new ArrayList<>();
    private ICreatureController creature;

    @Test
    void aTraineeWithoutARangedAttackGoesToTheDummyEvenWithAWallTargetCloser() {
        Task task = assignTraining(new Point(2, 1), false, List.of(WALL_TARGET_TILE, DUMMY_TILE));

        assertEquals(DUMMY_TILE, task.getTaskLocation());
        assertNull(task.getTaskTarget());
    }

    @Test
    void aTraineeWithoutARangedAttackDoesNotTrainAtAWallTarget() {
        assertNull(assignTraining(new Point(2, 1), false, List.of(WALL_TARGET_TILE)));
    }

    @Test
    void aTraineeWithARangedAttackShootsAtTheWallTargetFromTheClosestTileWithoutFurniture() {

        // The dummy is taken, so the wall target it is. The dummy's tile is the closest, the next one is used
        Task task = assignTraining(new Point(6, 1), true, List.of(WALL_TARGET_TILE));

        assertEquals(WALL_TARGET_TILE, task.getTaskLocation());
        assertEquals(wallTarget, task.getTaskTarget());
        assertEquals(new Vector2f(4, 1), task.getTarget(null));
    }

    @Test
    void theShootingTileMustSeeTheWallTarget() {

        // The room turns south at its east end, the tiles there can't see the target past the rock at (4, 2)
        roomTiles.addAll(List.of(new Point(5, 2), new Point(5, 3)));
        Task task = assignTraining(new Point(6, 3), true, List.of(WALL_TARGET_TILE));

        assertEquals(new Vector2f(4, 1), task.getTarget(null));
    }

    @Test
    void aTraineeCastsAtTheWallTargetButNotAtTheDummy() {
        assignTraining(new Point(6, 1), true, List.of(WALL_TARGET_TILE)).executeTask(creature, 0.25f);
        assignTraining(new Point(2, 1), false, List.of(DUMMY_TILE)).executeTask(creature, 0.25f);

        assertEquals(List.of(wallTarget), casts);
    }

    private Task assignTraining(Point creatureTile, boolean ranged, List<Point> freeSpots) {
        AtomicReference<Task> assigned = new AtomicReference<>();
        creature = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> creatureTile;
            case "getOwnerId" -> PLAYER_ID;
            case "hasRangedAttackReady" -> ranged;
            case "castTrainingShot" -> {
                casts.add((EntityId) args[0]);
                yield null;
            }
            case "setAssignedTask" -> {
                assigned.set((Task) args[0]);
                yield null;
            }
            default -> null;
        });

        boolean result = taskManager(trainingRoom(freeSpots)).assignTask(creature, Creature.JobType.TRAIN);

        assertEquals(result, assigned.get() != null);
        return assigned.get();
    }

    private IRoomController trainingRoom(List<Point> freeSpots) {
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 7, 0));
        roomInstance.addCoordinates(roomTiles);
        IRoomObjectControl<?> trainees = stub(IRoomObjectControl.class, (name, args) -> switch (name) {
            case "getAvailableCoordinates" -> freeSpots;
            default -> null;
        });
        return stub(IRoomController.class, (name, args) -> switch (name) {
            case "isFullCapacity" -> false;
            case "isTileAccessible" -> true;
            case "getRoomInstance" -> roomInstance;
            case "getObjectControl" -> trainees;
            case "getWallFurniture" -> Set.of(wallTarget);
            case "getFloorFurniture" -> Set.of(dummy);
            default -> null;
        });
    }

    private TaskManager taskManager(IRoomController room) {
        IMapDataInformation<?> emptyMap = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth", "getHeight" -> 0;
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> emptyMap;
            case "getRoomsByFunction" -> List.of(room);
            case "isSolid" -> !roomTiles.contains((Point) args[0]);
            default -> null;
        });
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "findPath" -> new DefaultGraphPath<>();
            default -> null;
        });
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> switch (name) {
            case "getKeeper" -> keeper;
            default -> null;
        });
        Map<MiscType, MiscVariable> gameSettings = Map.of(
                MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, ChickenSpawnFixtures.miscVariable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, 4),
                MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, ChickenSpawnFixtures.miscVariable(MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, -6));
        return new TaskManager(entityData, stub(IGameWorldController.class, (name, args) -> null), mapController, null, null,
                navigationService, List.of(player), stub(ILevelInfo.class, (name, args) -> null), null, gameSettings);
    }

    private EntityId furniture(Point tile) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(tile)));
        return entityId;
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
                    default -> {
                        Object result = answer.answer(method.getName(), args);
                        yield result == null && method.getReturnType() == boolean.class ? false : result;
                    }
                });
    }

}
