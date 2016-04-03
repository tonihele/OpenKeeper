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

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.task.type.AbstractTask;
import toniarts.openkeeper.game.task.type.AbstractTileTask;
import toniarts.openkeeper.game.task.type.ClaimTileTask;
import toniarts.openkeeper.game.task.type.ClaimWallTileTask;
import toniarts.openkeeper.game.task.type.DigTileTask;
import toniarts.openkeeper.game.task.type.RepairWallTileTask;
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
    private final Map<Short, Set<AbstractTask>> taskQueues;
    private static final Logger logger = Logger.getLogger(TaskManager.class.getName());

    public TaskManager(WorldState worldState, short... playerIds) {
        this.worldState = worldState;

        // Create a queue for each managed player
        taskQueues = new HashMap<>(playerIds.length);
        for (short playerId : playerIds) {
            taskQueues.put(playerId, new HashSet<>());
        }

        // Scan the initial tasks
        scanInitialTasks();

        // We want to be notified on tile changes, we are event based, not constantly scanning type
        this.worldState.addListener(new TileChangeListener() {

            @Override
            public void onTileChange(final int x, final int y) {
                MapData mapData = worldState.getMapData();
                scanTerrainTasks(mapData, x, y, true, true);
            }
        });
    }

    private void scanInitialTasks() {
        MapData mapData = worldState.getMapData();
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                scanTerrainTasks(mapData, x, y, false, false);
            }
        }
    }

    private void scanTerrainTasks(final MapData mapData, final int x, final int y, final boolean checkNeighbours, final boolean deleteObsolete) {
        for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {

            // Scan existing tasks that are they valid, should be only one tile task per tile?
            if (deleteObsolete) {
                Iterator<AbstractTask> iter = entry.getValue().iterator();
                while (iter.hasNext()) {
                    AbstractTask task = iter.next();
                    if (task instanceof AbstractTileTask) {
                        AbstractTileTask tileTask = (AbstractTileTask) task;
                        if (!tileTask.isValid()) {
                            iter.remove();
                        }
                    }
                }
            }

            // Add a task
            TileData tile = mapData.getTile(x, y);
            // Dig
            if (tile.isSelectedByPlayerId(entry.getKey())) {
                AbstractTask task = new DigTileTask(worldState, x, y, entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim wall
            else if (worldState.isClaimableWall(x, y, entry.getKey())) {
                AbstractTask task = new ClaimWallTileTask(worldState, x, y, entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim
            else if (worldState.isClaimable(x, y, entry.getKey())) {
                AbstractTask task = new ClaimTileTask(worldState, x, y, entry.getKey());
                addTask(entry.getKey(), task);
            } // Repair wall
            else if (worldState.isRepairableWall(x, y, entry.getKey())) {
                AbstractTask task = new RepairWallTileTask(worldState, x, y, entry.getKey());
                addTask(entry.getKey(), task);
            }
        }

        // See the neighbours
        if (checkNeighbours) {
            for (Point p : worldState.getMapLoader().getSurroundingTiles(new Point(x, y), false)) {
                scanTerrainTasks(mapData, p.x, p.y, false, false);
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

        Set<AbstractTask> taskQueue = taskQueues.get(creature.getOwnerId());
        if (taskQueue == null) {
            return false;
//            throw new IllegalArgumentException("This task manager instance is not for the given player!");
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
                } else if (task.getAssigneeCount() == 0) {

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

    private void addTask(short playerId, AbstractTask task) {
        Set<AbstractTask> tasks = taskQueues.get(playerId);
        if (!tasks.contains(task)) {
            tasks.add(task);
            logger.log(Level.INFO, "Added task {0} for player {1}!", new Object[]{task, playerId});
        } else {
            logger.log(Level.WARNING, "Already a task {0} for player {1}!", new Object[]{task, playerId});
        }
    }

}
