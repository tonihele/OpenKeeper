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
 * Builds {@link Creature} fixtures for the combat tests. Lives in
 * {@code Creature}'s own package because its setters are {@code protected}.
 */
public final class CombatCreatureFixtures {

    private CombatCreatureFixtures() {
    }

    public static Creature fighter(short creatureId, Creature.FightStyle fightStyle, Creature.CreatureFlag... flags) {
        EnumSet<Creature.CreatureFlag> set = EnumSet.noneOf(Creature.CreatureFlag.class);
        for (Creature.CreatureFlag flag : flags) {
            set.add(flag);
        }

        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Fighter " + creatureId);
        creature.setFightStyle(fightStyle);
        creature.setFlags(set);
        creature.setSpells(List.of());
        creature.getAttributes().setDistanceCanSee(16);
        return creature;
    }

    public static Creature withSightDistance(Creature creature, float distance) {
        creature.getAttributes().setDistanceCanSee(distance);
        return creature;
    }

    public static CreatureSpell attackSpell(CreatureSpell.CreatureSpellFlag... flags) {
        CreatureSpell spell = new CreatureSpell();
        EnumSet<CreatureSpell.CreatureSpellFlag> set = EnumSet.of(CreatureSpell.CreatureSpellFlag.IS_ATTACKING);
        for (CreatureSpell.CreatureSpellFlag flag : flags) {
            set.add(flag);
        }
        spell.setFlags(set);
        return spell;
    }

}
