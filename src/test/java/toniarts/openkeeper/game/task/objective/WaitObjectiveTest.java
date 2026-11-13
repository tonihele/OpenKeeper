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
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.creature.PartyObjectiveOrder;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitObjectiveTest {

    private static final Point HOLD_POSITION = new Point(7, 9);

    @Test
    void holdsAtTheOrderPositionAndStaysValidAfterAnInterruption() {
        Thing.HeroParty.Objective[] objective = {Thing.HeroParty.Objective.WAIT};
        PartyObjectiveOrder order = new PartyObjectiveOrder(objective[0], (short) 3, 0, HOLD_POSITION);
        IPartyController party = party(order, true);
        ICreatureController leader = creature(objective, party);
        Wait task = new Wait(null, null, HOLD_POSITION, (short) 0);

        assertEquals(HOLD_POSITION, task.getTaskLocation());
        assertTrue(task.isValid(leader));

        // FIGHT/FLEE unassigns movement work, then IDLE assigns the same party order again.
        task.executeTask(leader, 1f);
        assertTrue(task.isValid(leader));

        objective[0] = Thing.HeroParty.Objective.KILL_PLAYER;
        assertFalse(task.isValid(leader));
    }

    @Test
    void invalidatesWhenThePartyIssuesWaitAtANewPosition() {
        PartyObjectiveOrder[] order = {new PartyObjectiveOrder(Thing.HeroParty.Objective.WAIT,
                (short) 3, 0, HOLD_POSITION)};
        IPartyController party = party(order[0], true);
        ICreatureController leader = creature(new Thing.HeroParty.Objective[]{Thing.HeroParty.Objective.WAIT}, party);
        Wait task = new Wait(null, null, HOLD_POSITION, (short) 0);

        order[0] = order[0].withWaitPosition(new Point(8, 9));
        party = party(order[0], true);
        ICreatureController currentLeader = creature(new Thing.HeroParty.Objective[]{Thing.HeroParty.Objective.WAIT}, party);

        assertTrue(task.isValid(leader));
        assertFalse(task.isValid(currentLeader));
    }

    @Test
    void followersCannotOwnThePartyWaitTask() {
        IPartyController party = party(new PartyObjectiveOrder(Thing.HeroParty.Objective.WAIT,
                (short) 3, 0, HOLD_POSITION), false);
        ICreatureController follower = creature(new Thing.HeroParty.Objective[]{Thing.HeroParty.Objective.WAIT}, party);
        Wait task = new Wait(null, null, HOLD_POSITION, (short) 0);

        assertFalse(task.isValid(follower));
    }

    private static ICreatureController creature(Thing.HeroParty.Objective[] objective, IPartyController party) {
        return (ICreatureController) Proxy.newProxyInstance(WaitObjectiveTest.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObjective" -> objective[0];
                    case "getParty" -> party;
                    default -> null;
                });
    }

    private static IPartyController party(PartyObjectiveOrder order, boolean leader) {
        return (IPartyController) Proxy.newProxyInstance(WaitObjectiveTest.class.getClassLoader(),
                new Class<?>[]{IPartyController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObjectiveOrder" -> order;
                    case "isPartyLeader" -> leader;
                    default -> null;
                });
    }
}
