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
import com.simsilica.es.EntityId;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /**
     * A Thief carries 7500 gold at most
     */
    private static final int THIEF_MAX_GOLD = 7500;
    private static final Consumer<ICreatureController> NO_LOOT_LISTENER = thief -> {
    };

    @Test
    void thiefGoesForTheNearestGoldHeCanReach() {
        IRoomController treasury = treasury(false, new Point(4, 2), new Point(6, 2), new Point(9, 2));

        Point tile = StealGold.findGoldTile(thief(KEEPER, 0, null), List.of(treasury), navigation(Set.of(new Point(4, 2))));

        assertEquals(new Point(6, 2), tile);
    }

    @Test
    void goldInTheDungeonHeartIsLeftAlone() {
        IRoomController heart = treasury(true, new Point(3, 2));

        assertNull(StealGold.findGoldTile(thief(KEEPER, 0, null), List.of(heart), navigation(Set.of())));
    }

    @Test
    void thiefWithNoGoldInReachHasNothingToSteal() {
        IRoomController treasury = treasury(false, new Point(4, 2));

        assertNull(StealGold.findGoldTile(thief(KEEPER, 0, null), List.of(treasury), navigation(Set.of(new Point(4, 2)))));
    }

    @Test
    void thiefTakesAllHeCanCarryAndKeepsHalf() {
        List<Integer> asked = new ArrayList<>();
        List<Integer> kept = new ArrayList<>();
        StealGold task = new StealGold(null, null, treasuryGold(100_000, asked), new Point(6, 2), KEEPER, HERO, NO_LOOT_LISTENER);

        task.executeTask(thief(KEEPER, 0, kept), 0);

        assertEquals(List.of(THIEF_MAX_GOLD), asked);
        assertEquals(List.of(THIEF_MAX_GOLD / 2), kept);
    }

    @Test
    void thiefTakesOnlyWhatTheTreasuryHolds() {
        List<Integer> asked = new ArrayList<>();
        List<Integer> kept = new ArrayList<>();
        StealGold task = new StealGold(null, null, treasuryGold(2500, asked), new Point(6, 2), KEEPER, HERO, NO_LOOT_LISTENER);

        task.executeTask(thief(KEEPER, 0, kept), 0);

        assertEquals(List.of(THIEF_MAX_GOLD), asked);
        assertEquals(List.of(1250), kept);
    }

    @Test
    void thiefStealsOnce() {
        StealGold task = new StealGold(null, null, treasuryGold(100_000, new ArrayList<>()), new Point(6, 2), KEEPER, HERO, NO_LOOT_LISTENER);
        ICreatureController thief = thief(KEEPER, 0, new ArrayList<>());

        assertTrue(task.isValid(thief));
        task.executeTask(thief, 0);

        assertFalse(task.isValid(thief));
    }

    @Test
    void thiefWithTheLootIsReported() {
        List<ICreatureController> looted = new ArrayList<>();
        StealGold task = new StealGold(null, null, treasuryGold(100_000, new ArrayList<>()), new Point(6, 2), KEEPER, HERO, looted::add);
        ICreatureController thief = thief(KEEPER, 0, new ArrayList<>());

        task.executeTask(thief, 0);

        assertEquals(1, looted.size());
        assertSame(thief, looted.get(0));
    }

    @Test
    void thiefSentToAnotherKeeperDropsTheTask() {
        StealGold task = new StealGold(null, null, treasuryGold(100_000, new ArrayList<>()), new Point(6, 2), KEEPER, HERO, NO_LOOT_LISTENER);

        assertFalse(task.isValid(thief(OTHER_KEEPER, 0, new ArrayList<>())));
    }

    @Test
    void thiefBackWhereHeCameInIsGoneWithTheGold() {
        List<Integer> lost = new ArrayList<>();
        List<ICreatureController> escaped = new ArrayList<>();
        ICreatureController thief = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getObjective" -> Thing.HeroParty.Objective.STEAL_GOLD;
            case "getGold" -> 3750;
            case "substractGold" -> {
                lost.add((Integer) args[0]);
                yield null;
            }
            default -> null;
        });
        EscapeWithGold task = new EscapeWithGold(null, null, THIEF_TILE, HERO, escaped::add);

        assertTrue(task.isValid(thief));
        task.executeTask(thief, 0);

        assertEquals(List.of(3750), lost);
        assertEquals(1, escaped.size());
        assertSame(thief, escaped.get(0));
        assertFalse(task.isValid(thief));
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

    private static ICreatureController thief(short objectiveTarget, int gold, List<Integer> goldGiven) {
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> THIEF_TILE;
            case "getOwnerId" -> HERO;
            case "getObjective" -> Thing.HeroParty.Objective.STEAL_GOLD;
            case "getObjectiveTargetPlayerId" -> objectiveTarget;
            case "getGold" -> gold;
            case "getMaxGold" -> THIEF_MAX_GOLD;
            case "addGold" -> {
                goldGiven.add((Integer) args[0]);
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
