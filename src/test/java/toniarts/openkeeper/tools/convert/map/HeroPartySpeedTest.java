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

import com.jme3.math.Vector2f;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.PartyType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A hero party walks at the speed of its slowest member (DKII.EXE 0x519F90). */
class HeroPartySpeedTest {

    @Test
    void partyWalksAtTheSpeedOfItsSlowestMember() {
        Creature dwarf = creature((short) 14, 0.9f);
        Creature thief = creature((short) 18, 0.6f);
        Thing.HeroParty party = new Thing.HeroParty();
        party.setId((short) 3);
        party.setHeroPartyMembers(List.of(member(dwarf, Thing.Creature.CreatureFlag.LEADER),
                member(thief, Thing.Creature.CreatureFlag.FOLLOWER)));
        IKwdFile level = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getThings" -> args[0] == Thing.HeroParty.class ? List.of(party) : List.of();
            case "getCreature" -> ((Short) args[0]) == 14 ? dwarf : thief;
            case "getImp" -> new Creature();
            default -> null;
        });
        DefaultEntityData entities = new DefaultEntityData();
        CreaturesController creatures = new CreaturesController(level, entities, Map.of(),
                stub(IGameTimer.class, (method, args) -> null), stub(IGameController.class, (method, args) ->
                        method.equals("getGameWorldController") ? stub(IGameWorldController.class, (m, a) -> null) : null),
                stub(IMapController.class, (method, args) -> null), stub(ILevelInfo.class, (method, args) -> null), null);

        creatures.spawnHeroParty((short) 3, PartyType.NONE, new Vector2f(4, 20));

        var members = entities.getEntities(Party.class, Mobile.class);
        members.applyChanges();
        assertEquals(2, members.size());
        members.forEach(member -> assertEquals(0.6f, member.get(Mobile.class).maxSpeed, 0.0001f));
    }

    private static Creature creature(short id, float speed) {
        Creature creature = new Creature();
        creature.setCreatureId(id);
        creature.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
        creature.setMeleeAttackType(Creature.AttackType.MELEE_BLUNT);
        creature.setSpells(List.of());
        creature.getAttributes().setSpeed(speed);
        return creature;
    }

    private static Thing.GoodCreature member(Creature creature, Thing.Creature.CreatureFlag role) {
        Thing.GoodCreature member = new Thing.GoodCreature();
        member.setCreatureId(creature.getCreatureId());
        member.setLevel((short) 1);
        member.setInitialHealth(100);
        member.setFlags(EnumSet.of(role));
        return member;
    }

    private interface Calls {
        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Object result = calls.call(method.getName(), args);
                    Class<?> returnType = method.getReturnType();

                    // Default primitives, e.g. a game time of 0
                    return result != null || !returnType.isPrimitive() || returnType == void.class
                            ? result : Array.get(Array.newInstance(returnType, 1), 0);
                }));
    }
}
