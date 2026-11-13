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

/**
 * Builds the level data the Portal creature generation tests need. Lives in
 * the data classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class PortalGenerationFixtures {

    private PortalGenerationFixtures() {
    }

    /**
     * A creature without attraction requirements
     */
    public static Creature creature(short creatureId) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setAttractions(new Creature.Attraction[0]);
        return creature;
    }

    /**
     * A 3 by 3 room, built like the Portal
     */
    public static Room portal(short roomId) {
        Room room = new Room();
        room.setRoomId(roomId);
        room.setTileConstruction(Room.TileConstruction._3_BY_3);
        return room;
    }

    public static Variable.CreaturePool creaturePool(int value) {
        Variable.CreaturePool pool = new Variable.CreaturePool();
        pool.setValue(value);
        return pool;
    }

    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

}
