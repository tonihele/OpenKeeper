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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;

/**
 * Values from the shipped GlobalVariables.kwd and Terrain.kwd, and the
 * measurements in #559
 */
class TileWorkTest {

    private static final int FRESH_IMP = 80;
    private static final int SEAM_GOLD = 3000;
    private static final int SEAM_HEALTH = 660;
    private static final int IMP_CAPACITY = 1400;

    @Test
    void workRatesAreScaledByEfficiency() {
        assertEquals(240, TileWork.perApplication(-300, FRESH_IMP)); // Mine gold
        assertEquals(660, TileWork.perApplication(825, FRESH_IMP)); // Claim tile
        assertEquals(104, TileWork.perApplication(-130, FRESH_IMP)); // Dig own wall
        assertEquals(40, TileWork.perApplication(50, FRESH_IMP)); // Reinforce wall
        assertEquals(270, TileWork.perApplication(-300, FRESH_IMP + 10)); // Slapped
        assertEquals(300, TileWork.perApplication(-300, TileWork.FULL_EFFICIENCY));
    }

    @Test
    void seamReleasesGoldInProportionLikeTheOriginal() {

        // The original: a quarter of the rate four times a second, 60 health a swing
        List<Integer> swings = mine(60);

        assertEquals(List.of(272, 273, 273, 272, 273, 273, 273, 272, 273, 273, 273), swings);
        assertEquals(SEAM_GOLD, swings.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void seamReleasesAllItsGoldAtOurCadence() {

        // Once a second, 240 health an application
        List<Integer> applications = mine(TileWork.perApplication(-300, FRESH_IMP));

        assertEquals(3, applications.size());
        assertEquals(SEAM_GOLD, applications.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void impMinesOnlyWhatItCanCarry() {
        int carried = 1300;

        TileWork.Mining mining = TileWork.mine(SEAM_GOLD, SEAM_HEALTH, SEAM_HEALTH, SEAM_GOLD, 240, IMP_CAPACITY - carried);

        // The swing releases about 1090, the imp takes the 100 it has room for and the rest stays in the seam
        assertEquals(100, mining.taken());
        assertEquals(SEAM_GOLD - 100, mining.gold());
        assertEquals(SEAM_HEALTH - 240, mining.health());
    }

    @Test
    void seamHoldsUntilItsGoldIsTaken() {

        // The last swing with a full imp: nothing taken, the seam stands with its gold
        TileWork.Mining mining = TileWork.mine(SEAM_GOLD, SEAM_HEALTH, 100, 500, 240, 0);

        assertEquals(0, mining.taken());
        assertEquals(500, mining.gold());
        assertEquals(1, mining.health());
        assertFalse(mining.isExhausted());

        // The next imp gets it all
        mining = TileWork.mine(SEAM_GOLD, SEAM_HEALTH, mining.health(), mining.gold(), 240, IMP_CAPACITY);

        assertEquals(500, mining.taken());
        assertTrue(mining.isExhausted());
    }

    @Test
    void wholeSeamGoesToTheTreasuryWithoutLeftovers() {
        int health = SEAM_HEALTH;
        int gold = SEAM_GOLD;
        int carried = 0;
        int delivered = 0;
        int swings = 0;
        while (health > 0) {
            TileWork.Mining mining = TileWork.mine(SEAM_GOLD, SEAM_HEALTH, health, gold, 240, IMP_CAPACITY - carried);
            health = mining.health();
            gold = mining.gold();
            carried += mining.taken();
            assertTrue(carried <= IMP_CAPACITY, "Carries " + carried);

            // Full, the imp only leaves for the treasury when exactly full
            if (carried == IMP_CAPACITY) {
                delivered += carried;
                carried = 0;
            }
            assertTrue(++swings < 100, "Stuck mining");
        }
        delivered += carried;

        assertEquals(SEAM_GOLD, delivered);
        assertEquals(0, gold);
    }

    @Test
    void upwardJobsConvertTerrainNobodyOwns() {
        assertTrue(TileWork.convertsAtFullHealth(terrain(2, 9, SOLID))); // Rock -> Reinforced Wall
        assertTrue(TileWork.convertsAtFullHealth(terrain(3, 8))); // Dirt Path -> Claimed Path
        assertTrue(TileWork.convertsAtFullHealth(terrain(31, 32))); // Mana Vault -> Claimed Vault
    }

    @Test
    void repairDoesNotConvert() {

        // Claimed Path names Rock as its full health terrain, a repaired floor must not turn into rock
        assertFalse(TileWork.convertsAtFullHealth(terrain(8, 2, OWNABLE)));
        assertFalse(TileWork.convertsAtFullHealth(terrain(9, 9, SOLID, OWNABLE))); // Reinforced Wall
        assertFalse(TileWork.convertsAtFullHealth(terrain(6, 6, SOLID))); // Gold, itself
        assertFalse(TileWork.convertsAtFullHealth(terrain(4, 4))); // Water, itself
    }

    private static List<Integer> mine(int damagePerApplication) {
        List<Integer> released = new ArrayList<>();
        int health = SEAM_HEALTH;
        while (health > 0) {
            int before = TileWork.goldReleased(SEAM_GOLD, SEAM_HEALTH, health);
            health = Math.max(0, health - damagePerApplication);
            released.add(TileWork.goldReleased(SEAM_GOLD, SEAM_HEALTH, health) - before);
        }
        return released;
    }

    private static Terrain terrain(int terrainId, int maxHealthTerrainId, Terrain.TerrainFlag... flags) {
        return TerrainFixtures.terrain((short) terrainId, TerrainFixtures.flags(flags), 0, 1, 660, (short) maxHealthTerrainId);
    }

}
