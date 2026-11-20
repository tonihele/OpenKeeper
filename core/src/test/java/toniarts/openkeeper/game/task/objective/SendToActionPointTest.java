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

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.tools.convert.map.ActionPointFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * A hero sent to an action point waits there for the point's wait delay
 * (seconds) before moving on to the next waypoint, as in DKII.EXE (the
 * arrival state at 0x00472150 sets the wait timer, the patrol state at
 * 0x004721C0 holds the creature until it has passed).
 */
class SendToActionPointTest {

    private int objectiveActionPointId;
    private Thing.HeroParty.Objective objective = Thing.HeroParty.Objective.SEND_TO_ACTION_POINT;

    private ICreatureController creature(int actionPointId) {
        objectiveActionPointId = actionPointId;
        return (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getObjective":
                    return objective;
                case "getObjectiveTargetActionPointId":
                    return objectiveActionPointId;
                case "setObjectiveTargetActionPointId":
                    objectiveActionPointId = (Integer) args[0];
                    return null;
                default:
                    return null;
            }
        });
    }

    private SendToActionPoint task(int id, int waitDelay, int next) {
        return new SendToActionPoint(null, null, new ActionPoint(ActionPointFixtures.actionPoint(id, waitDelay, next)), (short) 4);
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
    void lastWaypointAlsoWaitsAndLeavesTheObjectiveTargetAlone() {
        ICreatureController creature = creature(3);
        SendToActionPoint task = task(3, 2, 0);

        task.executeTask(creature, 1f);
        assertTrue(task.isValid(creature));

        task.executeTask(creature, 2f);
        assertEquals(3, objectiveActionPointId);
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
