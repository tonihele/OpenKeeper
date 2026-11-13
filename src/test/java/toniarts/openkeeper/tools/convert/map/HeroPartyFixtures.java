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
package toniarts.openkeeper.tools.convert.map;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Test builders for parser-owned hero-party records. */
public final class HeroPartyFixtures {

    private HeroPartyFixtures() {
    }

    public static Thing.HeroParty party(short id, Thing.GoodCreature... members) {
        Thing.HeroParty party = new Thing.HeroParty();
        party.setId(id);
        party.setName("Test party " + id);
        party.setHeroPartyMembers(new ArrayList<>(List.of(members)));
        return party;
    }

    public static Thing.GoodCreature member(short creatureId, boolean leader,
            Thing.HeroParty.Objective objective, short targetPlayerId, int actionPointId, int triggerId) {
        Thing.GoodCreature member = new Thing.GoodCreature();
        member.setCreatureId(creatureId);
        member.setFlags(leader
                ? EnumSet.of(Thing.Creature.CreatureFlag.LEADER)
                : EnumSet.noneOf(Thing.Creature.CreatureFlag.class));
        member.setObjective(objective);
        member.setObjectiveTargetPlayerId(targetPlayerId);
        member.setObjectiveTargetActionPointId(actionPointId);
        member.setTriggerId(triggerId);
        return member;
    }
}
