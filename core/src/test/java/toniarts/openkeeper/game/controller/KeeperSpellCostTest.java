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
package toniarts.openkeeper.game.controller;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.KeeperSpellFlag.CREATE_WORKER;
import static toniarts.openkeeper.tools.convert.map.KeeperSpell.KeeperSpellFlag.DONT_CAST_ON_CREATURES;
import static toniarts.openkeeper.tools.convert.map.KeeperSpellFixtures.keeperSpell;

/**
 * Values from the shipped data: Create Imp costs 1500, MINIMUM_IMP_THRESHOLD is
 * 4 (#638)
 */
class KeeperSpellCostTest {

    private static final int FREE_IMPS = 4;
    private static final KeeperSpell CREATE_IMP = keeperSpell("Create Imp", 1500, CREATE_WORKER, DONT_CAST_ON_CREATURES);
    private static final KeeperSpell HEAL = keeperSpell("Heal", 1000);

    @Test
    void createImpCostsTheBasePriceUpToTheFreeImps() {
        for (int imps = 0; imps <= FREE_IMPS; imps++) {
            assertEquals(1500, KeeperSpellCost.getManaCost(CREATE_IMP, imps, FREE_IMPS), imps + " imps");
        }
    }

    @Test
    void eachFurtherImpAddsTheBasePrice() {
        assertEquals(3000, KeeperSpellCost.getManaCost(CREATE_IMP, 5, FREE_IMPS));
        assertEquals(4500, KeeperSpellCost.getManaCost(CREATE_IMP, 6, FREE_IMPS));
        assertEquals(15000, KeeperSpellCost.getManaCost(CREATE_IMP, 13, FREE_IMPS));
    }

    @Test
    void thePriceGoesDownWhenImpsDie() {
        int withSevenImps = KeeperSpellCost.getManaCost(CREATE_IMP, 7, FREE_IMPS);
        int afterTwoDied = KeeperSpellCost.getManaCost(CREATE_IMP, 5, FREE_IMPS);

        assertEquals(6000, withSevenImps);
        assertEquals(3000, afterTwoDied);
    }

    @Test
    void otherSpellsKeepTheirPrice() {
        assertEquals(1000, KeeperSpellCost.getManaCost(HEAL, 0, FREE_IMPS));
        assertEquals(1000, KeeperSpellCost.getManaCost(HEAL, 20, FREE_IMPS));
    }

}
