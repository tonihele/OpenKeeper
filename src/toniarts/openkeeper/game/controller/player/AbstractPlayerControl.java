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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.game.data.Keeper;

/**
 * Abstract base for player controls that have availabilities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <K> the type of object this control manages
 * @param <V> the value behind the type
 */
public abstract class AbstractPlayerControl<K extends Comparable<K> & IIndexable, V> {

    private final List<K> typesAvailable = new ArrayList<>();
    private final List<Short> availabilitiesList;
    protected final Map<K, V> types = new LinkedHashMap<>();
    protected final Keeper keeper;

    public AbstractPlayerControl(Keeper keeper, List<Short> availabilitiesList) {
        this.keeper = keeper;
        this.availabilitiesList = availabilitiesList;
    }

    /**
     * Add a type to the availability pool of this player
     *
     * @param type the type to add
     * @param available set available or not
     */
    public void setTypeAvailable(K type, boolean available) {
        int index = Collections.binarySearch(typesAvailable, type);
        if (index < 0 && available) {
            typesAvailable.add(~index, type);
            availabilitiesList.add(~index, type.getId());
        } else if (index >= 0 && !available) {
            typesAvailable.remove(index);
            availabilitiesList.remove(index);
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

    /**
     * Get all the objects by the types
     *
     * @return the objects
     */
    public Map<K, V> getTypes() {
        return types;
    }

    protected V get(K key) {
        return types.get(key);
    }

    protected V put(K key, V value) {
        return types.put(key, value);
    }

    /**
     * Get the amount of object types the player has
     *
     * @param key the type
     * @return amount of types
     */
    public int getTypeCount(K key) {
        V set = types.get(key);
        if (set != null) {
            if (set instanceof Collection) {
                return ((Collection) set).size();
            } else {
                return 1;
            }
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
