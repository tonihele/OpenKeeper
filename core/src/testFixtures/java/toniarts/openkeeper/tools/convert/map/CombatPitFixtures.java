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

import java.util.Collections;
import java.util.EnumSet;

/**
 * Builds {@link Creature} and level variable fixtures for the Combat Pit
 * tests. Lives in their own package because their setters are
 * {@code protected}.
 */
public final class CombatPitFixtures {

    private CombatPitFixtures() {
    }

    public static Creature pitCreature(short creatureId) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Pit creature " + creatureId);
        creature.setFightStyle(Creature.FightStyle.BLOCKER);
        creature.setFlags(EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY));
        return creature;
    }

    /**
     * A Combat Pit room without objects
     */
    public static Room pitRoom() {
        Room room = RoomFixtures.room((short) 16, 0);
        room.setObjects(Collections.nCopies(6, (short) 0));
        return room;
    }

    public static Variable.MiscVariable pitVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

}
