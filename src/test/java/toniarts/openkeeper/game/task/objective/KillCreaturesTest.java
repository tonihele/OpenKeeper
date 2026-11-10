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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hero told to kill a keeper's creatures goes after the nearest one he can
 * reach, and for the keeper's heart once the keeper has no creatures left
 */
class KillCreaturesTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short OTHER_KEEPER = Player.KEEPER2_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final Point HERO_TILE = new Point(2, 2);
    private static final Point HEART_CENTRE = new Point(20, 20);

    @Test
    void heroGoesAfterTheNearestCreatureHeCanReach() {
        ICreatureController walledIn = creature(new Point(3, 2), KEEPER, false);
        ICreatureController nearestReachable = creature(new Point(6, 2), KEEPER, false);
        ICreatureController farther = creature(new Point(12, 2), KEEPER, false);
        ICreatureController hero = hero(KEEPER);

        AbstractObjectiveTask task = KillCreatures.create(navigation(Set.of(new Point(3, 2))), null, levelInfo(), hero,
                List.of(farther, walledIn, nearestReachable));

        assertInstanceOf(KillCreatures.class, task);
        assertEquals(WorldUtils.pointToVector2f(new Point(6, 2)), task.getTarget(hero));
    }

    @Test
    void heroFollowsHisPreyAround() {
        Point[] preyTile = {new Point(6, 2)};
        ICreatureController prey = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> preyTile[0];
            case "getOwnerId" -> KEEPER;
            default -> null;
        });
        ICreatureController hero = hero(KEEPER);
        AbstractObjectiveTask task = KillCreatures.create(navigation(Set.of()), null, levelInfo(), hero, List.of(prey));

        preyTile[0] = new Point(9, 4);

        assertEquals(WorldUtils.pointToVector2f(new Point(9, 4)), task.getTarget(hero));
        assertTrue(task.isValid(hero));
    }

    @Test
    void heroWhoCanReachNoCreatureGetsNoTask() {
        ICreatureController walledIn = creature(new Point(3, 2), KEEPER, false);

        assertNull(KillCreatures.create(navigation(Set.of(new Point(3, 2))), null, levelInfo(), hero(KEEPER),
                List.of(walledIn)));
    }

    @Test
    void incapacitatedCreaturesAreLeftAlone() {
        ICreatureController knockedOut = creature(new Point(3, 2), KEEPER, true);

        assertNull(KillCreatures.create(navigation(Set.of()), null, levelInfo(), hero(KEEPER), List.of(knockedOut)));
    }

    @Test
    void heroGoesForTheHeartWhenTheKeeperHasNoCreatures() {
        AbstractObjectiveTask task = KillCreatures.create(navigation(Set.of()), heartMap(), levelInfo(), hero(KEEPER), List.of());

        assertInstanceOf(KillPlayer.class, task);
    }

    @Test
    void heroLooksForANewPreyOnceHeReachesIt() {
        ICreatureController hero = hero(KEEPER);
        AbstractObjectiveTask task = KillCreatures.create(navigation(Set.of()), null, levelInfo(), hero,
                List.of(creature(new Point(6, 2), KEEPER, false)));

        task.executeTask(hero, 0);

        assertFalse(task.isValid(hero));
    }

    @Test
    void preyThatIsKnockedOutEndsTheHunt() {
        boolean[] knockedOut = {false};
        ICreatureController prey = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> new Point(6, 2);
            case "getOwnerId" -> KEEPER;
            case "isIncapacitated" -> knockedOut[0];
            default -> null;
        });
        ICreatureController hero = hero(KEEPER);
        AbstractObjectiveTask task = KillCreatures.create(navigation(Set.of()), null, levelInfo(), hero, List.of(prey));

        knockedOut[0] = true;

        assertFalse(task.isValid(hero));
    }

    @Test
    void heroSentAfterAnotherKeeperDropsTheHunt() {
        AbstractObjectiveTask task = KillCreatures.create(navigation(Set.of()), null, levelInfo(), hero(KEEPER),
                List.of(creature(new Point(6, 2), KEEPER, false)));

        assertFalse(task.isValid(hero(OTHER_KEEPER)));
    }

    private static ILevelInfo levelInfo() {
        Keeper keeper = new Keeper(false, KEEPER);
        keeper.setDungeonHeartLocation(HEART_CENTRE);
        return stub(ILevelInfo.class, (name, args) -> "getPlayer".equals(name) ? Map.of(KEEPER, keeper).get((Short) args[0]) : null);
    }

    private static IMapController heartMap() {
        RoomInstance heartRoom = new RoomInstance(new Room());
        heartRoom.addCoordinate(HEART_CENTRE);
        IRoomController roomController = stub(IRoomController.class, (name, args) -> "getRoomInstance".equals(name) ? heartRoom : null);
        return stub(IMapController.class, (name, args) -> "getRoomControllerByCoordinates".equals(name) ? roomController : null);
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

    private static ICreatureController creature(Point tile, short owner, boolean incapacitated) {
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> tile;
            case "getOwnerId" -> owner;
            case "isIncapacitated" -> incapacitated;
            default -> null;
        });
    }

    private static ICreatureController hero(short objectiveTarget) {
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> HERO_TILE;
            case "getOwnerId" -> HERO;
            case "getObjective" -> Thing.HeroParty.Objective.KILL_CREATURES;
            case "getObjectiveTargetPlayerId" -> objectiveTarget;
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
