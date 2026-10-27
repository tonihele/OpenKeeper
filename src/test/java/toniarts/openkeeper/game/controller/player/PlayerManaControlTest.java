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
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mana a keeper spends at once shows with the loss of that second, and the
 * gain is limited
 */
class PlayerManaControlTest {

    private final Keeper keeper = new Keeper(false, (short) 3);
    private final PlayerManaControl manaControl = new PlayerManaControl(keeper, Map.of(
            MiscType.MAXIMUM_MANA_THRESHOLD, TrapFiringFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000),
            MiscType.MAXIMUM_MANA_GAIN_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.MAXIMUM_MANA_GAIN_PER_SECOND, 500)));

    @Test
    void gainUnderTheLimitIsKept() {
        manaControl.updateMana(130, 7, 0, 0);

        assertEquals(130, keeper.getManaGain());
        assertEquals(7, keeper.getManaLoose());
        assertEquals(123, keeper.getMana());
    }

    @Test
    void gainIsCappedAtTheMaximumManaGain() {
        manaControl.updateMana(800, 0, 0, 0);

        assertEquals(500, keeper.getManaGain());
        assertEquals(500, keeper.getMana());
    }

    @Test
    void upkeepIsPaidAfterTheCap() {
        manaControl.updateMana(800, 70, 0, 0);

        assertEquals(500, keeper.getManaGain());
        assertEquals(430, keeper.getMana());
    }

    @Test
    void ownedTilesRaiseTheCap() {
        manaControl.updateMana(800, 0, 200, 0);

        assertEquals(700, keeper.getManaGain());
        assertEquals(700, keeper.getMana());
    }

    @Test
    void spentManaIsTakenAtOnceAndShowsWithTheLoss() {
        keeper.setMana(1000);
        manaControl.updateMana(20, 5, 0, 0);

        manaControl.spendMana(250);

        assertEquals(765, keeper.getMana());
        assertEquals(255, keeper.getManaLoose());
        assertEquals(20, keeper.getManaGain());
    }

    @Test
    void spentManaLeavesTheLossAtTheNextUpdate() {
        keeper.setMana(1000);
        manaControl.spendMana(250);

        manaControl.updateMana(20, 5, 0, 0);

        assertEquals(5, keeper.getManaLoose());

        // The spent mana isn't taken a second time
        assertEquals(765, keeper.getMana());
    }

    @Test
    void possessionUnderTheGainLimitCostsNothing() {
        keeper.setMana(1000);

        // A Bile Demon costs 300 a second, it is in the loss too
        manaControl.updateMana(130, 300, 0, 300);

        assertEquals(430, keeper.getManaGain());
        assertEquals(300, keeper.getManaLoose());
        assertEquals(1130, keeper.getMana());
    }

    @Test
    void possessionCostsWhatGoesOverTheGainLimit() {
        keeper.setMana(1000);

        // A Vampire costs 600 a second, plus an imp's 7
        manaControl.updateMana(230, 607, 0, 600);

        assertEquals(500, keeper.getManaGain());
        assertEquals(607, keeper.getManaLoose());
        assertEquals(893, keeper.getMana());
    }

    @Test
    void possessionEndsWhenTheManaIsBelowTheCost() {
        keeper.setMana(600);
        assertTrue(manaControl.hasEnoughMana(600));

        keeper.setMana(599);
        assertFalse(manaControl.hasEnoughMana(600));
    }

    @Test
    void manaJustSpentDoesNotEndThePossession() {
        keeper.setMana(1100);

        // The 500 just spent shows in the loss until the next update, but it is gone from the mana already
        manaControl.spendMana(500);

        assertEquals(600, keeper.getMana());
        assertTrue(manaControl.hasEnoughMana(600));
    }

}
