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
import java.util.HashSet;
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

    private List<WorkerListener> workerListeners;
    private List<CreatureListener> creatureListeners;
    private final Map<Creature, Set<CreatureControl>> creatures = new LinkedHashMap<>();
    private final Set<CreatureControl> imps = new HashSet<>(); // Only imps are separated, not all workers

    public void init(List<CreatureControl> creatures) {
        for (CreatureControl creature : creatures) {
            onSpawn(creature);
        }
    }

    @Override
    public void onSpawn(CreatureControl creature) {

        // Add to the list
        Set<CreatureControl> creatureSet;
        boolean wasImp = false;
        if (isImp(creature)) {
            creatureSet = imps;
            wasImp = true;
        } else {
            creatureSet = creatures.get(creature.getCreature());
            if (creatureSet == null) {
                creatureSet = new LinkedHashSet<>();
                creatures.put(creature.getCreature(), creatureSet);
            }
        }
        creatureSet.add(creature);

        // Listeners
        if (wasImp) {
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
        Set<CreatureControl> creatureSet;
        boolean wasImp = false;
        if (isImp(creature)) {
            creatureSet = imps;
            wasImp = true;
        } else {
            creatureSet = creatures.get(creature.getCreature());
        }
        if (creatureSet != null) {
            creatureSet.remove(creature);
        }

        // Listeners
        if (wasImp) {
            updateWorkerListeners();
        } else {
            if (creatureListeners != null) {
                for (CreatureListener listener : creatureListeners) {
                    listener.onDie(creature);
                }
            }
        }
    }

    private boolean isImp(CreatureControl creature) {
        return creature.getCreature().getFlags().contains(Creature.CreatureFlag.IS_WORKER) && creature.getCreature().getFlags().contains(Creature.CreatureFlag.IS_EVIL);
    }

    public Map<Creature, Set<CreatureControl>> getCreatures() {
        return creatures;
    }

    public void zoomToCreature(Creature creature) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        for (CreatureControl creature : imps) {
            if (creature.getStateMachine().getCurrentState() == CreatureState.IDLE) {
                idle++;
            } else if (creature.getStateMachine().getCurrentState() == CreatureState.FIGHT) {
                fighting++;
            } else {
                busy++;
            }
        }
        workerListener.amountLabel.setText(String.format("%s", imps.size()));
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
