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

    public boolean hasSlapped() {
        return !slapsMap.isEmpty();
    }

    public boolean hasSlapped(Creature creature) {
        return slapsMap.containsKey(creature);
    }

    public void creaturedSlapped(Creature creature) {
        Integer numberOfSlaps = slapsMap.get(creature);
        if (numberOfSlaps == null) {
            numberOfSlaps = 0;
        }
        numberOfSlaps++;
        slapsMap.put(creature, numberOfSlaps);
    }

}
