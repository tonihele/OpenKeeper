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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.HeroPartyFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyControllerTest {

    private static final short TARGET_PLAYER = 3;
    private static final AtomicLong ENTITY_IDS = new AtomicLong(1);

    @Test
    void usesOnlyTheFlaggedLeadersOrder() {
        Thing.GoodCreature leaderDefinition = HeroPartyFixtures.member((short) 14, true,
                Thing.HeroParty.Objective.STEAL_GOLD, TARGET_PLAYER, 9, 88);
        Thing.GoodCreature followerDefinition = HeroPartyFixtures.member((short) 18, false,
                Thing.HeroParty.Objective.KILL_PLAYER, (short) 1, 2, 0);
        PartyController party = new PartyController(HeroPartyFixtures.party((short) 4,
                leaderDefinition, followerDefinition));
        CreatureStub leader = creature(false, false, false);
        CreatureStub follower = creature(false, false, false);
        party.create();
        party.addMemberInstance(leaderDefinition, leader.controller());
        party.addMemberInstance(followerDefinition, follower.controller());

        assertSame(leader.controller(), party.getPartyLeader());
        assertEquals(Thing.HeroParty.Objective.STEAL_GOLD, party.getObjectiveOrder().objective());
        assertEquals(TARGET_PLAYER, party.getObjectiveOrder().targetPlayerId());
        assertEquals(9, party.getObjectiveOrder().actionPointId());
        assertEquals(party.getObjectiveOrder(), leader.appliedOrder());
        assertNull(follower.appliedOrder());
        assertEquals(List.of(leader.controller(), follower.controller()), List.copyOf(party.getActualMembers()));
    }

    @Test
    void successorReceivesTheCompleteOrderAfterLeaderIsIncapacitated() {
        Thing.GoodCreature leaderDefinition = HeroPartyFixtures.member((short) 14, true,
                Thing.HeroParty.Objective.WAIT, TARGET_PLAYER, 7, 42);
        Thing.GoodCreature successorDefinition = HeroPartyFixtures.member((short) 18, false,
                Thing.HeroParty.Objective.KILL_PLAYER, (short) 1, 2, 0);
        PartyController party = new PartyController(HeroPartyFixtures.party((short) 4,
                leaderDefinition, successorDefinition));
        CreatureStub leader = creature(true, false, false);
        CreatureStub successor = creature(false, false, false);
        party.create();
        party.addMemberInstance(leaderDefinition, leader.controller());
        party.addMemberInstance(successorDefinition, successor.controller());
        PartyObjectiveOrder order = new PartyObjectiveOrder(Thing.HeroParty.Objective.WAIT,
                TARGET_PLAYER, 7, new Point(12, 8));
        party.setObjectiveOrder(order);

        assertSame(successor.controller(), party.getPartyLeader());
        assertEquals(order, successor.appliedOrder());
    }

    @Test
    void firstMemberIsSafeFallbackWhenMapHasNoLeaderFlag() {
        Thing.GoodCreature firstDefinition = HeroPartyFixtures.member((short) 14, false,
                Thing.HeroParty.Objective.KILL_CREATURES, TARGET_PLAYER, 0, 0);
        Thing.GoodCreature secondDefinition = HeroPartyFixtures.member((short) 18, false,
                Thing.HeroParty.Objective.STEAL_GOLD, (short) 1, 0, 0);
        PartyController party = new PartyController(HeroPartyFixtures.party((short) 4,
                firstDefinition, secondDefinition));
        CreatureStub first = creature(false, false, false);
        CreatureStub second = creature(false, false, false);
        party.create();
        party.addMemberInstance(firstDefinition, first.controller());
        party.addMemberInstance(secondDefinition, second.controller());

        assertSame(first.controller(), party.getPartyLeader());
        assertEquals(Thing.HeroParty.Objective.KILL_CREATURES, party.getObjectiveOrder().objective());
    }

    @Test
    void missingLeaderInstancePromotesAnActiveMemberAndCopiesTheOrder() {
        Thing.GoodCreature leaderDefinition = HeroPartyFixtures.member((short) 14, true,
                Thing.HeroParty.Objective.KILL_CREATURES, TARGET_PLAYER, 3, 17);
        Thing.GoodCreature followerDefinition = HeroPartyFixtures.member((short) 18, false,
                Thing.HeroParty.Objective.STEAL_GOLD, (short) 1, 0, 0);
        PartyController party = new PartyController(HeroPartyFixtures.party((short) 4,
                leaderDefinition, followerDefinition));
        CreatureStub follower = creature(false, false, false);
        party.create();
        party.addMemberInstance(followerDefinition, follower.controller());

        assertSame(follower.controller(), party.getPartyLeader());
        assertEquals(Thing.HeroParty.Objective.KILL_CREATURES, party.getObjectiveOrder().objective());
        assertEquals(TARGET_PLAYER, party.getObjectiveOrder().targetPlayerId());
        assertEquals(3, party.getObjectiveOrder().actionPointId());
        assertEquals(party.getObjectiveOrder(), follower.appliedOrder());
    }

    @Test
    void emptyOrFullyIncapacitatedPartyHasNoLeader() {
        PartyController empty = new PartyController(HeroPartyFixtures.party((short) 4));
        assertNull(empty.getPartyLeader());
        assertFalse(empty.isPartyLeader(null));

        Thing.GoodCreature definition = HeroPartyFixtures.member((short) 14, true,
                Thing.HeroParty.Objective.WAIT, TARGET_PLAYER, 0, 0);
        PartyController incapacitated = new PartyController(HeroPartyFixtures.party((short) 5, definition));
        CreatureStub creature = creature(true, false, false);
        incapacitated.create();
        incapacitated.addMemberInstance(definition, creature.controller());

        assertNull(incapacitated.getPartyLeader());
    }

    private static CreatureStub creature(boolean incapacitated, boolean dead, boolean removed) {
        boolean[] incapacitatedState = {incapacitated};
        boolean[] deadState = {dead};
        boolean[] removedState = {removed};
        PartyObjectiveOrder[] order = {null};
        ICreatureController controller = (ICreatureController) Proxy.newProxyInstance(
                PartyControllerTest.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isIncapacitated" -> incapacitatedState[0];
                    case "isDead" -> deadState[0];
                    case "isRemoved" -> removedState[0];
                    case "setPartyObjectiveOrder" -> {
                        order[0] = (PartyObjectiveOrder) args[0];
                        yield null;
                    }
                    case "getEntityId" -> new EntityId(ENTITY_IDS.getAndIncrement());
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });
        return new CreatureStub(controller, order);
    }

    private record CreatureStub(ICreatureController controller, PartyObjectiveOrder[] order) {

        private PartyObjectiveOrder appliedOrder() {
            return order[0];
        }
    }
}
