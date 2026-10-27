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
 * Builds the level variables and creature spells a possessed creature's
 * attack reads, their setters are protected
 *
 * @author OpenKeeper
 */
public final class PossessionFixtures {

    private PossessionFixtures() {
    }

    public static Variable.CreatureFirstPerson firstPersonStat(Variable.CreatureStats.StatType statType, int value) {
        Variable.CreatureFirstPerson stat = new Variable.CreatureFirstPerson();
        stat.setStatId(statType);
        stat.setValue(value);
        return stat;
    }

    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    public static void setSpells(Creature creature, List<Creature.Spell> spells) {
        creature.setSpells(spells);
    }

    public static void setAbilities(Creature creature, Creature.SpecialAbility ability1, Creature.SpecialAbility ability2) {
        creature.setFirstPersonSpecialAbility1(ability1);
        creature.setFirstPersonSpecialAbility2(ability2);
    }

    public static Creature.Spell spell(short creatureSpellId, int levelAvailable, float shotDelay) {
        Creature.Spell spell = new Creature.Spell();
        spell.setCreatureSpellId(creatureSpellId);
        spell.setLevelAvailable((short) levelAvailable);
        spell.setShotDelay(shotDelay);
        spell.setShotOffset(0, 0, 0);
        return spell;
    }

    public static CreatureSpell creatureSpell(short creatureSpellId, short shotTypeId, float rechargeTime, float range,
            EnumSet<CreatureSpell.CreatureSpellFlag> flags) {
        CreatureSpell creatureSpell = new CreatureSpell();
        creatureSpell.setCreatureSpellId(creatureSpellId);
        creatureSpell.setShotTypeId(shotTypeId);
        creatureSpell.setRechargeTime(rechargeTime);
        creatureSpell.setRange(range);
        creatureSpell.setFlags(flags);
        return creatureSpell;
    }

}
