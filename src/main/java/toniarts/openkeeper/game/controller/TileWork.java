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

import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * The arithmetic of tile work: digging, mining, claiming, reinforcing and
 * repairing. Measured against the original 1.70, see
 * <a href="https://github.com/tonihele/OpenKeeper/issues/559">#559</a>.
 */
public final class TileWork {

    /**
     * Efficiency of a worker that has no efficiency of its own
     */
    public static final int FULL_EFFICIENCY = 100;

    private TileWork() {
        // Nope
    }

    /**
     * How much one application of work does. The work variables in the
     * level data (dig rock health, claim tile health and so on) are rates per
     * game second, and the worker's efficiency scales them, 80% for a fresh
     * imp. The original applies a quarter of the rate four times a second;
     * our tile tasks apply once a second, so one application is the full
     * rate.
     *
     * @param ratePerSecond the work variable, the sign is ignored
     * @param efficiencyPercentage the worker's efficiency
     * @return the health change of one application, never negative
     */
    public static int perApplication(float ratePerSecond, int efficiencyPercentage) {
        return Math.round(Math.abs(ratePerSecond) * efficiencyPercentage / 100f);
    }

    /**
     * The gold a seam has released by the time its health has fallen to the
     * given value. Mining damages the seam's health like any dig, and the
     * gold comes out in proportion. Gold per swing is the difference of two
     * of these, which keeps the total exact: a seam of 3000 gold releases
     * exactly 3000.
     *
     * @param totalGold the seam's gold
     * @param maxHealth the seam's maximum health
     * @param health the seam's health now
     * @return the gold released so far
     */
    public static int goldReleased(int totalGold, int maxHealth, int health) {
        if (maxHealth <= 0) {
            return totalGold;
        }
        return (int) ((long) totalGold * (maxHealth - health) / maxHealth);
    }

    /**
     * A seam after one swing of mining
     *
     * @param health the seam's health
     * @param gold the gold left in the seam
     * @param released the gold this swing releases
     */
    public record Mining(int health, int gold, int released) {

        public boolean isExhausted() {
            return health == 0;
        }
    }

    /**
     * Mines a seam for one swing. The swing damages the seam and releases gold
     * in proportion (see {@link #goldReleased(int, int, int)}), whatever the
     * miner carries already. As in the original, the gold a full miner can't
     * carry falls to the floor, and the seam collapses once its health is
     * gone.
     *
     * @param totalGold the seam's gold when untouched
     * @param maxHealth the seam's maximum health
     * @param health the seam's health now
     * @param gold the gold left in the seam now
     * @param damage the damage of the swing
     * @return the seam after the swing
     */
    public static Mining mine(int totalGold, int maxHealth, int health, int gold, int damage) {
        int newHealth = Math.max(0, health - damage);
        int newGold = Math.min(gold, totalGold - goldReleased(totalGold, maxHealth, newHealth));

        return new Mining(newHealth, newGold, gold - newGold);
    }

    /**
     * Whether work that brings the terrain to full health turns it into
     * another terrain. That is the upward job of terrain nobody owns: rock is
     * reinforced into a wall, a dirt path or a mana vault is claimed. Terrain
     * that is owned already is only repaired at full health. A claimed path
     * for example names rock as its full-health terrain, and following that
     * would turn a repaired floor into rock.
     *
     * @param terrain the terrain worked on
     * @return true if full health converts it
     */
    public static boolean convertsAtFullHealth(Terrain terrain) {
        return !terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && terrain.getMaxHealthTypeTerrainId() != 0
                && terrain.getMaxHealthTypeTerrainId() != terrain.getTerrainId();
    }

}
