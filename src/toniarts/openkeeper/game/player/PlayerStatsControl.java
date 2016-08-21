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
package toniarts.openkeeper.game.player;

import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * A class to hold player's miscellaneous statistics
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerStatsControl {

    private final Map<Creature, Integer> slapsMap = new HashMap<>();
    private final Map<Creature, Integer> pickUpsMap = new HashMap<>();
    private final Map<Creature, Integer> dropsMap = new HashMap<>();

    public boolean hasSlapped() {
        return !slapsMap.isEmpty();
    }

    public boolean hasSlapped(Creature creature) {
        return slapsMap.containsKey(creature);
    }

    public void creatureSlapped(Creature creature) {
        Integer numberOfSlaps = slapsMap.get(creature);
        if (numberOfSlaps == null) {
            numberOfSlaps = 0;
        }
        numberOfSlaps++;
        slapsMap.put(creature, numberOfSlaps);
    }

    public boolean hasPickedUp() {
        return !pickUpsMap.isEmpty();
    }

    public boolean hasPickedUp(Creature creature) {
        return pickUpsMap.containsKey(creature);
    }

    public void creaturePickedUp(Creature creature) {
        Integer numberOfPickUps = pickUpsMap.get(creature);
        if (numberOfPickUps == null) {
            numberOfPickUps = 0;
        }
        numberOfPickUps++;
        pickUpsMap.put(creature, numberOfPickUps);
    }

    public boolean hasDropped() {
        return !dropsMap.isEmpty();
    }

    public boolean hasDropped(Creature creature) {
        return dropsMap.containsKey(creature);
    }

    public void creatureDropped(Creature creature) {
        Integer numberOfDrops = dropsMap.get(creature);
        if (numberOfDrops == null) {
            numberOfDrops = 0;
        }
        numberOfDrops++;
        dropsMap.put(creature, numberOfDrops);
    }

}
