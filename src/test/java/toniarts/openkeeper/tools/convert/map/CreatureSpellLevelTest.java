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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * From which level a creature has its spells
 */
class CreatureSpellLevelTest {

    @Test
    void spellComesAtItsLevel() {

        // The Firefly's Wind comes at level 8 in the original data
        Creature.Spell wind = spell(8);

        assertFalse(wind.isAvailableAt(1));
        assertFalse(wind.isAvailableAt(7));
        assertTrue(wind.isAvailableAt(8));
    }

    @Test
    void spellStaysOnTheHigherLevels() {

        // The Warlock's Fireball comes at level 1 and is still there at level 10
        Creature.Spell fireball = spell(1);

        assertTrue(fireball.isAvailableAt(1));
        assertTrue(fireball.isAvailableAt(10));
    }

    private static Creature.Spell spell(int levelAvailable) {
        Creature.Spell spell = new Creature.Spell();
        spell.setLevelAvailable((short) levelAvailable);
        return spell;
    }

}
