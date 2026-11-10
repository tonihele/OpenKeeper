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
 * Builds the graveyard related game data for tests, the setters are not
 * public
 */
public final class GraveyardFixtures {

    private GraveyardFixtures() {
        // Nope
    }

    /**
     * Creates a creature type
     *
     * @param creatureId the creature ID
     * @param flags the creature flags
     * @return the creature
     */
    public static Creature creature(short creatureId, Creature.CreatureFlag... flags) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Creature " + creatureId);
        EnumSet<Creature.CreatureFlag> set = EnumSet.noneOf(Creature.CreatureFlag.class);
        for (Creature.CreatureFlag flag : flags) {
            set.add(flag);
        }
        creature.setFlags(set);
        return creature;
    }

    /**
     * Creates the graveyard room type
     *
     * @param createdCreatureId the creature the room creates
     * @return the room
     */
    public static Room graveyard(short createdCreatureId) {
        Room room = new Room();
        room.setName("Graveyard");
        room.setRoomId((short) 14);
        room.setCreatedCreatureId(createdCreatureId);
        return room;
    }

    /**
     * Creates a level variable
     *
     * @param type the variable type
     * @param value the value
     * @return the variable
     */
    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

}
