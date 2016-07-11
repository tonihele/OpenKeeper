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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base for player controls that have availabilities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <K> the type of object this control manages
 * @param <V> the value behind the type
 */
public abstract class AbstractPlayerControl<K extends Comparable<K>, V> {

    private final List<K> typesAvailable = new ArrayList<>();
    protected final Map<K, Set<V>> types = new LinkedHashMap<>();

    /**
     * Add a type to the availability pool of this player
     *
     * @param type the creature to add
     */
    public void setTypeAvailable(K type) {
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
    public List<K> getTypesAvailable() {
        return typesAvailable;
    }

    protected Set<V> get(K key) {
        return types.get(key);
    }

    protected Set<V> put(K key, Set<V> value) {
        return types.put(key, value);
    }

    /**
     * Get the amount of object types the player has
     *
     * @param key the type
     * @return amount of types
     */
    public int getTypeCount(K key) {
        Set<V> set = types.get(key);
        if (set != null) {
            return set.size();
        }
        return 0;
    }

    /**
     * Get the amount of all object types the player has
     *
     * @return amount of all types
     */
    public abstract int getTypeCount();

}
