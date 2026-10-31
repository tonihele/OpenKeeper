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
package toniarts.openkeeper.game.controller.player;

import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.VariableFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The mana a keeper spends at once shows with the loss of that second
 */
class PlayerManaControlTest {

    private final Keeper keeper = new Keeper(false, (short) 3);
    private final PlayerManaControl manaControl = new PlayerManaControl(keeper,
            Map.of(MiscType.MAXIMUM_MANA_THRESHOLD, VariableFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000)));

    @Test
    void spentManaIsTakenAtOnce() {
        keeper.setMana(1000);

        manaControl.spendMana(250);

        assertEquals(750, keeper.getMana());
    }

    @Test
    void spentManaShowsWithTheLossOfThatSecond() {
        keeper.setMana(1000);
        manaControl.spendMana(250);

        manaControl.updateMana(20, 5);

        assertEquals(255, keeper.getManaLoose());
        assertEquals(20, keeper.getManaGain());

        // The spent mana isn't taken a second time
        assertEquals(765, keeper.getMana());
    }

    @Test
    void spentManaShowsOnlyOnce() {
        keeper.setMana(1000);
        manaControl.spendMana(250);
        manaControl.updateMana(20, 5);

        manaControl.updateMana(20, 5);

        assertEquals(5, keeper.getManaLoose());
    }

}
