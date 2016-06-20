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

import de.lessvoid.nifty.controls.Label;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.listener.CreatureListener;

/**
 * Holds a list of player creatures and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCreatureControl implements CreatureListener {

    public enum CreatureUIState {

        IDLE, BUSY, FIGHTING
    };

    private List<WorkerListener> workerListeners;
    private List<CreatureListener> creatureListeners;
    private Creature imp;
    private final Map<Creature, Set<CreatureControl>> creatures = new LinkedHashMap<>();
    private final Map<Creature, Integer> selectionIndices = new HashMap<>();

    public void init(List<CreatureControl> creatures, Creature imp) {
        this.imp = imp;
        for (CreatureControl creature : creatures) {
            onSpawn(creature);
        }
    }

    @Override
    public void onSpawn(CreatureControl creature) {

        // Add to the list
        Set<CreatureControl> creatureSet = creatures.get(creature.getCreature());
        if (creatureSet == null) {
            creatureSet = new LinkedHashSet<>();
            creatures.put(creature.getCreature(), creatureSet);
        }
        creatureSet.add(creature);

        // Listeners
        if (isImp(creature)) {
            updateWorkerListeners();
        } else {
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
        } else {
            if (creatureListeners != null) {
                for (CreatureListener listener : creatureListeners) {
                    listener.onStateChange(creature, newState, oldState);
                }
            }
        }
    }

    @Override
    public void onDie(CreatureControl creature) {

        // Delete
        Set<CreatureControl> creatureSet = creatures.get(creature.getCreature());
        if (creatureSet != null) {
            creatureSet.remove(creature);
        }

        // Listeners
        if (isImp(creature)) {
            updateWorkerListeners();
        } else {
            if (creatureListeners != null) {
                for (CreatureListener listener : creatureListeners) {
                    listener.onDie(creature);
                }
            }
        }
    }

    /**
     * Get creatures, minus the imps
     *
     * @return the creatures
     */
    public Map<Creature, Set<CreatureControl>> getCreatures() {
        Map<Creature, Set<CreatureControl>> map = new LinkedHashMap<>(creatures);
        map.remove(imp);
        return map;
    }

    /**
     * Listen to imp updates
     *
     * @param amountLabel the total amount of imps
     * @param idleLabel the amount of imps idling
     * @param busyLabel the amount of imps busy
     * @param fightingLabel the amount of imps fighting
     */
    public void addWorkerListener(Label amountLabel, Label idleLabel, Label busyLabel, Label fightingLabel) {
        if (workerListeners == null) {
            workerListeners = new ArrayList<>();
        }
        WorkerListener workerListener = new WorkerListener(amountLabel, idleLabel, busyLabel, fightingLabel);
        updateWorkerListener(workerListener);
        workerListeners.add(workerListener);
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

    private void updateWorkerListener(WorkerListener workerListener) {
        int idle = 0;
        int fighting = 0;
        int busy = 0;
        Set<CreatureControl> imps = creatures.get(imp);
        if (imps != null) {
            for (CreatureControl creature : imps) {
                if (isCreatureState(creature, CreatureUIState.IDLE)) {
                    idle++;
                } else if (isCreatureState(creature, CreatureUIState.FIGHTING)) {
                    fighting++;
                } else {
                    busy++;
                }
            }
        }
        workerListener.amountLabel.setText(String.format("%s", imps != null ? imps.size() : 0));
        workerListener.busyLabel.setText(String.format("%s", busy));
        workerListener.fightingLabel.setText(String.format("%s", fighting));
        workerListener.idleLabel.setText(String.format("%s", idle));
    }

    private void updateWorkerListeners() {
        if (workerListeners != null) {
            for (WorkerListener workerListener : workerListeners) {
                updateWorkerListener(workerListener);

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
        List<CreatureControl> creatureList = new ArrayList<>(creatures.get(creature));
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

    private boolean isCreatureState(CreatureControl creature, CreatureUIState state) {
        if (state == null) {
            return true;
        }
        if (creature.getStateMachine().getCurrentState() == CreatureState.IDLE) {
            return state == CreatureUIState.IDLE;
        } else if (creature.getStateMachine().getCurrentState() == CreatureState.FIGHT) {
            return state == CreatureUIState.FIGHTING;
        } else {
            return state == CreatureUIState.BUSY;
        }
    }

    private boolean isImp(CreatureControl creature) {
        return creature.getCreature().equals(imp);
    }

    public CreatureControl getNextImp() {
        return getNextImp(null);
    }

    public CreatureControl getNextImp(CreatureUIState state) {
        return getNextCreature(imp, state);
    }

    private static class WorkerListener {

        private final Label amountLabel;
        private final Label idleLabel;
        private final Label busyLabel;
        private final Label fightingLabel;

        public WorkerListener(Label amountLabel, Label idleLabel, Label busyLabel, Label fightingLabel) {
            this.amountLabel = amountLabel;
            this.idleLabel = idleLabel;
            this.busyLabel = busyLabel;
            this.fightingLabel = fightingLabel;
        }
    }

}
