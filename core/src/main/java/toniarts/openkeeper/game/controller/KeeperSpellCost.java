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

import toniarts.openkeeper.tools.convert.map.KeeperSpell;

/**
 * The mana cost of casting a keeper spell. Shared by the server, which charges
 * the mana, and the client, which shows the price.<br>
 * Create Imp gets more expensive the more imps the keeper has: each imp beyond
 * the free ones the dungeon heart provides (MINIMUM_IMP_THRESHOLD, 4 in the
 * shipped data) adds the base cost once more. So with the shipped base cost of
 * 1500 the fifth imp costs 1500, the sixth 3000 and so on. When imps die the
 * price goes down again.
 *
 * @see <a href="https://dungeonkeeper.fandom.com/wiki/Create_Imp">Create Imp</a>
 */
public final class KeeperSpellCost {

    private KeeperSpellCost() {
        // Nope
    }

    /**
     * Get the mana cost of casting a keeper spell
     *
     * @param keeperSpell the spell
     * @param impCount the number of imps the keeper has
     * @param freeImps the number of imps the dungeon heart provides for free
     * (MINIMUM_IMP_THRESHOLD)
     * @return the mana cost
     */
    public static int getManaCost(KeeperSpell keeperSpell, int impCount, int freeImps) {
        return getManaCost(keeperSpell.getManaCost(), keeperSpell.getFlags().contains(KeeperSpell.KeeperSpellFlag.CREATE_WORKER),
                impCount, freeImps);
    }

    /**
     * Get the mana cost of casting a keeper spell
     *
     * @param baseCost the mana cost of the spell in the level data
     * @param createsWorker whether the spell creates a worker (Create Imp)
     * @param impCount the number of imps the keeper has
     * @param freeImps the number of imps the dungeon heart provides for free
     * (MINIMUM_IMP_THRESHOLD)
     * @return the mana cost
     */
    public static int getManaCost(int baseCost, boolean createsWorker, int impCount, int freeImps) {
        if (!createsWorker) {
            return baseCost;
        }
        return baseCost * (Math.max(0, impCount - freeImps) + 1);
    }

}
