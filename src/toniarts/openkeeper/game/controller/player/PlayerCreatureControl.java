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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.gui.nifty.WorkerAmountControl;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.listener.CreatureListener;

/**
 * Holds a list of player creatures and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCreatureControl extends AbstractPlayerControl<Creature, Set<CreatureControl>> implements CreatureListener {

    public enum CreatureUIState {

        IDLE, BUSY, FIGHT, WORK, DEFENCE, HAPPY, UNHAPPY, ANGRY
    };

    private List<WorkerAmountControl> workerListeners;
    private List<CreatureListener> creatureListeners;
    private Creature imp;
    private int creatureCount = 0;
    private int impTotal = 0;
    private int impIdle = 0;
    private int impFighting = 0;
    private int impBusy = 0;
    private final Map<Creature, Integer> selectionIndices = new HashMap<>();

    public PlayerCreatureControl(Keeper keeper) {
        super(keeper);
    }

    public void init(List<CreatureControl> creatures, Creature imp) {
        this.imp = imp;
        if (creatures != null) {
            for (CreatureControl creature : creatures) {
                onSpawn(creature);
            }
        }
    }

    @Override
    public void onSpawn(CreatureControl creature) {

        // Add to the list
        Set<CreatureControl> creatureSet = get(creature.getCreature());
        if (creatureSet == null) {
            creatureSet = new LinkedHashSet<>();
            put(creature.getCreature(), creatureSet);
        }
        creatureSet.add(creature);

        // Listeners
        if (isImp(creature)) {
            updateWorkerListeners();
        } else {
            creatureCount++;
            if (creatureListeners != null) {
                for (CreatureListener listener : creatureListeners) {
                    listener.onSpawn(creature);
                }
            }
        }
    }

    @Override
    public void onStateChange(CreatureControl creature, CreatureState newState, CreatureState oldState) {
        if (isImp(creature)) {
            updateWorkerListeners();
        } else if (creatureListeners != null) {
            for (CreatureListener listener : creatureListeners) {
                listener.onStateChange(creature, newState, oldState);
            }
        }
    }

    @Override
    public void onDie(CreatureControl creature) {

        // Delete
        Set<CreatureControl> creatureSet = get(creature.getCreature());
        if (creatureSet != null) {
            creatureSet.remove(creature);
        }

        // Listeners
        if (isImp(creature)) {
            updateWorkerListeners();
        } else {
            creatureCount--;
            if (creatureListeners != null) {
                for (CreatureListener listener : creatureListeners) {
                    listener.onDie(creature);
                }
            }
        }
    }

    /**
     * Get all creatures
     *
     * @return the creatures
     */
    public Map<Creature, Set<CreatureControl>> getAllCreatures() {
        Map<Creature, Set<CreatureControl>> map = new LinkedHashMap<>(types);
        return map;
    }

    /**
     * Get creatures, minus the imps
     *
     * @return the creatures
     */
    public Map<Creature, Set<CreatureControl>> getCreatures() {
        Map<Creature, Set<CreatureControl>> map = getAllCreatures();
        map.remove(imp);
        return map;
    }

    /**
     * Listen to imp updates
     *
     * @param control control of workers
     */
    public void addWorkerListener(WorkerAmountControl control) {
        if (workerListeners == null) {
            workerListeners = new ArrayList<>();
        }
        workerListeners.add(control);
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

    private void updateWorkerListeners() {
        if (workerListeners != null) {

            // Calculate
            impTotal = 0;
            impIdle = 0;
            impFighting = 0;
            impBusy = 0;

            Set<CreatureControl> imps = get(imp);
            for (CreatureControl creature : imps) {
                if (creature.getStateMachine().getCurrentState() == CreatureState.PICKED_UP
                        || creature.getStateMachine().getCurrentState() == CreatureState.DEAD) {
                    continue;
                }
                if (isCreatureState(creature, CreatureUIState.IDLE)) {
                    impIdle++;
                } else if (isCreatureState(creature, CreatureUIState.FIGHT)) {
                    impFighting++;
                } else {
                    impBusy++;
                }
                impTotal++;
            }

            // Update
            for (WorkerAmountControl control : workerListeners) {
                control.setValues(impTotal, impIdle, impBusy, impFighting);
            }
        }
    }

    /**
     * Get the next creature. And advances the current selection
     *
     * @param creature the type of creature
     * @return the next creature of the type
     */
    public CreatureControl getNextCreature(Creature creature) {
        return getNextCreature(creature, null);
    }

    /**
     * Get the next creature of the selected type. And advances the current
     * selection
     *
     * @param creature the type of creature
     * @param state the state filter
     * @return the next creature of the type
     */
    public CreatureControl getNextCreature(Creature creature, CreatureUIState state) {
        List<CreatureControl> creatureList = new ArrayList<>(get(creature));
        Integer index = selectionIndices.get(creature);
        if (index == null || index >= creatureList.size()) {
            index = 0;
        }

        // Loop until filter hits
        int startIndex = index;
        CreatureControl selectedCreature;
        do {
            selectedCreature = creatureList.get(index);
            index++;
            selectionIndices.put(creature, index);
        } while (index != startIndex && !isCreatureState(selectedCreature, state));
        return selectedCreature;
    }

    public CreatureControl getCreature(Creature creature) {
        return getCreature(creature, null);
    }

    @Nullable
    public CreatureControl getCreature(Creature creature, CreatureUIState state) {
        List<CreatureControl> creatureList = new ArrayList<>(get(creature));

        for (CreatureControl creatureControl : creatureList) {
            if (creatureControl.getStateMachine().getCurrentState() == CreatureState.PICKED_UP
                    || creatureControl.getStateMachine().getCurrentState() == CreatureState.DEAD) {
                continue;
            }
            if (isCreatureState(creatureControl, state)) {
                return creatureControl;
            }
        }

        return null;
    }

    private boolean isCreatureState(CreatureControl creature, CreatureUIState state) {
        if (state == null) {
            return true;
        }
        if (creature.getStateMachine().getCurrentState() == CreatureState.IDLE) {
            return state == CreatureUIState.IDLE;
        } else if (creature.getStateMachine().getCurrentState() == CreatureState.FIGHT) {
            return state == CreatureUIState.FIGHT;
        } else {
            return state == CreatureUIState.BUSY;
        }
    }

    private boolean isImp(CreatureControl creature) {
        return creature.getCreature().equals(imp);
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
        Set<CreatureControl> imps = get(imp);
        return (imps != null ? imps.size() : 0);
    }

}
