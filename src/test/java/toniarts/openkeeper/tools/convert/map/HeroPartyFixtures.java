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

import java.util.EnumSet;
import java.util.List;

/**
 * Builds hero party fixtures for tests. Lives in {@code Thing}'s own package
 * because its setters are {@code protected} (parser-only).
 */
public final class HeroPartyFixtures {

    private HeroPartyFixtures() {
    }

    /**
     * Creates a hero party
     *
     * @param partyId the party ID
     * @param members the members
     * @return the hero party
     */
    public static Thing.HeroParty heroParty(short partyId, Thing.GoodCreature... members) {
        Thing.HeroParty heroParty = new Thing.HeroParty();
        heroParty.setId(partyId);
        heroParty.setName("Party " + partyId);
        heroParty.setHeroPartyMembers(List.of(members));
        return heroParty;
    }

    /**
     * Creates a hero party member
     *
     * @param flags the creature flags, such as the leader flag
     * @return the member
     */
    public static Thing.GoodCreature member(Thing.Creature.CreatureFlag... flags) {
        Thing.GoodCreature creature = new Thing.GoodCreature();
        creature.setFlags(flags.length > 0 ? EnumSet.of(flags[0], flags) : EnumSet.noneOf(Thing.Creature.CreatureFlag.class));
        return creature;
    }

}
