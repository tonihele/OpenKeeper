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
import java.lang.reflect.Proxy;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.ActionPointFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

/**
 * A hero sent to an action point waits there for the point's wait delay
 * (seconds) before moving on to the next waypoint, as in DKII.EXE (the
 * arrival state at 0x00472150 sets the wait timer, the patrol state at
 * 0x004721C0 holds the creature until it has passed). It goes to a random
 * tile of the point that it can get to (0x00471EB0), not to its middle.
 */
class SendToActionPointTest {

    private int objectiveActionPointId;
    private Thing.HeroParty.Objective objective = Thing.HeroParty.Objective.SEND_TO_ACTION_POINT;
    private ICreatureController creature;
    private Predicate<Point> reachable = tile -> true;

    private ICreatureController creature(int actionPointId) {
        objectiveActionPointId = actionPointId;
        creature = (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getObjective":
                    return objective;
                case "getObjectiveTargetActionPointId":
                    return objectiveActionPointId;
                case "setObjectiveTargetActionPointId":
                    objectiveActionPointId = (Integer) args[0];
                    return null;
                case "getOwnerId":
                    return (short) 1;
                case "getCreatureCoordinates":
                    return new Point(0, 0);
                default:
                    return null;
            }
        });
        return creature;
    }

    private SendToActionPoint task(int id, int waitDelay, int next) {
        return task(ActionPointFixtures.actionPoint(id, waitDelay, next));
    }

    private SendToActionPoint task(Thing.ActionPoint actionPoint) {
        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{INavigationService.class},
                (proxy, method, args) -> "findPath".equals(method.getName()) && reachable.test((Point) args[1]) ? new DefaultGraphPath<>() : null);
        return new SendToActionPoint(navigationService, null, new ActionPoint(actionPoint), creature);
    }

    @Test
    void headsForATileOfThePointThatTheCreatureCanGetTo() {
        creature(1);
        reachable = tile -> tile.equals(new Point(4, 10));

        SendToActionPoint task = task(ActionPointFixtures.actionPoint(1, 4, 9, 6, 11));

        assertEquals(new Point(4, 10), task.getTaskLocation());
    }

    @Test
    void headsForTheMiddleWhenNoTileOfThePointCanBeReached() {
        creature(1);
        reachable = tile -> false;

        SendToActionPoint task = task(ActionPointFixtures.actionPoint(1, 4, 9, 6, 11));

        assertEquals(new Point(5, 10), task.getTaskLocation());
    }

    @Test
    void keepsWaitingUntilTheWaitDelayHasPassed() {
        ICreatureController creature = creature(1);
        SendToActionPoint task = task(1, 5, 2);

        task.executeTask(creature, 0f);
        task.executeTask(creature, 4.9f);

        assertEquals(1, objectiveActionPointId);
        assertTrue(task.isValid(creature));
    }

    @Test
    void movesToTheNextWaypointAfterTheWaitDelay() {
        ICreatureController creature = creature(1);
        SendToActionPoint task = task(1, 5, 2);

        task.executeTask(creature, 5f);

        assertEquals(2, objectiveActionPointId);
        assertFalse(task.isValid(creature));
    }

    @Test
    void doesNotWaitWhenTheDelayIsZero() {
        ICreatureController creature = creature(1);
        SendToActionPoint task = task(1, 0, 2);

        task.executeTask(creature, 0f);

        assertEquals(2, objectiveActionPointId);
        assertFalse(task.isValid(creature));
    }

    @Test
    void lastWaypointAlsoWaitsThenLeavesNoActionPointToGoTo() {
        ICreatureController creature = creature(3);
        SendToActionPoint task = task(3, 2, 0);

        task.executeTask(creature, 1f);
        assertTrue(task.isValid(creature));

        task.executeTask(creature, 2f);
        assertEquals(0, objectiveActionPointId);
        assertFalse(task.isValid(creature));
    }

    @Test
    void isValidOnlyWhileTheCreatureHasTheObjectiveAndThePointAsTarget() {
        ICreatureController creature = creature(1);
        SendToActionPoint task = task(1, 5, 2);

        assertTrue(task.isValid(creature));

        objectiveActionPointId = 2;
        assertFalse(task.isValid(creature));

        objectiveActionPointId = 1;
        objective = Thing.HeroParty.Objective.NONE;
        assertFalse(task.isValid(creature));
    }

    @Test
    void withoutACreatureTheTaskIsValidUntilExecuted() {
        ICreatureController creature = creature(1);
        SendToActionPoint task = task(1, 0, 2);

        assertTrue(task.isValid(null));

        task.executeTask(creature, 0f);
        assertFalse(task.isValid(null));
    }
}
