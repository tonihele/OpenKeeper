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
 * Builds the data pay day needs in tests: creatures that get angry when they
 * are not paid, and the pay day level variables. Lives in the data classes'
 * own package because their setters are {@code protected} (parser-only).
 */
public final class PayDayFixtures {

    private PayDayFixtures() {
    }

    /**
     * A creature
     *
     * @param creatureId the creature ID
     * @param angerNoPay the anger the creature gets for not being paid
     * @return the creature
     */
    public static Creature creature(short creatureId, int angerNoPay) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.getAttributes().setAngerNoPay((short) angerNoPay);
        return creature;
    }

    /**
     * A level variable
     *
     * @param type the variable
     * @param value its value
     * @return the variable
     */
    public static Variable.MiscVariable payDayVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    /**
     * The level settings
     *
     * @param ticksPerSecond the logic ticks per second
     * @return the game level
     */
    public static GameLevel gameLevel(int ticksPerSecond) {
        GameLevel gameLevel = new GameLevel();
        gameLevel.setTicksPerSec(ticksPerSecond);
        return gameLevel;
    }

}
