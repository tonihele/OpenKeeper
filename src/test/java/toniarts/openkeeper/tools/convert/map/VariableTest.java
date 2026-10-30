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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The level variables are read in the units DK II uses them in
 */
class VariableTest {

    @Test
    void levelRatingKeepsItsFraction() {

        // Level 1 stores 6501, 20.12 fixed point
        assertEquals(6501 / 4096f, value(Variable.MiscVariable.MiscType.LEVEL_RATING, 6501));
    }

    @Test
    void averageTimeIsWholeMinutes() {
        assertEquals(8f, value(Variable.MiscVariable.MiscType.AVERAGE_TIME, 8));
    }

    private static float value(Variable.MiscVariable.MiscType type, int raw) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(raw);
        return variable.getValue();
    }
}
