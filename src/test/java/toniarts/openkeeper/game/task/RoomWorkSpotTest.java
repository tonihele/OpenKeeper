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
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.creature.Pray;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.TempleFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DK II researches for 7 to 13 rounds of the research animation at a bookcase,
 * then goes to the closest bookcase nobody is at. In the Temple it prays 2 or 3
 * times, then moves to another free tile 36 times out of 100
 */
class RoomWorkSpotTest {

    private static final short PLAYER_ID = 3;

    // One round of the research animation takes a second
    private static final Creature RESEARCHER = CreatureFixtures.creatureWithAnimation(Creature.AnimationType.RESEARCHING, 10, 10);

    // One prayer takes a second
    private static final Creature WORSHIPPER = CreatureFixtures.creatureWithAnimation(Creature.AnimationType.PRAYING, 10, 10);
    private static final Map<MiscType, MiscVariable> GAME_SETTINGS = new EnumMap<>(Map.of(
            MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND, TempleFixtures.miscVariable(MiscType.MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND, 0),
            MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND, TempleFixtures.miscVariable(MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND, 0)));

    @Test
    void researcherStaysForSevenRounds() {
        IRoomController library = library(new Point(1, 1), new Point(2, 1));
        TaskManager taskManager = taskManager(library);
        Worker researcher = new Worker(new Point(1, 1), RESEARCHER);
        assertTrue(taskManager.assignFreeRoomTask(researcher.creature, library, ObjectType.RESEARCHER));
        Task task = researcher.task;

        assertFalse(task.moveToOtherSpot(researcher.creature, 6.9f));
        assertSame(task, researcher.task);
    }

    @Test
    void researcherMovesToTheClosestFreeBookcaseAfterThirteenRounds() {
        IRoomController library = library(new Point(1, 1), new Point(6, 1), new Point(2, 1), new Point(3, 1));
        TaskManager taskManager = taskManager(library);
        Worker other = new Worker(new Point(2, 1), RESEARCHER);
        Worker researcher = new Worker(new Point(1, 1), RESEARCHER);
        assertTrue(taskManager.assignFreeRoomTask(other.creature, library, ObjectType.RESEARCHER));
        assertTrue(taskManager.assignFreeRoomTask(researcher.creature, library, ObjectType.RESEARCHER));
        assertEquals(new Point(1, 1), researcher.task.getTaskLocation());

        assertTrue(researcher.task.moveToOtherSpot(researcher.creature, 13));
        assertEquals(new Point(3, 1), researcher.task.getTaskLocation());
    }

    @Test
    void researcherStaysWhenNoOtherBookcaseIsFree() {
        IRoomController library = library(new Point(1, 1), new Point(2, 1));
        TaskManager taskManager = taskManager(library);
        Worker other = new Worker(new Point(2, 1), RESEARCHER);
        Worker researcher = new Worker(new Point(1, 1), RESEARCHER);
        assertTrue(taskManager.assignFreeRoomTask(other.creature, library, ObjectType.RESEARCHER));
        assertTrue(taskManager.assignFreeRoomTask(researcher.creature, library, ObjectType.RESEARCHER));
        Task task = researcher.task;

        assertFalse(task.moveToOtherSpot(researcher.creature, 13));
        assertSame(task, researcher.task);
    }

    @Test
    void researcherWithoutResearchAnimationStays() {
        IRoomController library = library(new Point(1, 1), new Point(2, 1));
        TaskManager taskManager = taskManager(library);
        Worker researcher = new Worker(new Point(1, 1), CreatureFixtures.creatureWithAnimation(Creature.AnimationType.WALK, 10, 10));
        assertTrue(taskManager.assignFreeRoomTask(researcher.creature, library, ObjectType.RESEARCHER));
        Task task = researcher.task;

        assertFalse(task.moveToOtherSpot(researcher.creature, 1000));
        assertSame(task, researcher.task);
    }

