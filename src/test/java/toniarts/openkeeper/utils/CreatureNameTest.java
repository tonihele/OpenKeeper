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
package toniarts.openkeeper.utils;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The names and the blood types of the creatures, unique within an owner like
 * in DK II
 */
class CreatureNameTest {

    @Test
    void givesEveryCreatureOfAnOwnerItsOwnNameUntilThePoolOfFiveHundredRunsOut() {
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            assertTrue(taken.add(Utils.generateCreatureName(taken)), "A name was given twice");
        }
        assertTrue(taken.contains("Spike"));

        // With every name taken a creature still gets one
        assertTrue(taken.contains(Utils.generateCreatureName(taken)));
    }

    @Test
    void givesEveryCreatureOfAnOwnerItsOwnBloodTypeUntilTheSevenRunOut() {
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            assertTrue(taken.add(Utils.generateBloodType(taken)), "A blood type was given twice");
        }
        assertEquals(Set.of("ARh+", "O", "MoO+", "BA", "PoE", "BO", "IkI"), taken);
        assertTrue(taken.contains(Utils.generateBloodType(taken)));
    }

}
