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

import com.badlogic.gdx.ai.pfa.GraphPath;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ai.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.task.creature.ClaimLair;
import toniarts.openkeeper.game.task.creature.GoToSleep;
import toniarts.openkeeper.game.task.creature.ResearchSpells;
import toniarts.openkeeper.game.task.objective.AbstractObjectiveTask;
import toniarts.openkeeper.game.task.worker.CarryEnemyCreatureToPrison;
import toniarts.openkeeper.game.task.worker.CarryGoldToTreasuryTask;
import toniarts.openkeeper.game.task.worker.ClaimRoomTask;
import toniarts.openkeeper.game.task.worker.ClaimTileTask;
import toniarts.openkeeper.game.task.worker.ClaimWallTileTask;
import toniarts.openkeeper.game.task.worker.DigTileTask;
import toniarts.openkeeper.game.task.worker.FetchObjectTask;
import toniarts.openkeeper.game.task.worker.RepairWallTileTask;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.object.ObjectControl;

/**
 * Task manager for several players. Can assign creatures to different tasks.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TaskManager implements ITaskManager {

    private final IMapController mapController;
    private final IGameWorldController gameWorldController;
    private final Map<Short, Set<AbstractTask>> taskQueues;
    private final Map<Short, IPlayerController> playerControllers;
    private final Map<IRoomController, Map<Point, AbstractCapacityCriticalRoomTask>> roomTasks = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(TaskManager.class.getName());

    public TaskManager(IGameWorldController gameWorldController, IMapController mapController, Collection<IPlayerController> players) {
        this.mapController = mapController;
        this.gameWorldController = gameWorldController;

        // Set the players
        // Create a queue for each managed player (everybody except Good & Neutral)
        taskQueues = new HashMap<>(players.size());
        playerControllers = new HashMap<>();
        for (IPlayerController playerController : players) {
            Keeper keeper = playerController.getKeeper();

            playerControllers.put(keeper.getId(), playerController);

            if (keeper.getId() != Player.GOOD_PLAYER_ID && keeper.getId() != Player.NEUTRAL_PLAYER_ID) {
                taskQueues.put(keeper.getId(), new HashSet<>());
            }
        }

        // Scan the initial tasks
        scanInitialTasks();

        // Add task listeners
        addListeners(players);
    }

    private void addListeners(Collection<IPlayerController> players) {

        // We want to be notified on tile changes, we are event based, not constantly scanning type
        this.mapController.addListener(new MapListener() {

            @Override
            public void onTilesChange(List<MapTile> updatedTiles) {
                for (MapTile tile : updatedTiles) {
                    scanTerrainTasks(tile, true, true);
                }
            }
        });

        // Get notified by object tasks
        // TODO: pick uppable objects entityset listener
//        this.worldState.getThingLoader().addListener(new ObjectListener() {
//
//            @Override
//            public void onAdded(ObjectControl objectControl) {
//                for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {
//                    entry.getValue().add(getObjectTask(objectControl, entry.getKey()));
//                }
//            }
//
//            @Override
//            public void onRemoved(ObjectControl objectControl) {
//                for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {
//                    entry.getValue().remove(getObjectTask(objectControl, entry.getKey()));
//                }
//            }
//        });
        // Get notified by fallen comrades and enemies
        // TODO: Entityset listener
//        for (Keeper keeper : players) {
//            this.worldState.getThingLoader().addListener(keeper.getId(), new CreatureListener() {
//                @Override
//                public void onSpawn(ICreatureController creature) {
//
//                }
//
//                @Override
//                public void onStateChange(ICreatureController creature, CreatureState newState, CreatureState oldState) {
//                    if (newState == CreatureState.UNCONSCIOUS) {
//
//                        // Add rescue mission for the own troops and capture for the enemy
//                        for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {
//                            if (entry.getKey() == creature.getOwnerId()) {
//
//                                // Rescue
//                                entry.getValue().add(new RescueCreatureTask(worldState, creature, entry.getKey()));
//                            } else {
//
//                                // Capture
//                                entry.getValue().add(new CaptureEnemyCreatureTask(worldState, creature, entry.getKey()));
//                            }
//                        }
//                    }
//                }
//
//                @Override
//                public void onDie(ICreatureController creature) {
//                    // TODO: Rob the corpses
//                }
//
//            });
//        }
    }

    private void scanInitialTasks() {
        MapData mapData = mapController.getMapData();
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                scanTerrainTasks(mapController.getMapData().getTile(x, y), false, false);
            }
        }

        // Object tasks
//        for (ObjectControl objectControl : worldState.getThingLoader().getObjects()) {
//            for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {
//                entry.getValue().add(getObjectTask(objectControl, entry.getKey()));
//            }
//        }
    }

    private void scanTerrainTasks(final MapTile tile, final boolean checkNeighbours, final boolean deleteObsolete) {
        for (Entry<Short, Set<AbstractTask>> entry : taskQueues.entrySet()) {

            // Scan existing tasks that are they valid, should be only one tile task per tile?
            if (deleteObsolete) {
                Iterator<AbstractTask> iter = entry.getValue().iterator();
                while (iter.hasNext()) {
                    AbstractTask task = iter.next();
                    if (task instanceof AbstractTileTask) {
                        AbstractTileTask tileTask = (AbstractTileTask) task;
                        if (tileTask.isRemovable()) {
                            iter.remove();
                        }
                    }
                }
            }

            // Dig
            if (mapController.isSelected(tile.getX(), tile.getY(), entry.getKey())) {
                AbstractTask task = new DigTileTask(gameWorldController, mapController, tile.getX(), tile.getY(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim wall
            else if (mapController.isClaimableWall(tile.getX(), tile.getY(), entry.getKey())) {
                AbstractTask task = new ClaimWallTileTask(gameWorldController, mapController, tile.getX(), tile.getY(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim
            else if (mapController.isClaimableTile(tile.getX(), tile.getY(), entry.getKey())) {
                AbstractTask task = new ClaimTileTask(gameWorldController, mapController, tile.getX(), tile.getY(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Repair wall
            else if (mapController.isRepairableWall(tile.getX(), tile.getY(), entry.getKey())) {
                AbstractTask task = new RepairWallTileTask(gameWorldController, mapController, tile.getX(), tile.getY(), entry.getKey());
                addTask(entry.getKey(), task);
            } // Claim room
            else if (mapController.isClaimableRoom(tile.getX(), tile.getY(), entry.getKey())) {
                AbstractTask task = new ClaimRoomTask(gameWorldController, mapController, tile.getX(), tile.getY(), entry.getKey());
                addTask(entry.getKey(), task);
            }
        }

        // See the neighbours
        if (checkNeighbours) {
            for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), tile.getLocation(), false)) {
                scanTerrainTasks(mapController.getMapData().getTile(p), false, false);
            }
        }
    }

    @Override
    public boolean assignTask(ICreatureController creature, boolean byDistance) {

        Set<AbstractTask> taskQueue = taskQueues.get(creature.getOwnerId());
        if (taskQueue == null) {
            return false;
//            throw new IllegalArgumentException("This task manager instance is not for the given player!");
        }

        // Sort by distance & priority
        final Point currentLocation = creature.getCreatureCoordinates();
        List<AbstractTask> prioritisedTaskQueue = new ArrayList<>(taskQueue);
        Collections.sort(prioritisedTaskQueue, new Comparator<AbstractTask>() {

            @Override
            public int compare(AbstractTask t, AbstractTask t1) {
                int result = Integer.compare(calculateDistance(currentLocation, t.getTaskLocation()) + t.getPriority(), calculateDistance(currentLocation, t1.getTaskLocation()) + t1.getPriority());
                if (result == 0) {

                    // If the same, compare by date added
                    return t.getTaskCreated().compareTo(t1.getTaskCreated());
                }
                return result;
            }

        });

        // Take the first available task from the sorted queue
        for (AbstractTask task : prioritisedTaskQueue) {
            if (task.canAssign(creature)) {

                // Assign to first task
                task.assign(creature, true);
                return true;
            }
        }

        return false;
    }

    private void addTask(short playerId, AbstractTask task) {
        Set<AbstractTask> tasks = taskQueues.get(playerId);
        if (!tasks.contains(task)) {
            tasks.add(task);
            LOGGER.log(Level.INFO, "Added task {0} for player {1}!", new Object[]{task, playerId});
        } else {
            LOGGER.log(Level.WARNING, "Already a task {0} for player {1}!", new Object[]{task, playerId});
        }
    }

    @Override
    public boolean assignGoldToTreasuryTask(ICreatureController creature) {

        // See if the creature's player lacks of gold
        IPlayerController player = playerControllers.get(creature.getOwnerId());
        if (!player.getGoldControl().isFullCapacity()) {
            return assignClosestRoomTask(creature, ObjectType.GOLD);
        }
        return false;
    }

    @Override
    public boolean assignClosestRoomTask(ICreatureController creature, ObjectType objectType) {
        return assignClosestRoomTask(creature, objectType, true);
    }

    /**
     * Assigns closest room task to a given creature of requested type or check
     * for validity
     *
     * @param creature the creature to assign to
     * @param objectType the type of room service
     * @param assign whether to actually assign the creature to the task or just
     * test
     * @return true if the task was assigned
     */
    private boolean assignClosestRoomTask(ICreatureController creature, ObjectType objectType, boolean assign) {
        Point currentPosition = creature.getCreatureCoordinates();

        // Get all the rooms of the given type
        List<IRoomController> rooms = mapController.getRoomsByFunction(objectType, creature.getOwnerId());
        Map<Integer, IRoomController> distancesToRooms = new TreeMap<>();
        for (IRoomController room : rooms) {
            if (!room.isFullCapacity()) {
                distancesToRooms.put(getShortestDistance(currentPosition, room.getRoomInstance().getCoordinates().toArray(new Point[room.getRoomInstance().getCoordinates().size()])), room
                );
            }
        }

        // See that are they really accessible starting from the least distance one
        for (IRoomController room : distancesToRooms.values()) {

            // FIXME: if we are to have more capacity than one per tile, we need to refactor
            // The whole rooms are always accessible, take a random point from the room like DK II seems to do
            List<Point> coordinates = new ArrayList<>(room.getObjectControl(objectType).getAvailableCoordinates());
            Iterator<Point> iter = coordinates.iterator();
            Map<Point, AbstractCapacityCriticalRoomTask> taskPoints = roomTasks.get(room);
            while (iter.hasNext()) {
                Point p = iter.next();
                if (!room.isTileAccessible(null, p) || (taskPoints != null && taskPoints.containsKey(p))) {
                    iter.remove();
                }
            }

            // Assign
            if (!coordinates.isEmpty()) {
                Point target = Utils.getRandomItem(coordinates);
                GraphPath<MapTile> path = gameWorldController.findPath(creature.getCreatureCoordinates(), target, creature);
                if (path != null || target == creature.getCreatureCoordinates()) {

                    // Assign the task
                    AbstractTask task = getRoomTask(objectType, target, creature, room);

                    // See if really assign
                    if (!assign) {
                        return task.isValid(creature);
                    }

                    if (task instanceof AbstractCapacityCriticalRoomTask) {
                        if (taskPoints == null) {
                            taskPoints = new HashMap<>();
                        }
                        taskPoints.put(target, (AbstractCapacityCriticalRoomTask) task);
                        roomTasks.put(room, taskPoints);
                    }
                    task.assign(creature, true);
                    return true;
                }
            }
        }

        return false;
    }

    private static Integer getShortestDistance(Point currentPosition, Point... coordinates) {
        int distance = Integer.MAX_VALUE;
        for (Point p : coordinates) {
            // TODO: do we need to do this diagonally?
            distance = Math.min(distance, calculateDistance(currentPosition, p));
            if (distance == 0) {
                break;
            }
        }
        return distance;
    }

    private static int calculateDistance(Point currentPosition, Point p) {
        return Math.abs(currentPosition.x - p.x) + Math.abs(currentPosition.y - p.y);
    }

    private AbstractTask getRoomTask(ObjectType objectType, Point target, ICreatureController creature, IRoomController room) {
        switch (objectType) {
            case GOLD: {
                return new CarryGoldToTreasuryTask(gameWorldController, mapController, target.x, target.y, creature.getOwnerId(), room);
            }
            case LAIR: {
                return new ClaimLair(gameWorldController, mapController, target.x, target.y, creature.getOwnerId(), room, this);
            }
            case RESEARCHER: {
                return new ResearchSpells(gameWorldController, mapController, target.x, target.y, creature.getOwnerId(), room, this);
            }
            case PRISONER: {
                return new CarryEnemyCreatureToPrison(gameWorldController, mapController, target.x, target.y, creature.getOwnerId(), room, this);
            }
        }
        return null;
    }

    protected void removeRoomTask(AbstractCapacityCriticalRoomTask task) {
        Map<Point, AbstractCapacityCriticalRoomTask> taskPoints = roomTasks.get(task.getRoom());
        taskPoints.remove(task.getTaskLocation());
        if (taskPoints.isEmpty()) {
            roomTasks.remove(task.getRoom());
        }
    }

    @Override
    public boolean assignObjectiveTask(ICreatureController creature, Thing.HeroParty.Objective objective) {
        AbstractObjectiveTask task = null;
        switch (objective) {
            case SEND_TO_ACTION_POINT: {
                //task = new SendToActionPoint(gameWorldController, mapController, creature.getObjectiveTargetActionPoint(), creature.getOwnerId());
                break;
            }
            case KILL_PLAYER: {
                //task = new KillPlayer(gameWorldController, mapController, creature.getObjectiveTargetPlayerId(), creature);
                break;
            }
        }

        // Assign
        if (task != null) {
            task.getTask().assign(creature, true);
            return true;
        }
        return false;
    }

    private AbstractTask getObjectTask(ObjectControl objectControl, short playerId) {
        return new FetchObjectTask(gameWorldController, mapController, objectControl, playerId);
    }

    @Override
    public boolean isTaskAvailable(ICreatureController creature, Creature.JobType jobType) {
        return assignTask(creature, jobType, false);
    }

    @Override
    public boolean assignTask(ICreatureController creature, Creature.JobType jobType) {
        return assignTask(creature, jobType, true);
    }

    private boolean assignTask(ICreatureController creature, Creature.JobType jobType, boolean assign) {
        switch (jobType) {
            case RESEARCH: {
                return assignClosestRoomTask(creature, ObjectType.RESEARCHER, assign);
            }
            default:
                return false;
        }
    }

    @Override
    public boolean assignSleepTask(ICreatureController creature) {
        GoToSleep task = new GoToSleep(gameWorldController, mapController, creature);
        if (task.isReachable(creature)) {
            task.assign(creature, true);
            return true;
        }
        return false;
    }

}
