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
package toniarts.openkeeper.view.control;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.GameObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static toniarts.openkeeper.tools.convert.map.GameObjectFixtures.gameObject;

/**
 * Which text an object shows when the cursor is on it
 */
class ObjectTooltipTest {

    @Test
    void creatureBedHasNoTooltipOfItsOwn() {

        // In the shipped data all the creature beds and the lair pillar have tooltip 0, which is "Jelly"
        GameObject bed = gameObject("Goblin Creature Bed", (short) 22, 0);

        assertNull(ObjectViewControl.getTooltipKey(bed));
    }

    @Test
    void objectWithATooltipShowsIt() {
        GameObject chicken = gameObject("Chicken", (short) 10, 1234);

        assertEquals("1234", ObjectViewControl.getTooltipKey(chicken));
    }

    @Test
    void goldShowsTheGoldTexts() {
        GameObject looseGold = gameObject("Gold", (short) 1, 0, GameObject.ObjectFlag.OBJECT_TYPE_GOLD);
        GameObject roomGold = gameObject("Gold (Room)", (short) 3, 0, GameObject.ObjectFlag.OBJECT_TYPE_GOLD);

        assertEquals("2544", ObjectViewControl.getTooltipKey(looseGold));
        assertEquals("2543", ObjectViewControl.getTooltipKey(roomGold));
    }

}
