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
package toniarts.openkeeper.game.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.logging.Logger;
import toniarts.openkeeper.game.task.type.AbstractTask;
import toniarts.openkeeper.game.task.type.AbstractTileTask;
import toniarts.openkeeper.game.task.type.DigTileTask;
import toniarts.openkeeper.world.MapData;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.listener.TileChangeListener;

/**
 * Task manager for several players. Can assign creatures to different tasks.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TaskManager {

    private final WorldState worldState;
    private final Map<Short, PriorityQueue<AbstractTask>> taskQueues;
    private static final Logger logger = Logger.getLogger(TaskManager.class.getName());

    public TaskManager(WorldState worldState, short... playerIds) {
        this.worldState = worldState;

        // Create a queue for each managed player
        taskQueues = new HashMap<>(playerIds.length);
        for (short playerId : playerIds) {
            taskQueues.put(playerId, new PriorityQueue<>());
        }

        // Scan the initial tasks
        scanInitialTasks();

        // We want to be notified on tile changes, we are event based, not constantly scanning type
        this.worldState.addListener(new TileChangeListener() {

            @Override
            public void onTileChange(final int x, final int y) {
                MapData mapData = worldState.getMapData();
                scanTerrainTasks(mapData, x, y);
            }
        });
    }

    private void scanInitialTasks() {
        MapData mapData = worldState.getMapData();
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                scanTerrainTasks(mapData, x, y);
            }
        }
    }

    private void scanTerrainTasks(final MapData mapData, final int x, final int y) {
        for (Entry<Short, PriorityQueue<AbstractTask>> entry : taskQueues.entrySet()) {

            // Scan existing tasks that are they valid, should be only one tile task per tile?
            Iterator<AbstractTask> iter = entry.getValue().iterator();
            while (iter.hasNext()) {
                AbstractTask task = iter.next();
                if (task instanceof AbstractTileTask) {
                    AbstractTileTask tileTask = (AbstractTileTask) task;
                    if (tileTask.getTaskLocation().x == x && tileTask.getTaskLocation().y == y && !tileTask.isValid()) {
                        iter.remove();
                        return; // Only one one tile task?
                    }
                }
            }

            // Add a task
            TileData tile = mapData.getTile(x, y);
            if (tile.isSelectedByPlayerId(entry.getKey())) {
                entry.getValue().add(new DigTileTask(worldState, x, y, entry.getKey()));
            }
        }
    }

    /**
     * Assign a task to a creature
     *
     * @param creature the creature to assign a task to
     * @param byDistance whether we should assign the closest task (i.e. if a
     * player drops the creature somewhere)
     * @return true if a task was assigned
     */
    public boolean assignTask(CreatureControl creature, boolean byDistance) {

        PriorityQueue<AbstractTask> taskQueue = taskQueues.get(creature.getOwnerId());
        if (taskQueue == null) {
            throw new IllegalArgumentException("This task manager instance is not for the given player!");
        }

        // TODO: distance
        Iterator<AbstractTask> iter = taskQueue.iterator();
        AbstractTask crowdedTask = null;
        while (iter.hasNext()) {
            AbstractTask task = iter.next();
            if (task.canAssign(creature)) {

                // If we can assign, that is fine, but prioritize on non-assigned tasks
                if ((crowdedTask == null && task.getAssigneeCount() > 0) || (crowdedTask != null && task.getAssigneeCount() < crowdedTask.getAssigneeCount())) {
                    crowdedTask = task;
                } else {

                    // Assign to first non-empty task
                    task.assign(creature);
                    return true;
                }
            }
        }

        // See if we have a crowded task for this
        if (crowdedTask != null) {
            crowdedTask.assign(creature);
            return true;
        }

        return false;
    }

}
