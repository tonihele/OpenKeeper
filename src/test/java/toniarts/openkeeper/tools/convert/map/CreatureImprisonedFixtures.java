/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import java.util.List;

/** Original synthetic definitions for prison-admission integration tests. */
public final class CreatureImprisonedFixtures {
    private CreatureImprisonedFixtures() { }

    public static Creature creature() {
        Creature creature=CreatureSleepFixtures.sleeper(1);
        creature.getAttributes().setHp(100);
        creature.setSpells(List.of());
        creature.setMeleeAttackType(Creature.AttackType.NONE);
        return creature;
    }

    public static Thing.GoodCreature hero() {
        Thing.GoodCreature hero=CreatureAngryFixtures.thing(1);
        hero.setCreatureId((short)6);hero.setLevel((short)1);
        hero.setPosX(1);hero.setPosY(1);hero.setInitialHealth(100);
        return hero;
    }

    public static Thing.DeadBody body() {
        Thing.DeadBody body=new Thing.DeadBody();
        body.setCreatureId((short)6);body.setPlayerId((short)1);
        body.setPosX(1);body.setPosY(1);
        return body;
    }
}