    @Test
    void worshipperPicksItsSpotAgainAfterTwoOrThreePrayers() {
        IRoomController temple = library(new Point(1, 1), new Point(2, 1));
        TaskManager taskManager = taskManager(temple);
        Worker worshipper = new Worker(new Point(1, 1), WORSHIPPER);
        assertTrue(taskManager.assignFreeRoomTask(worshipper.creature, temple, ObjectType.WORSHIPPER));
        Task task = worshipper.task;

        // The rolls: 3 prayers, 36 stays, 2 more prayers, 35 moves
        Pray pray = new Pray(null, null, new Point(1, 1), PLAYER_ID, temple, taskManager, null, GAME_SETTINGS, null, rolls(1, 36, 0, 35));

        assertFalse(pray.moveToOtherSpot(worshipper.creature, 2.9f));
        assertFalse(pray.moveToOtherSpot(worshipper.creature, 3));
        assertFalse(pray.moveToOtherSpot(worshipper.creature, 4.9f));
        assertSame(task, worshipper.task);

        assertTrue(pray.moveToOtherSpot(worshipper.creature, 5));
        assertEquals(new Point(2, 1), worshipper.task.getTaskLocation());
    }

    private static RandomGenerator rolls(int... values) {
        Iterator<Integer> rolls = IntStream.of(values).iterator();
        return new RandomGenerator() {

            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int nextInt(int bound) {
                return rolls.next();
            }
        };
    }

    private static TaskManager taskManager(IRoomController room) {
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" ->
                0;
            default ->
                null;
        });
        IMapController mapController = proxy(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                List.of(room);
            default ->
                null;
        });
        INavigationService navigationService = proxy(INavigationService.class, (proxy, method, args) -> new DefaultGraphPath<>());
        IGameWorldController gameWorldController = proxy(IGameWorldController.class, (proxy, method, args) -> null);
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = proxy(IPlayerController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" ->
                keeper;
            default ->
                null;
        });

        ILevelInfo levelInfo = proxy(ILevelInfo.class, (proxy, method, args) -> switch (method.getName()) {
            case "getLevelData" ->
                proxy(IKwdFile.class, (p, m, a) -> null);
            default ->
                null;
        });

        return new TaskManager(new DefaultEntityData(), gameWorldController, mapController, null, null, navigationService, List.of(player), levelInfo, null, GAME_SETTINGS);
    }

    /**
     * A library with a researcher's place at each of the given points
     */
    private static IRoomController library(Point... points) {
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 6, 0));
        for (Point point : points) {
            roomInstance.addCoordinate(point);
        }
        IRoomObjectControl control = proxy(IRoomObjectControl.class, (proxy, method, args) -> switch (method.getName()) {
            case "getAvailableCoordinates" ->
                List.of(points);
            case "getMaxCapacity" ->
                points.length + 1;
            case "getCurrentCapacity" ->
                0;
            default ->
                null;
        });
        return proxy(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomInstance" ->
                roomInstance;
            case "getObjectControl" ->
                control;
            case "isTileAccessible" ->
                true;
            case "isFullCapacity" ->
                false;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        });
    }

    /**
     * A creature that keeps the task it is given and leaves its previous one
     * like the creature controller does
     */
    private static final class Worker {

        private final ICreatureController creature;
        private Task task;

        private Worker(Point position, Creature creatureData) {
            creature = proxy(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
                case "getCreatureCoordinates" ->
                    position;
                case "getOwnerId" ->
                    PLAYER_ID;
                case "getCreature" ->
                    creatureData;
                case "setAssignedTask" -> {
                    if (task != null) {
                        task.unassign((ICreatureController) proxy);
                    }
                    task = (Task) args[0];
                    yield null;
                }
                case "hashCode" ->
                    System.identityHashCode(proxy);
                case "equals" ->
                    proxy == args[0];
                default ->
                    null;
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
