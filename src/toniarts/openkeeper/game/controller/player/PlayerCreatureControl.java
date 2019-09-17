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

import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.world.listener.CreatureListener;

/**
 * Holds a list of player creatures and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCreatureControl extends AbstractPlayerControl<Creature, Set<EntityId>, Short> {

    private List<CreatureListener> creatureListeners;
    private final Creature imp;
    private int creatureCount = 0;

    public PlayerCreatureControl(Keeper keeper, Creature imp, Collection<Creature> creatures) {
        super(keeper, keeper.getAvailableCreatures(), creatures);
        this.imp = imp;
    }

    @Override
    protected short getDataTypeId(Short type) {
        return type;
    }

    @Override
    protected Short getDataType(Creature type) {
        return type.getCreatureId();
    }

    public void onCreatureAdded(EntityId entityId, Creature creature) {

        // Add to the list
        Set<EntityId> creatureSet = get(creature);
        if (creatureSet == null) {
            creatureSet = new LinkedHashSet<>();
            put(creature, creatureSet);
        }
        creatureSet.add(entityId);

        // Listeners
        if (!isImp(creature)) {
            creatureCount++;
            if (creatureListeners != null) {
                for (CreatureListener listener : creatureListeners) {
                    // listener.onSpawn(creature);
                }
            }
        }
    }

    public void onCreatureRemoved(EntityId entityId, Creature creature) {

        // Delete
        Set<EntityId> creatureSet = get(creature);
        if (creatureSet != null) {
            creatureSet.remove(entityId);
        }

        // Listeners
        if (!isImp(creature)) {
            creatureCount--;
            if (creatureListeners != null) {
                for (CreatureListener listener : creatureListeners) {
                    //listener.onDie(creature);
                }
            }
        }
    }

    /**
     * Get all creatures
     *
     * @return the creatures
     */
    public Map<Creature, Set<EntityId>> getAllCreatures() {
        Map<Creature, Set<EntityId>> map = new LinkedHashMap<>(types);
        return map;
    }

    /**
     * Get creatures, minus the imps
     *
     * @return the creatures
     */
    public Map<Creature, Set<EntityId>> getCreatures() {
        Map<Creature, Set<EntityId>> map = getAllCreatures();
        map.remove(imp);
        return map;
    }

    /**
     * Listen to creature updates. Excluding imps
     *
     * @param listener the listener
     */
    public void addCreatureListener(CreatureListener listener) {
        if (creatureListeners == null) {
            creatureListeners = new ArrayList<>();
        }
        creatureListeners.add(listener);
    }

    private boolean isImp(Creature creature) {
        return creature.equals(imp);
    }

    public Creature getImp() {
        return imp;
    }

    /**
     * Get player creature count. Excluding imps.
     *
     * @return the creature count
     */
    @Override
    public int getTypeCount() {
        return creatureCount;
    }

    /**
     * Get the imp count
     *
     * @return the number of imps
     */
    public int getImpCount() {
        if (imp == null) {
            return 0;
        }
        Set<EntityId> imps = get(imp);
        return (imps != null ? imps.size() : 0);
    }

}
