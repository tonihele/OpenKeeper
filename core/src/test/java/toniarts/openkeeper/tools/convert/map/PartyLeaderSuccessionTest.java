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

import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.PartyController;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * When a hero party's leader falls, the first standing member with the
 * follower flag takes over (DKII.EXE 0x4B2CE0).
 */
class PartyLeaderSuccessionTest {

    private boolean leaderDown;
    private boolean firstFollowerDown;

    @Test
    void firstFollowerInPartyOrderBecomesLeader() {
        Thing.GoodCreature[] things = {member(Thing.Creature.CreatureFlag.LEADER), member(Thing.Creature.CreatureFlag.WILL_FIGHT),
            member(Thing.Creature.CreatureFlag.FOLLOWER), member(Thing.Creature.CreatureFlag.FOLLOWER)};
        ICreatureController[] heroes = {hero(() -> leaderDown), hero(() -> false), hero(() -> firstFollowerDown), hero(() -> false)};
        PartyController party = party(things, heroes);

        assertSame(heroes[0], party.getPartyLeader());
        leaderDown = true;
        assertSame(heroes[2], party.getPartyLeader());
    }

    @Test
    void deadFollowerIsSkipped() {
        Thing.GoodCreature[] things = {member(Thing.Creature.CreatureFlag.LEADER),
            member(Thing.Creature.CreatureFlag.FOLLOWER), member(Thing.Creature.CreatureFlag.FOLLOWER)};
        ICreatureController[] heroes = {hero(() -> leaderDown), hero(() -> firstFollowerDown), hero(() -> false)};
        PartyController party = party(things, heroes);

        leaderDown = true;
        firstFollowerDown = true;
        assertSame(heroes[2], party.getPartyLeader());
    }

    @Test
    void membersWithoutTheFollowerFlagAreNeverPicked() {
        Thing.GoodCreature[] things = {member(Thing.Creature.CreatureFlag.LEADER), member(Thing.Creature.CreatureFlag.WILL_FIGHT)};
        ICreatureController[] heroes = {hero(() -> leaderDown), hero(() -> false)};
        PartyController party = party(things, heroes);

        leaderDown = true;
        assertSame(heroes[0], party.getPartyLeader());
    }

    private static PartyController party(Thing.GoodCreature[] things, ICreatureController[] heroes) {
        Thing.HeroParty heroParty = new Thing.HeroParty();
        heroParty.setHeroPartyMembers(List.of(things));
        PartyController party = new PartyController(heroParty);
        for (int i = 0; i < things.length; i++) {
            party.addMemberInstance(things[i], heroes[i]);
        }
        return party;
    }

    private static Thing.GoodCreature member(Thing.Creature.CreatureFlag flag) {
        Thing.GoodCreature member = new Thing.GoodCreature();
        member.setFlags(EnumSet.of(flag));
        return member;
    }

    private static ICreatureController hero(java.util.function.BooleanSupplier incapacitated) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isIncapacitated" -> incapacitated.getAsBoolean();
                    case "getObjectiveTargetActionPointId" -> 0;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }

}
