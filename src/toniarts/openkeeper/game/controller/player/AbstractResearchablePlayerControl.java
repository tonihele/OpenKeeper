/*
 * Copyright (C) 2014-2019 OpenKeeper
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
import java.util.List;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;

/**
 * Abstract base for player controls that have availabilities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <K> the type of object this control manages
 * @param <V> the data type held in the user data
 * @param <TListener> the type of listener used for listening changes in the
 * control data
 */
public abstract class AbstractResearchablePlayerControl<K extends IIndexable & Comparable<K>, V extends ResearchableEntity, TListener>
        extends AbstractPlayerControl<K, V, V> {

    protected List<TListener> playerListeners;

    public AbstractResearchablePlayerControl(Keeper keeper, List<V> availabilitiesList, Collection<K> types) {
        super(keeper, availabilitiesList, types);
    }

    @Override
    protected short getDataTypeId(V type) {
        return type.getId();
    }

    @Override
    protected V getDataType(K type) {
        return get(type);
    }

    protected abstract V createDataType(K type);

    /**
     * Add a type to the availability pool of this player
     *
     * @param type the type to add
     * @param available set available or not
     * @param discovered set the status as discovered or needs research
     * @return true if availability status changed
     */
    public boolean setTypeAvailable(K type, boolean available, boolean discovered) {
        V researchableEntity;
        if (available) {
            if (!containsKey(type)) {
                researchableEntity = createDataType(type);
                put(type, researchableEntity);
            } else {
                researchableEntity = get(type);
            }
        } else {
            researchableEntity = remove(type);
        }

        boolean result = super.setTypeAvailable(type, available);

        // If the type was or is in the list, mark the discovered flag
        if (researchableEntity != null) {
            if (!result && researchableEntity.isDiscovered() != discovered) {
                result = true;
            }
            researchableEntity.setDiscovered(discovered);
        }

        // Listeners
        if (result && playerListeners != null) {
            for (TListener playerListener : playerListeners) {
                if (available) {
                    onAdded(playerListener, keeper, researchableEntity);
                } else {
                    onRemoved(playerListener, keeper, researchableEntity);
                }
            }
        }

        return result;
    }

    @Override
    public boolean setTypeAvailable(K type, boolean available) {
        return setTypeAvailable(type, available, available);
    }

    public void onResearchResultsAdded(K type) {
        V researchableEntity = get(type);
        if (researchableEntity != null) {
            if (!researchableEntity.isDiscovered()) {
                researchableEntity.setDiscovered(true);
            }
        }

        // Notify listeners
        if (playerListeners != null) {
            for (TListener playerListener : playerListeners) {
                onAdded(playerListener, keeper, researchableEntity);
            }
        }
    }

    public void onResearchResultsRemoved(K type) {
        V researchableEntity = get(type);
        if (researchableEntity != null) {
            if (researchableEntity.isDiscovered()) {
                researchableEntity.setDiscovered(false);
            }
        }

        // Notify listeners
        if (playerListeners != null) {
            for (TListener playerListener : playerListeners) {
                onRemoved(playerListener, keeper, researchableEntity);
            }
        }
    }

    /**
     * Listen to type status changes
     *
     * @param listener the listener
     */
    public void addListener(TListener listener) {
        if (playerListeners == null) {
            playerListeners = new ArrayList<>();
        }
        playerListeners.add(listener);
    }

    /**
     * No longer listen to type status changes
     *
     * @param listener the listener
     */
    public void removeListener(TListener listener) {
        if (playerListeners != null) {
            playerListeners.remove(listener);
        }
    }

    /**
     * Notify on entity added to the listener
     *
     * @param playerListener the listener
     * @param keeper the keeper
     * @param researchableEntity added entity
     */
    protected abstract void onAdded(TListener playerListener, Keeper keeper, V researchableEntity);

    /**
     * Notify on entity removed to the listener
     *
     * @param playerListener the listener
     * @param keeper the keeper
     * @param researchableEntity removed entity
     */
    protected abstract void onRemoved(TListener playerListener, Keeper keeper, V researchableEntity);

}
