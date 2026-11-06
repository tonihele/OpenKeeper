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
 * Builds the {@link Creature} and {@link Variable} fixtures the fog of war
 * tests need. Lives in their own package because their setters are
 * {@code protected} (parser-only).
 */
public final class FogVisionFixtures {

    private FogVisionFixtures() {
    }

    /**
     * A creature with only its sight attributes set
     *
     * @param creatureId the creature ID
     * @param distanceCanSee how far the creature explores, in tiles
     * @param perceptionRange how far the creature perceives, in tiles
     * @return the creature
     */
    public static Creature creature(short creatureId, float distanceCanSee, float perceptionRange) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.getAttributes().setDistanceCanSee(distanceCanSee);
        creature.getAttributes().setPerceptionRange(perceptionRange);
        return creature;
    }

    /**
     * A level variable
     *
     * @param type the variable
     * @param value its value
     * @return the variable
     */
    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

}
