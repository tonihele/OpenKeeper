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
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CombatPitSpectator;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.creature.WatchCombatPit;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.CombatPitFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature with nothing else to do watches a fight in its keeper's Combat
 * Pit from the tiles round the pit, as in DK II (0x478680, 0x457780)
 */
class WatchCombatPitTest {

    private static final short KEEPER = Player.KEEPER1_ID;

    private final EntityData entityData = new DefaultEntityData();
    private final double[] gameTime = {100};
    private final Map<Point, List<ICreatureController>> creaturesOnTiles = new HashMap<>();
    private final IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class, (proxy, method, args)
            -> "getEntityTypesInLocation".equals(method.getName()) ? creaturesOnTiles.getOrDefault((Point) args[0], List.of()) : null);
    private final AtomicReference<Task> assigned = new AtomicReference<>();

    /**
     * The pit is the tiles 1..5 x 1..5, the creatures fight on 2..4 x 2..4
     */
    private final CombatPitController pit = buildPit();

    @Test
    void spectatorCheersAndLeavesOnceTheFightIsFourSecondsOver() {
        EntityId fighter = fighter();
        assertTrue(pit.isFightOn());
        ICreatureController spectator = creatureAt(new Point(1, 3));
        WatchCombatPit task = new WatchCombatPit(null, null, pit, new Point(1, 3), KEEPER, entityData, positionLookup, rolls(50, 0, 99, 50, 0));

        task.executeTask(spectator, 0f);
        assertEquals(Creature.AnimationType.HAPPY, entityData.getComponent(spectator.getEntityId(), CombatPitSpectator.class).animation);
        assertEquals(new Point(3, 3), task.getTaskLocation()); // faces the middle of the pit

        // The animation, a second without timing data, plays once. The fight ended in the meantime
        entityData.removeComponent(fighter, CombatPitFighter.class);
        task.executeTask(spectator, 0.5f);
        gameTime[0] += 3;
        task.executeTask(spectator, 1f);
        assertTrue(task.isValid(spectator));

        task.executeTask(spectator, 1f);
        gameTime[0] += 2;
        task.executeTask(spectator, 2f);
        assertFalse(task.isValid(spectator));
        assertNull(entityData.getComponent(spectator.getEntityId(), CombatPitSpectator.class));
    }

    @Test
    void spectatorIdlesOnALowRollAndCheersTwiceOnAHighOne() {
        fighter();
        ICreatureController spectator = creatureAt(new Point(1, 3));
        WatchCombatPit task = new WatchCombatPit(null, null, pit, new Point(1, 3), KEEPER, entityData, positionLookup, rolls(30, 99, 31, 1, 99));

        task.executeTask(spectator, 0f);
        assertEquals(Creature.AnimationType.IDLE_1, entityData.getComponent(spectator.getEntityId(), CombatPitSpectator.class).animation);

        // Stays on its tile (roll 99), then cheers twice: still at it after one second
        task.executeTask(spectator, 1f);
        task.executeTask(spectator, 1f);
        assertEquals(Creature.AnimationType.HAPPY, entityData.getComponent(spectator.getEntityId(), CombatPitSpectator.class).animation);
        task.executeTask(spectator, 2.5f);
        task.executeTask(spectator, 3f);
        assertEquals(new Vector2f(1, 3), task.getTarget(spectator));
    }

    @Test
    void spectatorMovesToAFreeTileNextToItRoundThePit() {
        fighter();
        ICreatureController spectator = creatureAt(new Point(1, 1));
        creaturesOnTiles.put(new Point(2, 1), List.of(creatureAt(new Point(2, 1))));
        WatchCombatPit task = new WatchCombatPit(null, null, pit, new Point(1, 1), KEEPER, entityData, positionLookup, rolls(50, 0, 35, 0));

        task.executeTask(spectator, 0f);
        task.executeTask(spectator, 1f);

        // Of the corner's two neighbours one is taken
        assertEquals(new Vector2f(1, 2), task.getTarget(spectator));
        Vector2f place = task.getWorkPosition(spectator);
        assertTrue(Math.abs(place.x - 1) <= 1 / 6f && Math.abs(place.y - 2) <= 1 / 6f);
    }

    @Test
    void spectatorSharingItsTileMovesOnInsteadOfCheering() {
        fighter();
        ICreatureController spectator = creatureAt(new Point(3, 1));
        creaturesOnTiles.put(new Point(3, 1), List.of(spectator, creatureAt(new Point(3, 1))));
        creaturesOnTiles.put(new Point(2, 1), List.of(creatureAt(new Point(2, 1))));
        WatchCombatPit task = new WatchCombatPit(null, null, pit, new Point(3, 1), KEEPER, entityData, positionLookup, rolls(0));

        task.executeTask(spectator, 0f);

        // Not into the pit (3, 2), not onto the taken tile
        assertEquals(new Vector2f(4, 1), task.getTarget(spectator));
        assertNull(entityData.getComponent(spectator.getEntityId(), CombatPitSpectator.class));
    }

    @Test
    void idleCreatureGoesToTheNearestFreeTileRoundAPitWithAFight() {
        TaskManager taskManager = taskManager(Set.of(new Point(5, 3)));
        ICreatureController creature = creatureAt(new Point(8, 3));
        creaturesOnTiles.put(new Point(5, 4), List.of(creatureAt(new Point(5, 4))));

        assertFalse(taskManager.assignTask(creature, Creature.JobType.COMBAT_PIT_SPECTATE)); // no fight

        fighter();
        assertTrue(taskManager.isTaskAvailable(creature, Creature.JobType.COMBAT_PIT_SPECTATE));
        assertNull(assigned.get());
        assertTrue(taskManager.assignTask(creature, Creature.JobType.COMBAT_PIT_SPECTATE));

        // (5, 3) can't be reached and (5, 4) is taken
        assertEquals(TaskType.WATCH_COMBAT_PIT, assigned.get().getTaskType());
        assertEquals(new Vector2f(5, 2), assigned.get().getTarget(creature));
    }

    @Test
    void creatureAtThePitWatchesFromItsTileForAMomentAfterTheFight() {
        TaskManager taskManager = taskManager(Set.of());
        EntityId fighter = fighter();
        assertTrue(pit.isFightOn());
        entityData.removeComponent(fighter, CombatPitFighter.class);

        // From elsewhere nobody comes for a fight that is over
        gameTime[0] += 2;
        assertFalse(taskManager.assignTask(creatureAt(new Point(8, 6)), Creature.JobType.COMBAT_PIT_SPECTATE));

        ICreatureController atThePit = creatureAt(new Point(4, 5));
        assertTrue(taskManager.assignTask(atThePit, Creature.JobType.COMBAT_PIT_SPECTATE));
        assertEquals(new Vector2f(4, 5), assigned.get().getTarget(atThePit));

        gameTime[0] += 3;
        assertFalse(taskManager.assignTask(atThePit, Creature.JobType.COMBAT_PIT_SPECTATE));
    }

    private EntityId fighter() {
        EntityId fighter = entityData.createEntity();
        entityData.setComponents(fighter, new CreatureComponent(), new Owner(KEEPER, KEEPER));
        assertTrue(pit.addFighter(fighter, new Point(3, 3)));
        return fighter;
    }

    private ICreatureController creatureAt(Point p) {
        EntityId entityId = entityData.createEntity();
        Creature creature = CreatureFixtures.creatureWithAnimation(Creature.AnimationType.HAPPY, ArtResource.ArtResourceType.ANIMATING_MESH);
        return stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityId" -> entityId;
            case "getCreatureCoordinates" -> p;
            case "getOwnerId" -> KEEPER;
            case "getCreature" -> creature;
            case "setAssignedTask" -> {
                assigned.set((Task) args[0]);
                yield null;
            }
            default -> null;
        });
    }

    private CombatPitController buildPit() {
        IObjectsController objectsController = stub(IObjectsController.class, (proxy, method, args)
                -> "loadObject".equals(method.getName()) ? entityData.createEntity() : null);
        RoomInstance roomInstance = new RoomInstance(CombatPitFixtures.pitRoom());
        roomInstance.setOwnerId(KEEPER);
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        CombatPitController controller = new CombatPitController(entityData.createEntity(), entityData, null, roomInstance,
                objectsController, () -> gameTime[0]);
        controller.construct();
        return controller;
    }

    private TaskManager taskManager(Set<Point> walledIn) {
        Keeper keeper = new Keeper(false, KEEPER);
        IPlayerController player = stub(IPlayerController.class, (proxy, method, args) -> "getKeeper".equals(method.getName()) ? keeper : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, method, args) -> null);
        IMapController mapController = stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getRoomControllers" -> List.<IRoomController>of(pit);
            default -> null;
        });
        INavigationService navigation = stub(INavigationService.class, (proxy, method, args)
                -> "findPath".equals(method.getName()) && !walledIn.contains((Point) args[1]) ? new DefaultGraphPath<>() : null);
        return new TaskManager(entityData, stub(IGameWorldController.class, (proxy, method, args) -> null),
                mapController, stub(IObjectsController.class, (proxy, method, args) -> null), stub(ICreaturesController.class, (proxy, method, args) -> null),
                navigation, List.of(player), stub(ILevelInfo.class, (proxy, method, args) -> null), positionLookup, Map.of());
    }

    /**
     * The rolls to come, in order. Each is a roll of a hundred or, for a
     * choice of two or of the tiles, taken as it is
     */
    private static RandomGenerator rolls(int... rolls) {
        Deque<Integer> queue = new ArrayDeque<>();
        for (int roll : rolls) {
            queue.add(roll);
        }
        return new RandomGenerator() {

            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                return queue.remove();
            }

            @Override
            public float nextFloat() {
                return 0.5f;
            }
        };
    }

    /**
     * A stub of the interface, the primitives it doesn't answer are 0 or false
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object value = handler.invoke(proxy, method, args);
            Class<?> returnType = method.getReturnType();
            if (value != null || !returnType.isPrimitive() || returnType == void.class) {
                return value;
            }
            return returnType == boolean.class ? false : MethodHandles.zero(returnType).invoke();
        });
    }

}
