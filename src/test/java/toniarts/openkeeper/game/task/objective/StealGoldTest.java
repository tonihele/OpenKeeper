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
package toniarts.openkeeper.game.task.objective;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.StolenGold;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.CreatureStealsFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A thief goes to the keeper's Treasury, takes what he can carry and keeps
 * half of it, then runs back to where he came in and is gone with the gold
 */
class StealGoldTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short OTHER_KEEPER = Player.KEEPER2_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final Point THIEF_TILE = new Point(2, 2);
    private static final Point GOLD_TILE = new Point(6, 2);

    /**
     * A Thief carries 7500 gold at most
     */
    private static final int THIEF_MAX_GOLD = 7500;
    private static final Consumer<ICreatureController> NO_ESCAPE_LISTENER = thief -> {
    };

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId thiefId = createThief();
    private Consumer<ICreatureController> stealingListener = thief -> {};
    private short thiefOwner = HERO;

    private EntityId createThief() {
        EntityId id = entityData.createEntity();
        entityData.setComponent(id, new CreatureComponent());
        return id;
    }
    private final List<Integer> goldGiven = new ArrayList<>();
    private final List<Integer> goldLost = new ArrayList<>();

    @Test
    void thiefGoesForTheNearestGoldHeCanReach() {
        IRoomController treasury = treasury(false, new Point(4, 2), new Point(6, 2), new Point(9, 2));

        Point tile = StealGold.findGoldTile(thief(KEEPER), List.of(treasury), navigation(Set.of(new Point(4, 2))));

        assertEquals(new Point(6, 2), tile);
    }

    @Test
    void goldInTheDungeonHeartIsLeftAlone() {
        IRoomController heart = treasury(true, new Point(3, 2));

        assertNull(StealGold.findGoldTile(thief(KEEPER), List.of(heart), navigation(Set.of())));
    }

    @Test
    void thiefWithNoGoldInReachHasNothingToSteal() {
        IRoomController treasury = treasury(false, new Point(4, 2));

        StealGold task = stealGold(List.of(treasury), navigation(Set.of(new Point(4, 2))), treasuryGold(100_000, new ArrayList<>()), NO_ESCAPE_LISTENER);

        assertNull(task.getCurrentTask());
    }

    @Test
    void thiefFirstGoesForTheGold() {
        Task current = stealGold(100_000, new ArrayList<>()).getCurrentTask();

        assertNotNull(current);
        assertEquals(GOLD_TILE, current.getTaskLocation());
    }

    @Test
    void thiefTakesAllHeCanCarryAndKeepsHalf() {
        List<Integer> asked = new ArrayList<>();

        stealGold(100_000, asked).getCurrentTask().executeTask(thief(KEEPER), 0);

        assertEquals(List.of(THIEF_MAX_GOLD), asked);
        assertEquals(List.of(THIEF_MAX_GOLD / 2), goldGiven);
    }

    @Test
    void thiefTakesOnlyWhatTheTreasuryHolds() {
        List<Integer> asked = new ArrayList<>();

        stealGold(2500, asked).getCurrentTask().executeTask(thief(KEEPER), 0);

        assertEquals(List.of(THIEF_MAX_GOLD), asked);
        assertEquals(List.of(1250), goldGiven);
    }

    @Test
    void thiefStealsOnce() {
        Task steal = stealGold(100_000, new ArrayList<>()).getCurrentTask();
        ICreatureController thief = thief(KEEPER);

        assertTrue(steal.isValid(thief));
        steal.executeTask(thief, 0);

        assertFalse(steal.isValid(thief));
    }

    @Test
    void thiefWithTheGoldIsTaggedWithTheWayOut() {
        stealGold(100_000, new ArrayList<>()).getCurrentTask().executeTask(thief(KEEPER), 0);

        StolenGold stolenGold = entityData.getComponent(thiefId, StolenGold.class);
        assertNotNull(stolenGold);
        assertEquals(THIEF_TILE, stolenGold.exit);
    }

    @Test
    void thiefWithTheGoldRunsBackToTheExit() {

        // Like a loaded game, all there is to go on is the tag
        Point exit = new Point(30, 41);
        entityData.setComponent(thiefId, new StolenGold(exit));
        List<Integer> asked = new ArrayList<>();

        StealGold task = stealGold(List.of(), navigation(Set.of()), treasuryGold(100_000, asked), NO_ESCAPE_LISTENER);

        assertSame(task, task.getCurrentTask());
        assertEquals(exit, task.getTaskLocation());
        assertTrue(asked.isEmpty());
    }

    @Test
    void thiefSentToAnotherKeeperDropsTheTask() {
        StealGold task = stealGold(100_000, new ArrayList<>());

        assertFalse(task.isValid(thief(OTHER_KEEPER)));
        assertFalse(task.getCurrentTask().isValid(thief(OTHER_KEEPER)));
    }

    @Test
    void thiefWithTheGoldRunsWhoeverHeIsSentAfter() {
        entityData.setComponent(thiefId, new StolenGold(THIEF_TILE));

        assertTrue(stealGold(100_000, new ArrayList<>()).isValid(thief(OTHER_KEEPER)));
    }

    @Test
    void thiefBackWhereHeCameInIsGoneWithTheGold() {
        entityData.setComponent(thiefId, new StolenGold(THIEF_TILE));
        List<ICreatureController> escaped = new ArrayList<>();
        ICreatureController thief = thief(KEEPER);
        StealGold task = stealGold(List.of(), navigation(Set.of()), treasuryGold(0, new ArrayList<>()), escaped::add);

        assertTrue(task.isValid(thief));
        task.getCurrentTask().executeTask(thief, 0);

        assertEquals(List.of(3750), goldLost);
        assertEquals(1, escaped.size());
        assertSame(thief, escaped.get(0));
        assertFalse(task.isValid(thief));
    }

    @ParameterizedTest
    @ValueSource(shorts = {HERO, OTHER_KEEPER})
    void stealingEventRunsBeforeLootForHeroAndKeeperCreatures(short owner) {
        thiefOwner = owner;
        try (ScriptScene scene = scripts(255, true)) {
            stealingListener = thief -> {
                assertTrue(goldGiven.isEmpty());
                assertNull(entityData.getComponent(thiefId, StolenGold.class));
                scene.controller.onSteals(thief.getEntityId());
                assertEquals(1, scene.count());
            };
            stealGold(100_000, new ArrayList<>()).getCurrentTask().executeTask(thief(KEEPER), 0);
            assertEquals(1, scene.count());
            assertEquals(0, scene.flags.getOrDefault((short) 2, 0));
            assertEquals(List.of(THIEF_MAX_GOLD / 2), goldGiven);
        }
    }

    @Test
    void acceptedAttemptEmitsEvenWhenNoGoldIsLeft() {
        try (ScriptScene scene = scripts(255, true)) {
            stealingListener = thief -> scene.controller.onSteals(thief.getEntityId());
            stealGold(0, new ArrayList<>()).getCurrentTask().executeTask(thief(KEEPER), 0);
            assertEquals(1, scene.count());
            assertEquals(List.of(0), goldGiven);
        }
    }

    @Test
    void carryingGoldPollingAndEscapingDoNotEmitStealingAgain() {
        try (ScriptScene scene = scripts(255, false)) {
            stealingListener = thief -> scene.controller.onSteals(thief.getEntityId());
            StealGold task = stealGold(100_000, new ArrayList<>());
            task.getCurrentTask().executeTask(thief(KEEPER), 0);
            for (int i = 0; i < 12; i++) scene.controller.processTick(0.25f);
            assertEquals(1, scene.count());
            StealGold resumed = stealGold(List.of(), navigation(Set.of()),
                    treasuryGold(0, new ArrayList<>()), NO_ESCAPE_LISTENER);
            resumed.getCurrentTask().executeTask(thief(KEEPER), 0);
            assertEquals(1, scene.count());
            assertEquals(0, scene.flags.getOrDefault((short) 2, 0));
        }
    }

    @Test
    void noReachableGoldDoesNotEmitAnAttempt() {
        try (ScriptScene scene = scripts(255, false)) {
            stealingListener = thief -> scene.controller.onSteals(thief.getEntityId());
            assertNull(stealGold(List.of(), navigation(Set.of()),
                    treasuryGold(0, new ArrayList<>()), NO_ESCAPE_LISTENER).getCurrentTask());
            scene.controller.processTick(0.25f);
            assertEquals(0, scene.count());
        }
    }

    @Test
    void separateAttemptsRespectFiniteScriptRepeats() {
        try (ScriptScene scene = scripts(2, false)) {
            stealingListener = thief -> scene.controller.onSteals(thief.getEntityId());
            for (int i = 0; i < 3; i++) {
                entityData.removeComponent(thiefId, StolenGold.class);
                stealGold(100_000, new ArrayList<>()).getCurrentTask().executeTask(thief(KEEPER), 0);
            }
            assertEquals(2, scene.count());
            assertEquals(3, goldGiven.size());
        }
    }

    @Test
    void eventWithoutCurrentBindingIsIgnored() {
        try (ScriptScene scene = scripts(255, false)) {
            entityData.removeComponent(thiefId, toniarts.openkeeper.game.component.Trigger.class);
            scene.controller.onSteals(thiefId);
            assertEquals(0, scene.count());
        }
    }

    @Test
    void removalDuringEventDoesNotTakeGoldOrRecreateThief() {
        List<Integer> asked = new ArrayList<>();
        stealingListener = thief -> entityData.removeEntity(thief.getEntityId());
        stealGold(100_000, asked).getCurrentTask().executeTask(thief(KEEPER), 0);
        assertTrue(asked.isEmpty());
        assertTrue(goldGiven.isEmpty());
        assertNull(entityData.getComponent(thiefId, StolenGold.class));
    }

    private ScriptScene scripts(int repeats, boolean checkGold) {
        Map<Short, Integer> flags = new HashMap<>();
        Thing.Creature thing = CreatureStealsFixtures.thing(thiefOwner);
        IKwdFile kwd = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getTriggers" -> CreatureStealsFixtures.script(repeats, checkGold);
            case "getThings" -> ((Class<?>) args[0]).isInstance(thing) ? List.of(thing) : List.of();
            default -> null;
        });
        ILevelInfo info = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwd;
            case "getFlag" -> flags.getOrDefault(((Number) args[0]).shortValue(), 0);
            case "setFlag" -> flags.put(((Number) args[0]).shortValue(), ((Number) args[1]).intValue());
            default -> null;
        });
        ICreaturesController creatures = stub(ICreaturesController.class, (name, args) ->
                name.equals("createController") ? thief(KEEPER) : null);
        entityData.setComponent(thiefId, new toniarts.openkeeper.game.component.Trigger(1));
        return new ScriptScene(new CreatureTriggerLogicController(null, info, () -> 0,
                null, creatures, null, entityData), flags);
    }

    private record ScriptScene(CreatureTriggerLogicController controller,
            Map<Short, Integer> flags) implements AutoCloseable {
        int count() { return flags.getOrDefault((short) 1, 0); }
        @Override
        public void close() { controller.stop(); }
    }

    private StealGold stealGold(int held, List<Integer> requests) {
        return stealGold(List.of(treasury(false, GOLD_TILE)), navigation(Set.of()), treasuryGold(held, requests), NO_ESCAPE_LISTENER);
    }

    private StealGold stealGold(List<IRoomController> treasuries, INavigationService navigation, IGameWorldController gameWorldController,
            Consumer<ICreatureController> escapeListener) {
        IMapController map = stub(IMapController.class, (name, args) -> "getRoomsByFunction".equals(name) ? treasuries : null);
        return new StealGold(navigation, map, gameWorldController, entityData, thief(KEEPER), escapeListener, stealingListener);
    }

    /**
     * The keeper's treasuries, taking from them what they hold
     *
     * @param held the gold the keeper has there
     * @param requests what the thief asks for
     */
    private static IGameWorldController treasuryGold(int held, List<Integer> requests) {
        return stub(IGameWorldController.class, (name, args) -> {
            if ("substractGold".equals(name) && args.length == 3 && (Short) args[1] == KEEPER) {
                int amount = (Integer) args[0];
                requests.add(amount);
                return Math.max(0, amount - held);
            }
            return null;
        });
    }

    private static IRoomController treasury(boolean dungeonHeart, Point... goldTiles) {
        RoomInstance room = new RoomInstance(new Room());
        for (int x = 2; x <= 10; x++) {
            room.addCoordinate(new Point(x, 2));
        }
        Set<Point> withGold = Set.of(goldTiles);
        IRoomObjectControl<?> goldControl = stub(IRoomObjectControl.class, (name, args) -> {
            if ("getItems".equals(name)) {
                return withGold.contains((Point) args[0]) ? List.of(new EntityId(1)) : null;
            }
            return null;
        });
        return stub(IRoomController.class, (name, args) -> switch (name) {
            case "getRoomInstance" -> room;
            case "isDungeonHeart" -> dungeonHeart;
            case "getObjectControl" -> goldControl;
            default -> null;
        });
    }

    /**
     * Finds a path to every tile except the walled in ones
     */
    private static INavigationService navigation(Set<Point> walledIn) {
        return stub(INavigationService.class, (name, args) -> {
            if ("findPath".equals(name) && !walledIn.contains((Point) args[1])) {
                return new DefaultGraphPath<>();
            }
            return null;
        });
    }

    /**
     * A thief with no gold of his own, and 3750 once he has stolen some
     */
    private ICreatureController thief(short objectiveTarget) {
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getEntityId" -> thiefId;
            case "getCreatureCoordinates" -> THIEF_TILE;
            case "getOwnerId" -> thiefOwner;
            case "getObjective" -> Thing.HeroParty.Objective.STEAL_GOLD;
            case "getObjectiveTargetPlayerId" -> objectiveTarget;
            case "getGold" -> entityData.getComponent(thiefId, StolenGold.class) == null ? 0 : 3750;
            case "getMaxGold" -> THIEF_MAX_GOLD;
            case "addGold" -> {
                goldGiven.add((Integer) args[0]);
                yield null;
            }
            case "substractGold" -> {
                goldLost.add((Integer) args[0]);
                yield null;
            }
            default -> null;
        });
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    /**
     * A stub that answers what it is told and otherwise runs the default
     * methods of the interface
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
