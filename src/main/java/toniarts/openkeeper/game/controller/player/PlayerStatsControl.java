/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * A class to hold player's miscellaneous statistics
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerStatsControl {

    private static final int NO_SLAPS_LIMIT = 0;

    private int payDays;
    private int slapsLimit = NO_SLAPS_LIMIT;
    private int slapsSinceLimit;
    private final Map<Creature, Integer> slapsMap = new HashMap<>();
    private final Map<Creature, Integer> pickUpsMap = new HashMap<>();
    private final Map<Creature, Integer> dropsMap = new HashMap<>();

    public boolean hasSlapped() {
        return !slapsMap.isEmpty();
    }

    public boolean hasSlapped(Creature creature) {
        return slapsMap.containsKey(creature);
    }

    /**
     * Counts a pay day of the player
     */
    public void payDay() {
        payDays++;
    }

    /**
     * Get how many pay days the player has had
     *
     * @return the pay days
     */
    public int getPayDays() {
        return payDays;
    }

    public void creatureSlapped(Creature creature) {
        increaseMapCount(creature, slapsMap);
    }

    /**
     * Sets how many times the player may slap from now on
     *
     * @param limit the number of slaps, 0 for no limit
     */
    public void setSlapsLimit(int limit) {

        // An unsigned limit too big for an int comes in negative, take it as no limit
        slapsLimit = Math.max(NO_SLAPS_LIMIT, limit);
        slapsSinceLimit = 0;
    }

    /**
     * Uses one slap of the player, if the limit allows it
     *
     * @return whether the player may slap
     */
    public boolean tryUseSlap() {
        if (slapsLimit == NO_SLAPS_LIMIT) {
            return true;
        }
        if (slapsSinceLimit >= slapsLimit) {
            return false;
        }
        slapsSinceLimit++;
        return true;
    }

    private void increaseMapCount(Creature creature, Map<Creature, Integer> map) {
        Integer count = map.get(creature);
        if (count == null) {
            count = 0;
        }
        count++;
        map.put(creature, count);
    }

    public boolean hasPickedUp() {
        return !pickUpsMap.isEmpty();
    }

    public boolean hasPickedUp(Creature creature) {
        return pickUpsMap.containsKey(creature);
    }

    public void creaturePickedUp(Creature creature) {
        increaseMapCount(creature, pickUpsMap);
    }

    public boolean hasDropped() {
        return !dropsMap.isEmpty();
    }

    public boolean hasDropped(Creature creature) {
        return dropsMap.containsKey(creature);
    }

    public void creatureDropped(Creature creature) {
        increaseMapCount(creature, dropsMap);
    }

}
