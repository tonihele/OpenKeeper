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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base for player controls that have availabilities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> the type of object this control manages
 */
public class AbstractPlayerControl<T extends Comparable<T>> {

    private final List<T> typesAvailable = new ArrayList<>();

    /**
     * Add a type to the availability pool of this player
     *
     * @param type the creature to add
     */
    public void setTypeAvailable(T type) {
        int index = Collections.binarySearch(typesAvailable, type);
        if (index < 0) {
            typesAvailable.add(~index, type);
        }
    }

    /**
     * Get the types available for this player
     *
     * @return the types available
     */
    public List<T> getTypesAvailable() {
        return typesAvailable;
    }

}
