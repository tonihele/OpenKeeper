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
 * Builds the creature fixtures for the Portal creature choice tests. Lives in
 * the data classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class PortalChoiceFixtures {

    private PortalChoiceFixtures() {
    }

    /**
     * A creature with its flags, lair object and attraction requirements. It
     * is ordered by its id, as the keeper's available types are kept sorted.
     */
    public static Creature creature(short creatureId, short lairObjectId, EnumSet<Creature.CreatureFlag> flags,
            EnumSet<Creature.CreatureFlag2> flags2, Creature.Attraction... attractions) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setOrderInEditor(creatureId);
        creature.setLairObjectId(lairObjectId);
        creature.setFlags(flags);
        creature.setFlags2(flags2);
        creature.setAttractions(attractions);
        return creature;
    }

}
