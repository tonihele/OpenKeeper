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

/**
 * Builds level variable and creature fixtures for the combat experience tests.
 * Lives in {@code Creature}'s own package because its setters are
 * {@code protected} (parser-only).
 */
public final class CombatExperienceFixtures {

    private CombatExperienceFixtures() {
    }

    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    public static Creature creature(short creatureId, Creature.CreatureFlag... flags) {
        EnumSet<Creature.CreatureFlag> set = EnumSet.noneOf(Creature.CreatureFlag.class);
        for (Creature.CreatureFlag flag : flags) {
            set.add(flag);
        }

        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Creature " + creatureId);
        creature.setFlags(set);
        return creature;
    }

}
