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

    private int payDays;
    private int enemyCreaturesKilled;
    private int creaturesLost;
    private final Map<Short, Integer> creaturesKilledByOwner = new HashMap<>();
    private final Map<Creature, Integer> slapsMap = new HashMap<>();
    private final Map<Creature, Integer> pickUpsMap = new HashMap<>();
    private final Map<Creature, Integer> dropsMap = new HashMap<>();

    /**
     * Records an actual death, rather than a creature knocked unconscious.
     * DK II's total excludes workers and own creatures; its per-owner counters
     * include both (DKII.EXE 0x4882B6 and 0x4C7DF0).
     */
    public void creatureKilled(short killerId, short victimOwnerId, boolean worker) {
        creaturesKilledByOwner.merge(victimOwnerId, 1, Integer::sum);
        if (!worker && killerId != victimOwnerId) {
            enemyCreaturesKilled++;
        }
    }

    /** Victim player 0 selects the total, otherwise the selected owner's deaths. */
    public int getCreaturesKilled(short victimOwnerId) {
        return victimOwnerId == 0 ? enemyCreaturesKilled
                : creaturesKilledByOwner.getOrDefault(victimOwnerId, 0);
    }

    /** Records a creature's actual death, including workers and unattributed deaths. */
    public void creatureLost() {
        creaturesLost++;
    }

    public int getCreaturesLost() {
        return creaturesLost;
    }

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
