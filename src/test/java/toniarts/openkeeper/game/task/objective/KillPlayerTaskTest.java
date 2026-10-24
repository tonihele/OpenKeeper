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

import com.jme3.math.Vector2f;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hero with a clear way to the keeper's heart: it keeps its kill task while
 * it walks there, and heads for the nearest heart tile
 */
class KillPlayerTaskTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short OTHER_KEEPER = Player.KEEPER2_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final Point HEART_CENTRE = new Point(10, 10);

    /**
     * Above the 5x5 heart room, two tiles from its top edge
     */
    private static final Point HERO_TILE = new Point(10, 6);

    @Test
    void heroWalkingToTheHeartKeepsItsTask() {
        ICreatureController hero = hero(false, false, KEEPER);
        KillPlayer task = createTask(hero);

        assertTrue(task.getTaskQueue().isEmpty());
        assertTrue(task.isValid(hero));
    }

    @Test
    void heroAtTheHeartKeepsItsTask() {
        ICreatureController hero = hero(true, true, KEEPER);

        assertTrue(createTask(hero).isValid(hero));
    }

    @Test
    void heroStoppedShortOfTheHeartNeedsANewPlan() {
        ICreatureController hero = hero(true, false, KEEPER);

        assertFalse(createTask(hero).isValid(hero));
    }

    @Test
    void heroSentAfterAnotherKeeperDropsTheTask() {
        ICreatureController hero = hero(false, false, OTHER_KEEPER);

        assertFalse(createTask(hero).isValid(hero));
    }

    @Test
    void heroHeadsForTheNearestHeartTile() {
        ICreatureController hero = hero(false, false, KEEPER);

        assertEquals(new Vector2f(10, 8), createTask(hero).getTarget(hero));
    }

    private static KillPlayer createTask(ICreatureController hero) {
        Keeper keeper = new Keeper(false, KEEPER);
        keeper.setDungeonHeartLocation(HEART_CENTRE);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> "getPlayer".equals(name) ? Map.of(KEEPER, keeper).get((Short) args[0]) : null);

        RoomInstance heartRoom = new RoomInstance(new Room());
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                heartRoom.addCoordinate(new Point(HEART_CENTRE.x + x, HEART_CENTRE.y + y));
            }
        }
        IRoomController roomController = stub(IRoomController.class, (name, args) -> "getRoomInstance".equals(name) ? heartRoom : null);
        IMapController mapController = stub(IMapController.class, (name, args) -> "getRoomControllerByCoordinates".equals(name) ? roomController : null);

        // No path is found, so there are no steps to take on the way
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> null);

        return new KillPlayer(navigationService, mapController, levelInfo, KEEPER, hero);
    }

    private static ICreatureController hero(boolean stopped, boolean atTarget, short objectiveTarget) {
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> HERO_TILE;
            case "getOwnerId" -> HERO;
            case "getObjective" -> Thing.HeroParty.Objective.KILL_PLAYER;
            case "getObjectiveTargetPlayerId" -> objectiveTarget;
            case "isStopped" -> stopped;
            case "isAtAssignedTaskTarget" -> atTarget;
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
