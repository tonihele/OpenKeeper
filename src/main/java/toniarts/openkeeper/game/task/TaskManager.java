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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import com.simsilica.event.EventBus;
import toniarts.openkeeper.game.component.Casino;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.ClaimingFloor;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DiggingTiles;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DropArea;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperAiNoPrisoner;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.TrapDeliveries;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.CasinoController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.ThreeByThreeController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.event.RoomWanted;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.creature.ClaimLair;
import toniarts.openkeeper.game.task.creature.CollectPay;
import toniarts.openkeeper.game.task.creature.CollectWinnings;
import toniarts.openkeeper.game.task.creature.Explore;
import toniarts.openkeeper.game.task.creature.Gamble;
import toniarts.openkeeper.game.task.creature.GoToEat;
import toniarts.openkeeper.game.task.creature.GoToSleep;
import toniarts.openkeeper.game.task.creature.Guard;
import toniarts.openkeeper.game.task.creature.Leave;
import toniarts.openkeeper.game.task.creature.Manufacture;
import toniarts.openkeeper.game.task.creature.Pray;
import toniarts.openkeeper.game.task.creature.Rebel;
import toniarts.openkeeper.game.task.creature.Research;
import toniarts.openkeeper.game.task.creature.Sulk;
import toniarts.openkeeper.game.task.creature.Torture;
import toniarts.openkeeper.game.task.creature.Train;
import toniarts.openkeeper.game.task.objective.AbstractObjectiveTask;
import toniarts.openkeeper.game.task.objective.KillCreatures;
import toniarts.openkeeper.game.task.objective.KillPlayer;
import toniarts.openkeeper.game.task.objective.SendToActionPoint;
import toniarts.openkeeper.game.task.objective.StealGold;
import toniarts.openkeeper.game.task.worker.ArmTrapTask;
import toniarts.openkeeper.game.task.worker.BuildDoorTask;
import toniarts.openkeeper.game.task.worker.CaptureEnemyCreatureTask;
import toniarts.openkeeper.game.task.worker.CarryCorpseToGraveyardTask;
import toniarts.openkeeper.game.task.worker.CarryEnemyCreatureToPrison;
import toniarts.openkeeper.game.task.worker.CarryGoldToTreasuryTask;
import toniarts.openkeeper.game.task.worker.CarryObjectToStorageTask;
import toniarts.openkeeper.game.task.worker.ClaimRoomTask;
import toniarts.openkeeper.game.task.worker.ClaimTileTask;
import toniarts.openkeeper.game.task.worker.ClaimWallTileTask;
import toniarts.openkeeper.game.task.worker.DeliverDoorCrateTask;
import toniarts.openkeeper.game.task.worker.DeliverTrapCrateTask;
import toniarts.openkeeper.game.task.worker.DigTileTask;
import toniarts.openkeeper.game.task.worker.FetchCorpseTask;
import toniarts.openkeeper.game.task.worker.FetchObjectTask;
import toniarts.openkeeper.game.task.worker.RepairWallTileTask;
import toniarts.openkeeper.game.task.worker.RescueCreatureTask;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Task manager for several players. Can assign creatures to different tasks.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TaskManager implements ITaskManager, IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(TaskManager.class.getName());
    private static final int MAX_WORKERS_LOOKING_FOR_WORK = 4;

    /**
     * How far around the drop a dropped worker looks for work first, in tiles
     * in every direction. The original (DKII.EXE 0x463DF0) searches 2 tiles
     * around a dropped imp before its usual work.
     */
    static final int DROP_AREA_RADIUS = 2;

    /**
     * How many random tiles a creature that explores tries (DK II 0x45A290)
     */
    private static final int EXPLORE_TRIES = 20;

    private final IMapController mapController;
    private final IGameWorldController gameWorldController;
    private final IObjectsController objectsController;
    private final ICreaturesController creaturesController;
    private final INavigationService navigationService;
    private final ILevelInfo levelInfo;
    private final IEntityPositionLookup entityPositionLookup;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final EntityData entityData;
    private final EntitySet taskEntities;
    private final EntitySet unconsciousEntities;
    private final EntitySet corpseEntities;
    private final EntitySet freeObjectEntities;
    private final EntitySet trapBlueprintEntities;
    private final EntitySet doorBlueprintEntities;
    private final EntitySet crateEntities;
    private final Map<EntityId, Task> fetchCrateTasks = new HashMap<>();
    private boolean crateDeliveryTasksScanned = false;
    private final Map<Short, Set<Task>> taskQueues;
    private final Map<Long, Task> tasksByIds = new HashMap<>();
    private final Map<EntityId, Long> tasksIdsByEntities = new HashMap<>();
    private final Map<EntityId, ICreatureController> creaturesByEntities = new HashMap<>();
    private final Map<Short, IPlayerController> playerControllers;
    private final Map<IRoomController, Map<Long, Point>> roomTasks = new HashMap<>();
    private final Map<ICreatureController, Consumer<Boolean>> unemployedCreatures = new LinkedHashMap<>();
    private final Set<ICreatureController> escapedThieves = new HashSet<>();
    private final TaskWorkerPriorityComparator taskWorkerPriorityComparator = new TaskWorkerPriorityComparator();

    public TaskManager(EntityData entityData, IGameWorldController gameWorldController, IMapController mapController,
            IObjectsController objectsController, ICreaturesController creaturesController, INavigationService navigationService,
            Collection<IPlayerController> players, ILevelInfo levelInfo, IEntityPositionLookup entityPositionLookup,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.gameWorldController = gameWorldController;
        this.objectsController = objectsController;
        this.creaturesController = creaturesController;
        this.navigationService = navigationService;
        this.levelInfo = levelInfo;
        this.entityPositionLookup = entityPositionLookup;
        this.gameSettings = gameSettings;

        // Set the players
        // Create a queue for each managed player (everybody except Good & Neutral)
        taskQueues = HashMap.newHashMap(players.size());
        playerControllers = HashMap.newHashMap(players.size());
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

        // Listen to entities having a task, for cleanup purposes
        taskEntities = entityData.getEntities(TaskComponent.class);
        processAddedTasks(taskEntities);

        // Listen to rescue/capture missions
        unconsciousEntities = entityData.getEntities(CreatureComponent.class, Health.class, Owner.class, Unconscious.class);
        processAddedUnconsciousEntities(unconsciousEntities);

        // Listen to corpse robbing missions
        corpseEntities = entityData.getEntities(CreatureComponent.class, Death.class);
        processAddedCorpseEntities(corpseEntities);

        // Listen to object picking up missions
        freeObjectEntities = entityData.getEntities(ObjectComponent.class, Position.class, Placeable.class);
        processAddedFreeObjectEntities(freeObjectEntities);

        // Listen to trap and door blueprints and crates, workers bring the crates to the blueprints. Not the crate
        // positions, they change all the time while a crate is carried
        trapBlueprintEntities = entityData.getEntities(TrapComponent.class, TrapViewState.class, Owner.class, Position.class);
        doorBlueprintEntities = entityData.getEntities(DoorComponent.class, Owner.class, Position.class);
        crateEntities = entityData.getEntities(Crate.class, Owner.class);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        taskEntities.release();
        unconsciousEntities.release();
        corpseEntities.release();
        freeObjectEntities.release();
        trapBlueprintEntities.release();
        doorBlueprintEntities.release();
        crateEntities.release();
    }

    @Override
    public void processTick(float tpf) {
        removeEscapedThieves();
        if (taskEntities.applyChanges()) {
            processDeletedTasks(taskEntities.getRemovedEntities());
            processAddedTasks(taskEntities.getAddedEntities());
            processChangedTasks(taskEntities.getChangedEntities());
            removeUnusedTasks();
        }
        if (unconsciousEntities.applyChanges()) {
            processDeletedUnconsciousEntities(unconsciousEntities.getRemovedEntities());
            processAddedUnconsciousEntities(unconsciousEntities.getAddedEntities());
        }
        if (corpseEntities.applyChanges()) {
            processDeletedCorpseEntities(corpseEntities.getRemovedEntities());
            processAddedCorpseEntities(corpseEntities.getAddedEntities());
        }
        if (freeObjectEntities.applyChanges()) {
            processDeletedFreeObjectEntities(freeObjectEntities.getRemovedEntities());
            processAddedFreeObjectEntities(freeObjectEntities.getAddedEntities());
        }
        boolean trapBlueprintsChanged = trapBlueprintEntities.applyChanges();
        boolean doorBlueprintsChanged = doorBlueprintEntities.applyChanges();

        // Placing a trap or a door sends the creatures on their way to work, and the idle ones, back to choose again,
        // like a room change does (0x4C3860 and 0x4C33B0 call 0x4E8D30)
        for (Entity entity : trapBlueprintEntities.getAddedEntities()) {
            if (entity.get(TrapViewState.class).blueprint) {
                resetCreatures(entity.get(Owner.class).ownerId);
            }
        }
        for (Entity entity : doorBlueprintEntities.getAddedEntities()) {
            if (entity.get(DoorComponent.class).blueprint) {
                resetCreatures(entity.get(Owner.class).ownerId);
            }
        }
        if (crateEntities.applyChanges() || trapBlueprintsChanged || doorBlueprintsChanged || !crateDeliveryTasksScanned) {
            updateCrateDeliveryTasks();
            crateDeliveryTasksScanned = true;
        }
    }

    /**
     * Drops the crate delivery tasks that are done or can't be done anymore
     * and gives the trap and door blueprints waiting for a crate a delivery,
     * if there is one
     */
    private void updateCrateDeliveryTasks() {
        Iterator<Task> iter = fetchCrateTasks.values().iterator();
        while (iter.hasNext()) {
            Task task = iter.next();
            if (task.isRemovable()) {
                iter.remove();
                for (Set<Task> tasks : taskQueues.values()) {
                    tasks.remove(task);
                }
            }
        }
        for (Set<Task> tasks : taskQueues.values()) {
            tasks.removeIf(task -> (task instanceof DeliverTrapCrateTask || task instanceof DeliverDoorCrateTask) && task.isRemovable());
        }

        // The fetch tasks by blueprint, their target is the crate
        Map<EntityId, EntityId> underWay = HashMap.newHashMap(fetchCrateTasks.size());
        for (Entry<EntityId, Task> task : fetchCrateTasks.entrySet()) {
            underWay.put(task.getKey(), task.getValue().getTaskTarget());
        }
        for (Short playerId : taskQueues.keySet()) {
            SequencedMap<EntityId, Short> trapBlueprints = getBlueprints(trapBlueprintEntities, playerId,
                    entity -> entity.get(TrapViewState.class).blueprint, entity -> entity.get(TrapComponent.class).trapId);
            for (Entry<EntityId, EntityId> delivery : TrapDeliveries.next(trapBlueprints, getCrates(ResearchableType.TRAP, playerId),
                    underWay).entrySet()) {
                Point tile = WorldUtils.vectorToPoint(trapBlueprintEntities.getEntity(delivery.getKey()).get(Position.class).position);
                ArmTrapTask task = new ArmTrapTask(this, navigationService, mapController, entityData, gameWorldController.getTrapsController(),
                        objectsController, delivery.getKey(), tile, delivery.getValue(), playerId);
                fetchCrateTasks.put(delivery.getKey(), task);
                addTask(playerId, task);
            }

            SequencedMap<EntityId, Short> doorBlueprints = getBlueprints(doorBlueprintEntities, playerId,
                    entity -> entity.get(DoorComponent.class).blueprint, entity -> entity.get(DoorComponent.class).doorId);
            for (Entry<EntityId, EntityId> delivery : TrapDeliveries.next(doorBlueprints, getCrates(ResearchableType.DOOR, playerId),
                    underWay).entrySet()) {
                Point tile = WorldUtils.vectorToPoint(doorBlueprintEntities.getEntity(delivery.getKey()).get(Position.class).position);
                BuildDoorTask task = new BuildDoorTask(this, navigationService, mapController, entityData, gameWorldController.getDoorsController(),
                        objectsController, delivery.getKey(), tile, delivery.getValue(), playerId);
                fetchCrateTasks.put(delivery.getKey(), task);
                addTask(playerId, task);
            }
        }
    }

    /**
     * The keeper's blueprints in the order they were placed
     *
     * @param entities the traps or the doors
     * @param playerId the keeper
     * @param isBlueprint whether the trap or door is still a blueprint
     * @param typeId the trap or door id
     * @return the blueprints and their trap or door ids
     */
    private static SequencedMap<EntityId, Short> getBlueprints(EntitySet entities, short playerId, Predicate<Entity> isBlueprint,
            Function<Entity, Short> typeId) {
        List<Entity> placed = new ArrayList<>();
        for (Entity entity : entities) {
            if (isBlueprint.test(entity) && entity.get(Owner.class).ownerId == playerId) {
                placed.add(entity);
            }
        }
        placed.sort(Comparator.comparingLong(entity -> entity.getId().getId()));
        SequencedMap<EntityId, Short> blueprints = new LinkedHashMap<>();
        for (Entity entity : placed) {
            blueprints.put(entity.getId(), typeId.apply(entity));
        }
        return blueprints;
    }

    private Map<EntityId, Short> getCrates(ResearchableType type, short playerId) {
        Map<EntityId, Short> crates = new LinkedHashMap<>();
        for (Entity entity : crateEntities) {
            Crate crate = entity.get(Crate.class);
            if (crate.type == type && entity.get(Owner.class).ownerId == playerId) {
                crates.put(entity.getId(), crate.typeId);
            }
        }
        return crates;
    }

    private void processAddedTasks(Set<Entity> entities) {
        for (Entity entity : entities) {
            long taskId = entity.get(TaskComponent.class).taskId;
            tasksIdsByEntities.put(entity.getId(), taskId);
            trackCreature(entity.getId());
        }
    }

    /**
     * Remembers the creature that has a task. When it is killed its entity is
     * removed outright and a controller can't be made of it anymore, but the
     * task still needs to let go of it
     */
    private void trackCreature(EntityId entityId) {
        if (entityData.getComponent(entityId, CreatureComponent.class) != null) {
            creaturesByEntities.put(entityId, creaturesController.createController(entityId));
        }
    }

    private void processDeletedTasks(Set<Entity> entities) {
        for (Entity entity : entities) {
            Long taskId = tasksIdsByEntities.remove(entity.getId());
            ICreatureController creature = creaturesByEntities.remove(entity.getId());
            Task task = tasksByIds.get(taskId);
            if (task != null && creature != null) {
                task.unassign(creature);
            }
        }
    }

    private void processChangedTasks(Set<Entity> entities) {
        for (Entity entity : entities) {
            long taskId = entity.get(TaskComponent.class).taskId;
            Long oldTaskId = tasksIdsByEntities.put(entity.getId(), taskId);
            trackCreature(entity.getId());
            ICreatureController creature = creaturesByEntities.get(entity.getId());
            if (oldTaskId == null || taskId == oldTaskId) {
                continue;
            }

            Task task = tasksByIds.get(oldTaskId);
            if (task != null && creature != null) {
                task.unassign(creature);
            }
        }
    }

    /**
     * Forgets the tasks that nobody has and that can't be given out anymore.
     * Most tasks, such as the room, sleep and objective tasks, are made for
     * one creature and are only looked up by the ID in its task component.
     * Kept, they would pile up for the whole game
     */
    private void removeUnusedTasks() {
        Set<Long> usedTaskIds = new HashSet<>(tasksIdsByEntities.values());
        for (Set<Task> tasks : taskQueues.values()) {
            for (Task task : tasks) {
                usedTaskIds.add(task.getId());
            }
        }
        tasksByIds.values().removeIf(task -> task.getAssigneeCount() == 0 && !usedTaskIds.contains(task.getId()));
    }

    private void processAddedUnconsciousEntities(Set<Entity> entities) {

        // Add rescue mission for the own troops and capture for the enemy
        for (Entity entity : entities) {
            Owner owner = entity.get(Owner.class);
            KeeperAiNoPrisoner noPrisoner = entityData.getComponent(entity.getId(), KeeperAiNoPrisoner.class);
            for (Entry<Short, Set<Task>> entry : taskQueues.entrySet()) {

                Task task = null;
                if (entry.getKey() == owner.ownerId) {

                    // Rescue
                    task = new RescueCreatureTask(this, navigationService, mapController, creaturesController.createController(entity.getId()), entry.getKey());
                } else if (playerControllers.get(entry.getKey()).getKeeper().isEnemy(owner.ownerId)
                        && (noPrisoner == null || (noPrisoner.players & 1 << entry.getKey()) == 0)) {

                    // Capture, unless a computer player has decided to let it die
                    task = new CaptureEnemyCreatureTask(navigationService, mapController, creaturesController.createController(entity.getId()), entry.getKey(), this);
                }

                if (task == null) {
                    continue;
                }

                entry.getValue().add(task);
                tasksByIds.put(task.getId(), task);
            }
        }
    }

    private void processDeletedUnconsciousEntities(Set<Entity> entities) {

        // The creature got up, was locked up or died, there is nobody to rescue or capture anymore
        for (Entity entity : entities) {
            for (Set<Task> tasks : taskQueues.values()) {
                tasks.removeIf(task -> (task instanceof RescueCreatureTask || task instanceof CaptureEnemyCreatureTask)
                        && entity.getId().equals(task.getTaskTarget()));
            }
        }
    }

    private void processAddedCorpseEntities(Set<Entity> entities) {

        // Add graveyard missions, only the creatures that leave a body behind
        for (Entity entity : entities) {
            Creature creature = levelInfo.getLevelData().getCreature(entity.get(CreatureComponent.class).creatureId);
            if (creature == null || !creature.getFlags().contains(Creature.CreatureFlag.GENERATE_DEAD_BODY)
                    || entityData.getComponent(entity.getId(), Position.class) == null) {
                continue;
            }
            for (Short playerId : taskQueues.keySet()) {
                addTask(playerId, new FetchCorpseTask(navigationService, mapController, entityData, this, entity.getId(), playerId));
            }
        }
    }

    private void processDeletedCorpseEntities(Set<Entity> entities) {
        // TODO:
    }

    private void processAddedFreeObjectEntities(Set<Entity> entities) {

        // Add fetch object missions for changed objects
        for (Entity entity : entities) {
            Point p = WorldUtils.vectorToPoint(entity.get(Position.class).position);
            IMapTileInformation tile = mapController.getMapData().getTile(p);
            short playerId = tile.getOwnerId();
            if (!taskQueues.containsKey(playerId)) {
                continue;
            }
            createFetchObjectTask(entity, playerId);
        }
    }

    private void processDeletedFreeObjectEntities(Set<Entity> entities) {
        // TODO:
    }

    private void addListeners(Collection<IPlayerController> players) {

        // We want to be notified on tile changes, we are event based, not constantly scanning type
        this.mapController.addListener(new MapListener() {

            @Override
            public void onTilesChange(List<Point> updatedTiles) {
                for (Point tile : updatedTiles) {
                    scanTerrainTasks(tile, true, true);
                    scanFetchObjectTasks(tile);
                }
            }

            @Override
            public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
                // Not interested
            }

            @Override
            public void onTilesReveal(List<Point> points, boolean explore, short keeperId) {
                // Not interested
            }

            @Override
            public void onSightOfEvil(Point centre, int radius, int durationTicks, short keeperId) {
                // Not interested
            }

            @Override
            public void onFogOfWarDisabled(short keeperId) {
                // Not interested
            }

            @Override
            public void onFogOfWarReset(short keeperId) {
                // Not interested
            }

        });

        // Bridges! They open up new opportunities in new lands
        // Here we got capturing
        for (IPlayerController player : players) {
            this.mapController.addListener(player.getKeeper().getId(), new RoomListener() {

                @Override
                public void onBuild(IRoomController room) {

                }

                @Override
                public void onCaptured(IRoomController room) {
                    scanBridgeSurroundings(room);
                }

                @Override
                public void onCapturedByEnemy(IRoomController room) {

                }

                @Override
                public void onSold(IRoomController room) {

                }

                private void scanBridgeSurroundings(IRoomController room) {
                    if (room.getRoom().getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) || room.getRoom().getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER)) {

                        // Just gather the adjacent tiles to the brigde
                        Set<Point> roomPoints = new HashSet<>(room.getRoomInstance().getCoordinates());
                        Set<Point> adjacentPoints = new HashSet<>();
                        for (Point p : roomPoints) {
                            for (Point adjacentPoint : WorldUtils.getSurroundingTiles(mapController.getMapData(), p, false)) {
                                if (!roomPoints.contains(adjacentPoint)) {
                                    adjacentPoints.add(adjacentPoint);
                                }
                            }

                            // Scan all the adjacent tiles for tasks
                            for (Point adjacentPoint : adjacentPoints) {
                                scanTerrainTasks(adjacentPoint, false, true);
                            }
                        }
                    }
                }
            });
        }

        // Bridges! They open up new opportunities in new lands
        this.gameWorldController.addListener(new PlayerActionListener() {

            @Override
            public void onBuild(short keeperId, List<Point> tiles) {
                scanBridgeSurroundings(tiles);
            }

            @Override
            public void onSold(short keeperId, List<Point> tiles) {
                scanBridgeSurroundings(tiles);
                for (Point tile : tiles) {
                    scanFetchObjectTasks(tile);
                }
            }

            @Override
            public void onRoomSelectionComplete(short keeperId, List<Point> tiles) {
                resetCreatures(keeperId);
            }

            private void scanBridgeSurroundings(List<Point> tiles) {

                // Check first to see if we are on land or water
                Terrain terrain = mapController.getTerrain(mapController.getMapData().getTile(tiles.get(0)));
                if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    Room room = levelInfo.getLevelData().getRoomByTerrain(terrain.getTerrainId());
                    if (!(room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) || room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER))) {
                        return;
                    }
                } else if (!(levelInfo.getLevelData().getMap().getLava().equals(terrain) || levelInfo.getLevelData().getMap().getWater().equals(terrain))) {
                    return;
                }

                // Just gather the adjacent tiles to the brigde
                Set<Point> roomPoints = new HashSet<>(tiles);
                Set<Point> adjacentPoints = new HashSet<>();
                for (Point tile : roomPoints) {
                    for (Point adjacentPoint : WorldUtils.getSurroundingTiles(mapController.getMapData(), tile, false)) {
                        if (adjacentPoint != null) {
                            if (!roomPoints.contains(adjacentPoint)) {
                                adjacentPoints.add(adjacentPoint);
                            }
                        }
                    }

                    // Scan all the adjacent tiles for tasks
                    for (Point mapTile : adjacentPoints) {
                        scanTerrainTasks(mapTile, false, true);
                    }
                }
            }

        });
    }

    /**
     * Stops the keeper's creatures that are on their way to a job, or idle,
     * and sends them back to choose what to do. The original does this every
     * time the keeper's rooms change (CPlayer::resetCreaturesState 0x4E8D30,
     * called from the room list update 0x4F04F0) and when the keeper places a
     * door or a trap (0x4C33B0, 0x4C3860). It resets a creature doing nothing
     * (states 3 and 4), on an idle walk (state 6 going back to state 1), which a
     * worker takes too, or walking to a room (state 7). A worker's other idle
     * time and its work are left alone, and so are creatures on their way to
     * make their lair: the original sends those back too, a mistake Flame's
     * creatures_setup_lair_fix corrects. Creatures already in the room of
     * their job carry on too, as the original only resets the walk to a room.
     *
     * @param playerId the keeper whose rooms changed
     */
    private void resetCreatures(short playerId) {
        for (EntityId entityId : entityData.findEntities(new FieldFilter<>(Owner.class, "ownerId", playerId), Owner.class, CreatureComponent.class)) {
            ICreatureController creature = creaturesController.createController(entityId);
            boolean reset;
            if (creature.getStateMachine().isInState(CreatureState.IDLE)) {
                reset = !creature.isWorker() || !creature.isStopped();
            } else {
                reset = !creature.isWorker() && creature.getStateMachine().isInState(CreatureState.WORK)
                        && isWalkingToJobRoom(creature, entityData.getComponent(entityId, TaskComponent.class));
            }
            if (reset) {
                creature.stopCreature();
                creature.getStateMachine().changeState(CreatureState.IDLE);
            }
        }
    }

    private boolean isWalkingToJobRoom(ICreatureController creature, TaskComponent task) {
        if (task == null || task.taskType == TaskType.CLAIM_LAIR || creature.isAtAssignedTaskTarget()) {
            return false;
        }

        // A creature already in the room starts the job without the walk (0x471A70), and moving about inside the room
        // is part of the job
        IRoomController room = task.targetLocation != null ? mapController.getRoomControllerByCoordinates(task.targetLocation) : null;
        return room == null || room != mapController.getRoomControllerByCoordinates(creature.getCreatureCoordinates());
    }

    private void scanInitialTasks() {
        IMapDataInformation<IMapTileController> mapData = mapController.getMapData();
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                scanTerrainTasks(mapData.getTile(x, y), false, false);
            }
        }
    }

    private void scanTerrainTasks(final Point tile, final boolean checkNeighbours, final boolean deleteObsolete) {
        scanTerrainTasks(mapController.getMapData().getTile(tile), tile, checkNeighbours, deleteObsolete);
    }

    private void scanTerrainTasks(final IMapTileController mapTile, final boolean checkNeighbours, final boolean deleteObsolete) {
        scanTerrainTasks(mapTile, mapTile.getLocation(), checkNeighbours, deleteObsolete);
    }

    private void scanTerrainTasks(final IMapTileController mapTile, final Point tile, final boolean checkNeighbours, final boolean deleteObsolete) {
        Terrain terrain = null;
        if (mapTile != null) {
            terrain = mapController.getTerrain(mapTile);
        }

        for (Entry<Short, Set<Task>> entry : taskQueues.entrySet()) {

            // Scan existing tasks that are they valid, should be only one tile task per tile?
            if (deleteObsolete) {
                removeObsoleteTileTasks(entry.getValue());
            }

            if (mapTile == null) {
                continue;
            }

            // Perhaps we should have a store for these, since only one of such per player can exist, would save IDs
            createTerrainTask(entry.getKey(), terrain, mapTile, tile);
        }

        // See the neighbours
        if (checkNeighbours) {
            for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), tile, false)) {
                scanTerrainTasks(p, false, false);
            }
        }
    }

    private void removeObsoleteTileTasks(Set<Task> tasks) {
        Iterator<Task> iter = tasks.iterator();
        while (iter.hasNext()) {
            Task task = iter.next();
            if (task instanceof AbstractTileTask tileTask && tileTask.isRemovable()) {
                iter.remove();
            }
        }
    }

    private void createTerrainTask(short playerId, Terrain terrain, IMapTileController mapTile, Point tile) {

        // Dig
        if (mapController.isSelected(mapTile, playerId)) {
            addTask(playerId, new DigTileTask(navigationService, mapController, tile, playerId, entityData, entityPositionLookup,
                    (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue(),
                    levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
        } // Claim wall
        else if (mapController.isClaimableWall(mapTile, terrain, playerId)) {

            // Nobody's reinforced wall only becomes work once it is below half its health (DKII.EXE 0x453E90)
            if (!terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) || mapTile.getHealth() < mapTile.getMaxHealth() / 2) {
                addTask(playerId, new ClaimWallTileTask(navigationService, mapController, tile, playerId));
            }
        } // Claim
        else if (mapController.isClaimableTile(mapTile, terrain, playerId)) {
            addTask(playerId, new ClaimTileTask(navigationService, mapController, tile, playerId, levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
        } // Repair wall
        else if (mapController.isRepairableWall(mapTile, terrain, playerId)) {
            addTask(playerId, new RepairWallTileTask(navigationService, mapController, tile, playerId));
        } // Claim room
        else if (mapController.isClaimableRoom(mapTile, terrain, playerId)) {
            addTask(playerId, new ClaimRoomTask(navigationService, mapController, tile, playerId, levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
        }
    }

    private void scanFetchObjectTasks(Point tile) {

        // Add the tasks to tile owner and only if the object is not already in storage
        short playerId = mapController.getMapData().getTile(tile).getOwnerId();
        if (!taskQueues.containsKey(playerId)) {
            return;
        }
        for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
            Entity entity = entityData.getEntity(entityId, ObjectComponent.class, Placeable.class, Position.class);
            createFetchObjectTask(entity, playerId);
        }
    }

    private void createFetchObjectTask(Entity entity, short playerId) {
        ObjectComponent objectComponent = entity.get(ObjectComponent.class);
        if (objectComponent != null && objectComponent.objectType != null && entityData.getComponent(entity.getId(), Stored.class) == null) {
            addTask(playerId, getObjectTask(objectsController.createController(entity.getId()), playerId));
        }
    }

    @Override
    public void addTask(short playerId, Task task) {
        Set<Task> tasks = taskQueues.get(playerId);
        if (!tasks.contains(task)) {
            tasks.add(task);
            tasksByIds.put(task.getId(), task);
            logger.log(Level.INFO, "Added task {0} for player {1}!", new Object[]{task, playerId});
        } else {
            logger.log(Level.WARNING, "Already a task {0} for player {1}!", new Object[]{task, playerId});
        }
    }

    @Override
    public boolean assignGoldToTreasuryTask(ICreatureController creature) {

        // See if the creature's player lacks of gold. When the gold doesn't fit, DK II asks for a Treasury
        // if there is none (0x4BA420)
        IPlayerController player = playerControllers.get(creature.getOwnerId());
        if (!player.getGoldControl().isFullCapacity()) {
            return assignClosestRoomTask(creature, ObjectType.GOLD, null);
        }
        EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(creature.getOwnerId(), ObjectType.GOLD));
        return false;
    }

    @Override
    public boolean assignClosestRoomTask(ICreatureController creature, ObjectType objectType, EntityId target) {
        return assignClosestRoomTask(creature, objectType, target, true);
    }

    /**
     * Sends a creature working in a room to the closest point of the same room
     * that has no task. The point the creature is at still has its task, so it
     * is never picked
     *
     * @param creature the working creature
     * @param room the room the creature works in
     * @param objectType the type of room service
     * @return true if the creature was given a new task
     */
    public boolean assignFreeRoomTask(ICreatureController creature, IRoomController room, ObjectType objectType) {
        Map<Long, Point> taskPoints = roomTasks.computeIfAbsent(room, r -> new HashMap<>());
        Set<Point> points = new HashSet<>(taskPoints.values());
        Point currentPosition = creature.getCreatureCoordinates();
        IRoomObjectControl<?> control = room.getObjectControl(objectType);
        Point target = control.getAvailableCoordinates().stream()
                .filter(p -> !points.contains(p) && room.isTileAccessible(null, p))
                .min(Comparator.comparingInt(p -> WorldUtils.calculateDistance(currentPosition, p)))
                .orElse(null);
        if (target == null || !navigationService.isReachable(currentPosition, target, creature)) {
            return false;
        }

        AbstractCapacityCriticalRoomTask task = (AbstractCapacityCriticalRoomTask) getRoomTask(objectType, target, null, creature, room);
        taskPoints.put(task.getId(), target);
        task.assign(creature, true);
        tasksByIds.put(task.getId(), task);
        return true;
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
    private boolean assignClosestRoomTask(ICreatureController creature, ObjectType objectType, EntityId targetEntity, boolean assign) {
        return assignClosestRoomTask(creature, objectType, targetEntity, assign,
                room -> room.getObjectControl(objectType).isFullCapacity() ? Collections.emptyList() : room.getObjectControl(objectType).getAvailableCoordinates());
    }

    private boolean assignClosestRoomTask(ICreatureController creature, ObjectType objectType, EntityId targetEntity, boolean assign,
            Function<IRoomController, Collection<Point>> spots) {
        Point currentPosition = creature.getCreatureCoordinates();

        // Get all the rooms of the given type. Without any, the keeper is told the creature wants one, as DK II's
        // room searches do (for example 0x4E0470 for the Lair and 0x4F3560 for the Training Room)
        List<IRoomController> rooms = mapController.getRoomsByFunction(objectType, creature.getOwnerId());
        if (rooms.isEmpty()) {
            EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(creature.getOwnerId(), objectType));
        }
        Map<Integer, IRoomController> distancesToRooms = new TreeMap<>();
        for (IRoomController room : rooms) {

            // A destroyed room serves nobody, like a claimed ruined Dungeon Heart that no longer stores gold
            if (!room.isDestroyed() && !spots.apply(room).isEmpty()) {
                distancesToRooms.put(getShortestDistance(currentPosition, room.getRoomInstance().getCoordinates().toArray(new Point[0])), room
                );
            }
        }

        // See that are they really accessible starting from the least distance one
        for (IRoomController room : distancesToRooms.values()) {

            // The whole rooms are always accessible, take a random point from the room like DK II seems to do
            IRoomObjectControl control = room.getObjectControl(objectType);
            List<Point> coordinates = new ArrayList<>(spots.apply(room));
            Iterator<Point> iter = coordinates.iterator();
            Map<Long, Point> taskPoints = roomTasks.get(room);
            while (iter.hasNext()) {
                Point p = iter.next();
                if (!room.isTileAccessible(null, p)) {
                    iter.remove();
                }
            }

            // Take a free point first. A room can take more than it has points, like one more
            // trainee than the Training Room has dummies, so then the points are shared until it is full.
            // A Casino object takes one gambler only, DK II skips an object another creature has (0x4D7460)
            if (taskPoints != null) {
                List<Point> freeCoordinates = new ArrayList<>(coordinates);
                freeCoordinates.removeAll(taskPoints.values());
                if (!freeCoordinates.isEmpty() || objectType == ObjectType.GAMBLER
                        || taskPoints.size() + control.getCurrentCapacity() >= control.getMaxCapacity()) {
                    coordinates = freeCoordinates;
                }
            }

            if (objectType == ObjectType.TRAINEE) {
                coordinates = getTrainingSpot(coordinates, room, creature);
            }

            // Assign
            if (!coordinates.isEmpty()) {
                Point target;
                if (objectType == ObjectType.SPECIAL || objectType == ObjectType.SPELL_BOOK) {
                    Vector3f source = targetEntity != null ? EntityController.getPosition(entityData, targetEntity) : null;
                    float originX = source != null ? source.x / WorldUtils.TILE_WIDTH : currentPosition.x;
                    float originY = source != null ? source.z / WorldUtils.TILE_WIDTH : currentPosition.y;
                    target = coordinates.stream().min(Comparator.comparingDouble(p -> {
                        double dx = p.x - originX;
                        double dy = p.y - originY;
                        return dx * dx + dy * dy;
                    })).orElseThrow();
                } else {
                    target = Utils.getRandomItem(coordinates);
                }
                if (navigationService.isReachable(creature.getCreatureCoordinates(), target, creature)) {

                    // Assign the task
                    Task task = getRoomTask(objectType, target, targetEntity, creature, room);

                    // See if really assign
                    if (!assign) {
                        return task.isValid(creature);
                    }

                    if (task instanceof AbstractCapacityCriticalRoomTask abstractCapacityCriticalRoomTask) {
                        if (taskPoints == null) {
                            taskPoints = new HashMap<>();
                        }
                        taskPoints.put(abstractCapacityCriticalRoomTask.getId(), target);
                        roomTasks.put(room, taskPoints);
                    }
                    task.assign(creature, true);
                    tasksByIds.put(task.getId(), task);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * The torture devices with a victim on them
     */
    private Collection<Point> getTortureVictimSpots(IRoomController room) {
        List<Point> spots = new ArrayList<>();
        Set<EntityId> devices = new HashSet<>(room.getFloorFurniture());
        devices.addAll(room.getWallFurniture());
        for (EntityId device : devices) {
            Point p = WorldUtils.vectorToPoint(entityData.getComponent(device, Position.class).position);
            Collection<?> victims = room.getObjectControl(ObjectType.TORTUREE).getItems(p);
            if (victims != null && !victims.isEmpty()) {
                spots.add(p);
            }
        }
        return spots;
    }

    /**
     * Where the trainee trains, like DK II (Training_cpp_4F2FF0, 0x4F2FF0): at
     * the closest Training Dummy. A trainee with an attacking spell ready
     * shoots at the closest wall target instead half of the time, and when no
     * dummy is free. DK II sends a trainee without one to a wall target only
     * when the room has no dummies, but it can't train there.
     *
     * @param coordinates the free Training Dummy and wall target tiles
     * @param room the Training Room
     * @param creature the trainee
     * @return the tile of the dummy or the wall target, empty if none suits
     */
    private List<Point> getTrainingSpot(List<Point> coordinates, IRoomController room, ICreatureController creature) {
        Set<Point> wallTargets = getFurnitureTiles(room.getWallFurniture()).keySet();
        boolean ranged = creature.hasRangedAttackReady();
        boolean wallTargetFirst = ranged && Utils.getRandom().nextBoolean();
        Point from = creature.getCreatureCoordinates();
        Comparator<Point> closest = Comparator.comparingInt(p -> WorldUtils.calculateDistance(from, p));
        Optional<Point> spot = coordinates.stream().filter(p -> wallTargets.contains(p) == wallTargetFirst).min(closest);
        if (spot.isEmpty() && ranged) {
            spot = coordinates.stream().min(closest);
        }
        return spot.map(List::of).orElse(List.of());
    }

    /**
     * The tile a trainee shoots at a wall target from, like DK II
     * (Training_cpp_4F3310, 0x4F3310): the room tile closest to the trainee
     * that has no training furniture on it and sees the target
     *
     * @param room the Training Room
     * @param creature the trainee
     * @param wallTarget the wall target
     * @return the tile, {@code null} if there is none
     */
    private Point getShootingTile(IRoomController room, ICreatureController creature, EntityId wallTarget) {
        Vector3f target = entityData.getComponent(wallTarget, Position.class).position;
        Set<Point> furniture = new HashSet<>(getFurnitureTiles(room.getWallFurniture()).keySet());
        furniture.addAll(getFurnitureTiles(room.getFloorFurniture()).keySet());
        Point from = creature.getCreatureCoordinates();
        return room.getRoomInstance().getCoordinates().stream()
                .filter(p -> !furniture.contains(p) && room.isTileAccessible(null, p)
                && entityPositionLookup.hasLineOfSight(WorldUtils.pointToVector3f(p), target))
                .min(Comparator.comparingInt(p -> WorldUtils.calculateDistance(from, p)))
                .orElse(null);
    }

    private Map<Point, EntityId> getFurnitureTiles(Collection<EntityId> furniture) {
        Map<Point, EntityId> tiles = new HashMap<>(furniture.size());
        for (EntityId entityId : furniture) {
            Position position = entityData.getComponent(entityId, Position.class);
            if (position != null) {
                tiles.put(WorldUtils.vectorToPoint(position.position), entityId);
            }
        }
        return tiles;
    }

    private static Integer getShortestDistance(Point currentPosition, Point... coordinates) {
        int distance = Integer.MAX_VALUE;
        for (Point p : coordinates) {
            // TODO: do we need to do this diagonally?
            distance = Math.min(distance, WorldUtils.calculateDistance(currentPosition, p));
            if (distance == 0) {
                break;
            }
        }
        return distance;
    }

    private AbstractTask getRoomTask(ObjectType objectType, Point target, EntityId targetEntity, ICreatureController creature, IRoomController room) {
        switch (objectType) {
            case GOLD -> {
                return new CarryGoldToTreasuryTask(navigationService, mapController, target, creature.getOwnerId(), room, gameWorldController);
            }
            case LAIR -> {
                return new ClaimLair(navigationService, mapController, target, creature.getOwnerId(), room, this, gameSettings);
            }
            case RESEARCHER -> {
                return new Research(navigationService, mapController, target, creature.getOwnerId(), room, this, playerControllers.get(creature.getOwnerId()).getResearchControl(), objectsController);
            }
            case PRISONER -> {
                return new CarryEnemyCreatureToPrison(navigationService, mapController, target, creature.getOwnerId(), room, this, creaturesController.createController(targetEntity));
            }
            case SPECIAL, SPELL_BOOK, CRATE -> {
                return new CarryObjectToStorageTask(navigationService, mapController, target, creature.getOwnerId(), room, this, objectsController.createController(targetEntity));
            }
            case TORTUREE -> {
                return new Torture(navigationService, mapController, target, creature.getOwnerId(), room, this, entityData);
            }
            case CORPSE -> {
                return new CarryCorpseToGraveyardTask(navigationService, mapController, target, creature.getOwnerId(), room, this, entityData, targetEntity);
            }
            case TRAINEE -> {
                EntityId wallTarget = getFurnitureTiles(room.getWallFurniture()).get(target);
                return new Train(navigationService, mapController, target, creature.getOwnerId(), room, this, gameWorldController, gameSettings, playerControllers.get(creature.getOwnerId()),
                        wallTarget, wallTarget != null ? getShootingTile(room, creature, wallTarget) : null);
            }
            case MANUFACTURER -> {
                return new Manufacture(navigationService, mapController, target, creature.getOwnerId(), room, this,
                        playerControllers.get(creature.getOwnerId()).getManufactureControl(), objectsController);
            }
            case WORSHIPPER -> {
                return new Pray(navigationService, mapController, target, creature.getOwnerId(), room, this, entityData, gameSettings, playerControllers.get(creature.getOwnerId()).getManaControl());
            }
            case GUARD -> {
                return new Guard(navigationService, mapController, target, creature.getOwnerId(), room, this,
                        levelInfo.getLevelData().getGameLevel().getTicksPerSec(),
                        (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_TIME_IN_GUARD_ROOM_BEFORE_WANDERING_TO_GUARD_POST_SECONDS).getValue(),
                        FastMath.rand);
            }
            case GAMBLER -> {
                return new Gamble(navigationService, mapController, entityPositionLookup, target, creature.getOwnerId(), room, this, entityData,
                        gameWorldController, objectsController, gameSettings,
                        room instanceof CasinoController casino ? casino::getPayOut : () -> Casino.PayOut.SMILES,
                        FastMath.rand);
            }
            default -> {
                logger.log(Level.DEBUG, "No task defined for " + objectType);
                return null;
            }
        }
    }

    protected void removeRoomTask(AbstractCapacityCriticalRoomTask task) {
        Map<Long, Point> taskPoints = roomTasks.get(task.getRoom());
        if (taskPoints != null) {
            taskPoints.remove(task.getId());
            if (taskPoints.isEmpty()) {
                roomTasks.remove(task.getRoom());
            }
        }
    }

    @Override
    public boolean assignObjectiveTask(ICreatureController creature, Thing.HeroParty.Objective objective) {
        AbstractObjectiveTask task = null;
        switch (objective) {
            case SEND_TO_ACTION_POINT: {

                // Past his last waypoint the creature has no action point left to go to
                if (creature.getObjectiveTargetActionPointId() == 0) {
                    break;
                }
                ActionPoint actionPoint = levelInfo.getActionPoint(creature.getObjectiveTargetActionPointId());
                if (actionPoint != null) {
                    task = new SendToActionPoint(navigationService, mapController, actionPoint, creature,
                            levelInfo.getLevelData().getGameLevel().getTicksPerSec());
                } else {
                    logger.log(Level.ERROR, "Creature is sent to action point {0}, but the level doesn't have it!", creature.getObjectiveTargetActionPointId());
                }
                break;
            }
            case KILL_PLAYER: {

                // Without the keeper the creature has nobody to go after, it just carries on as usual
                if (KillPlayer.isTargetPresent(levelInfo, creature.getObjectiveTargetPlayerId())) {
                    task = new KillPlayer(navigationService, mapController, levelInfo, creature.getObjectiveTargetPlayerId(), creature);
                }
                break;
            }
            case KILL_CREATURES: {
                task = KillCreatures.create(navigationService, mapController, levelInfo, creature,
                        getCreatures(creature.getObjectiveTargetPlayerId()));
                break;
            }
            case STEAL_GOLD: {
                task = new StealGold(navigationService, mapController, gameWorldController, entityData, creature, escapedThieves::add);
                break;
            }
            default: {

                // The other objectives don't give the creature a task of its own
                break;
            }
        }

        // Assign
        if (task != null && task.getCurrentTask() != null) {
            task.getCurrentTask().assign(creature, true);
            tasksByIds.put(task.getId(), task);
            return true;
        }
        return false;
    }

    private List<ICreatureController> getCreatures(short playerId) {
        IPlayerController playerController = playerControllers.get(playerId);
        if (playerController == null) {
            return Collections.emptyList();
        }

        List<ICreatureController> creatures = new ArrayList<>();
        for (Set<EntityId> creatureEntities : playerController.getCreatureControl().getAllCreatures().values()) {
            for (EntityId entityId : creatureEntities) {

                // A creature removed this tick is still listed until the player creature system catches up
                if (entityData.getComponent(entityId, CreatureComponent.class) != null) {
                    creatures.add(creaturesController.createController(entityId));
                }
            }
        }

        return creatures;
    }

    /**
     * Thieves that got away with their loot leave the level. Not done while
     * the thief is busy with his own task
     */
    private void removeEscapedThieves() {
        for (ICreatureController thief : escapedThieves) {

            // Done with the task here, the thief won't be around to be told later
            thief.unassingCurrentTask();
            tasksIdsByEntities.remove(thief.getEntityId());
            thief.remove();
        }
        escapedThieves.clear();
    }

    private AbstractTask getObjectTask(IObjectController gameObject, short playerId) {
        return new FetchObjectTask(this, navigationService, mapController, gameObject, playerId);
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
                return assignClosestRoomTask(creature, ObjectType.RESEARCHER, null, assign);
            }
            case TRAIN: {
                return assignClosestRoomTask(creature, ObjectType.TRAINEE, null, assign);
            }
            case TORTURE: {

                // As in the original (0x483AE0), whip a victim if there is one, otherwise get on a free device
                return assignClosestRoomTask(creature, ObjectType.TORTUREE, null, assign, this::getTortureVictimSpots)
                        || assignClosestRoomTask(creature, ObjectType.TORTUREE, null, assign);
            }
            case MANUFACTURE: {
                return assignClosestRoomTask(creature, ObjectType.MANUFACTURER, null, assign);
            }
            case PRAY: {
                return assignClosestRoomTask(creature, ObjectType.WORSHIPPER, null, assign);
            }
            case GUARD: {
                return assignClosestRoomTask(creature, ObjectType.GUARD, null, assign);
            }
            case DRINK: {

                // DK II's Drink job (0x4560F0) only looks for a Casino, and so only asks for one, when the creature
                // holds more gold than the stake
                return creature.getGold() > Math.abs(gameSettings.get(Variable.MiscVariable.MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND).getValue())
                        && assignClosestRoomTask(creature, ObjectType.GAMBLER, null, assign);
            }
            case SULK: {

                // Like in DK II, only a creature that sleeps and has a lair it can get to sulks
                if (!creature.hasLair() || creature.getCreature().getAttributes().getTimeSleep() <= 0) {
                    return false;
                }
                Sulk task = new Sulk(navigationService, mapController, creature, entityData, gameSettings);
                if (!task.isReachable(creature)) {
                    return false;
                }
                if (assign) {
                    task.assign(creature, true);
                    tasksByIds.put(task.getId(), task);
                }
                return true;
            }
            case LEAVE: {
                return assignLeaveTask(creature, false, assign);
            }
            case EXPLORE: {

                // Like in DK II (0x45A290), the first of up to 20 random tiles of the map that nobody has claimed, that
                // isn't solid and that it can get to
                IMapDataInformation<IMapTileController> mapData = mapController.getMapData();
                for (int i = 0; i < EXPLORE_TRIES && mapData.getWidth() > 0 && mapData.getHeight() > 0; i++) {
                    Point p = new Point(Utils.getRandom().nextInt(mapData.getWidth()), Utils.getRandom().nextInt(mapData.getHeight()));
                    IMapTileController tile = mapData.getTile(p);
                    if (tile != null && tile.getOwnerId() == Player.NEUTRAL_PLAYER_ID && !mapController.isSolid(p)) {
                        Explore task = new Explore(navigationService, mapController, p, creature.getOwnerId());
                        if (task.isReachable(creature)) {
                            if (assign) {
                                task.assign(creature, true);
                                tasksByIds.put(task.getId(), task);
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
            case REBEL: {

                // Like in DK II, a rebel walks out through a portal, and turns on its keeper when it can't get to one
                if (assignLeaveTask(creature, true, assign)) {
                    return true;
                }
                if (creature.getOwnerId() == Player.GOOD_PLAYER_ID) {
                    return false;
                }
                if (assign) {
                    Rebel task = new Rebel(navigationService, mapController, creature,
                            levelInfo.getLevelData().getGameLevel().getTicksPerSec());
                    task.assign(creature, true);
                    tasksByIds.put(task.getId(), task);
                }
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Like in DK II, a creature that leaves walks out through the portal
     * nearest in a straight line, whoever owns it, even when another portal is
     * a shorter walk or it can't get to that one at all (0x459DE0). A rebel
     * takes the nearest of its own keeper's portals, and only when it can get
     * to it (0x475170)
     */
    private boolean assignLeaveTask(ICreatureController creature, boolean rebel, boolean assign) {
        ThreeByThreeController nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (IRoomController room : mapController.getRoomControllers()) {
            if (!(room instanceof ThreeByThreeController portal) || (rebel && portal.getOwnerId() != creature.getOwnerId())) {
                continue;
            }

            // DK II compares the squared distance to the middle of each portal
            float distance = WorldUtils.pointToVector2f(portal.getEntranceCoordinate())
                    .distanceSquared(creature.getPosition().x, creature.getPosition().z);
            if (distance < nearestDistance) {
                nearest = portal;
                nearestDistance = distance;
            }
        }
        Leave task = nearest == null ? null
                : new Leave(navigationService, mapController, nearest.getEntranceCoordinate(), creature.getOwnerId(), nearest, entityData);
        if (task != null && rebel && !task.isReachable(creature)) {
            return false;
        }
        if (task != null && assign) {
            task.assign(creature, true);
            tasksByIds.put(task.getId(), task);
        }
        return task != null;
    }

    @Override
    public boolean assignSleepTask(ICreatureController creature) {
        GoToSleep task = new GoToSleep(navigationService, mapController, creature);
        if (task.isReachable(creature)) {
            task.assign(creature, true);
            tasksByIds.put(task.getId(), task);
            return true;
        }
        return false;
    }

    @Override
    public boolean assignCollectPayTask(ICreatureController creature) {
        Point currentPosition = creature.getCreatureCoordinates();

        // The gold rooms that hold gold, nearest first
        List<IRoomController> rooms = new ArrayList<>(mapController.getRoomsByFunction(ObjectType.GOLD, creature.getOwnerId()));
        rooms.removeIf(room -> room.getObjectControl(ObjectType.GOLD).getCurrentCapacity() <= 0);
        Map<IRoomController, Integer> distances = new HashMap<>();
        for (IRoomController room : rooms) {
            distances.put(room, getShortestDistance(currentPosition, room.getRoomInstance().getCoordinates().toArray(new Point[0])));
        }
        rooms.sort(Comparator.comparingInt(distances::get));
        for (IRoomController room : rooms) {
            List<Point> coordinates = new ArrayList<>(room.getRoomInstance().getCoordinates());
            coordinates.removeIf(p -> !room.isTileAccessible(null, p));
            if (coordinates.isEmpty()) {
                continue;
            }

            Point target = Utils.getRandomItem(coordinates);
            if (navigationService.isReachable(currentPosition, target, creature)) {
                Task task = new CollectPay(navigationService, mapController, target, creature.getOwnerId(), room, gameWorldController, entityData);
                task.assign(creature, true);
                tasksByIds.put(task.getId(), task);

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean assignEatTask(ICreatureController creature) {

        // The creatures in the original don't seem to make anykind of reservations on the food items, I've seen them "fight over food", first come first serve
        // Hmm, is this a good way to find the food, performance-wise...?
        List<EntityId> foods = new ArrayList<>(entityData.findEntities(new FieldFilter(Owner.class, "ownerId", creature.getOwnerId()), Food.class, Owner.class, Position.class));
        if (foods.isEmpty()) {

            // DK II's search for food asks for a Hatchery when there is none (0x4DED00)
            EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(creature.getOwnerId(), ObjectType.FOOD));
            return false;
        }

        // Sort by manhattan distance in relation to the creature
        Collections.sort(foods, new Comparator<EntityId>() {

            private final Point currentLocation = creature.getCreatureCoordinates();

            @Override
            public int compare(EntityId entityId, EntityId entityId1) {
                int result = Integer.compare(WorldUtils.calculateDistance(currentLocation, entityPositionLookup.getEntityLocation(entityId).getLocation()), WorldUtils.calculateDistance(currentLocation, entityPositionLookup.getEntityLocation(entityId1).getLocation()));
                return result;
            }

        });

        // Pick closest we can actually access
        for (EntityId food : foods) {
            Point target = entityPositionLookup.getEntityLocation(food).getLocation();
            if (navigationService.isReachable(creature.getCreatureCoordinates(), target, creature)) {
                GoToEat task = new GoToEat(navigationService, mapController, entityPositionLookup, food, entityData, creature);
                task.assign(creature, true);
                tasksByIds.put(task.getId(), task);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean assignCollectWinningsTask(ICreatureController creature) {

        // The keeper's gold is in the Treasuries and the Dungeon Heart, go to the nearest one we can reach
        Point currentLocation = creature.getCreatureCoordinates();
        List<IRoomController> rooms = mapController.getRoomsByFunction(ObjectType.GOLD, creature.getOwnerId());
        rooms.sort(Comparator.comparing(room -> getShortestDistance(currentLocation, room.getRoomInstance().getCoordinates().toArray(new Point[0]))));
        for (IRoomController room : rooms) {
            List<Point> coordinates = new ArrayList<>(room.getRoomInstance().getCoordinates());
            coordinates.removeIf(p -> !room.isTileAccessible(null, p));
            Point target = Utils.getRandomItem(coordinates);
            if (target != null && navigationService.isReachable(currentLocation, target, creature)) {
                CollectWinnings task = new CollectWinnings(navigationService, mapController, target, creature.getOwnerId(), room, entityData, gameWorldController);
                task.assign(creature, true);
                tasksByIds.put(task.getId(), task);
                return true;
            }
        }

        return false;
    }

    @Override
    public Task getTaskById(long taskId) {
        Task task = tasksByIds.get(taskId);

        // For nested task, return the actual task
        if (task != null && task instanceof AbstractObjectiveTask nestedTask) {
            return nestedTask.getCurrentTask();
        }

        return task;
    }

    private IMapTileInformation getEntityPosition(Entity entity) {
        IMapTileInformation mapTile = entityPositionLookup.getEntityLocation(entity.getId());

        // If the entity is new, it might not be in the registry, rather unfortunatety problem...
        if (mapTile == null) {
            mapTile = mapController.getMapData().getTile(WorldUtils.vectorToPoint(EntityController.getPosition(entityData, entity.getId())));
        }

        return mapTile;
    }

    @Override
    public void addToUnemployedWorkerQueue(ICreatureController creature, Consumer<Boolean> workResult) {
        unemployedCreatures.put(creature, workResult);
    }

    @Override
    public void processUnemployedWorkerQueue() {
        if (unemployedCreatures.isEmpty()) {
            return;
        }

        // The queue is from the last tick, workers that have got something else to do since don't look for work
        Map<Short, List<Entry<ICreatureController, Consumer<Boolean>>>> workersByOwnerId = unemployedCreatures
                .entrySet()
                .stream()
                .filter((creature) -> creature.getKey().getStateMachine().isInState(CreatureState.IDLE))
                .collect(Collectors.groupingBy((creature) -> creature.getKey().getOwnerId()));
        unemployedCreatures.clear();
        workersByOwnerId.forEach((ownerId, workers) -> {
            processUnemployedWorkers(taskQueues.getOrDefault(ownerId, Collections.emptySet()), workers);
        });
    }

    private void processUnemployedWorkers(Set<Task> tasks, List<Entry<ICreatureController, Consumer<Boolean>>> workers) {

        // Only so many of a player's workers look for work on one tick, the others wander off and look again later. In
        // DK II the idle imp state (0x463DF0) counts the searches of the player's imps (CPlayer +0x7a, reset every tick
        // in 0x4B9EC0) and sends the fifth and the next ones to wander. A just dropped imp (CState +0x99) always looks and
        // isn't counted
        int searching = 0;
        Map<ICreatureController, Consumer<Boolean>> unemployedWorkers = new HashMap<>();
        for (Entry<ICreatureController, Consumer<Boolean>> worker : workers) {
            boolean dropped = entityData.getComponent(worker.getKey().getEntityId(), DropArea.class) != null;
            if (dropped || searching++ < MAX_WORKERS_LOOKING_FOR_WORK) {
                unemployedWorkers.put(worker.getKey(), worker.getValue());
            } else {
                worker.getValue().accept(Boolean.FALSE);
            }
        }
        Set<Task> taskQueue = new HashSet<>(tasks);

        // Process each available task for each available worker
        List<TaskWorkerPriority> priorizedTasks = new LinkedList<>();
        List<ICreatureController> goingHome = new ArrayList<>();
        for (Entry<ICreatureController, Consumer<Boolean>> workerEntry : unemployedWorkers.entrySet()) {
            ICreatureController creature = workerEntry.getKey();
            Consumer<Boolean> workResult = workerEntry.getValue();
            final Point currentLocation = creature.getCreatureCoordinates();
            DropArea dropArea = entityData.getComponent(creature.getEntityId(), DropArea.class);
            boolean claiming = entityData.getComponent(creature.getEntityId(), ClaimingFloor.class) != null;
            boolean digging = entityData.getComponent(creature.getEntityId(), DiggingTiles.class) != null;
            List<TaskWorkerPriority> workerTasks = new ArrayList<>();

            // Give points by distance and priority. The distance is to the task's own tile: finding the spot to work from, and
            // whether it can be reached, takes path finding, which canAssign() does only for the tasks being handed out (#671)
            for (Task task : taskQueue) {

                // A task is worked on its tile or next to it. One out of the worker's reach would cost a path search that
                // goes through all of the worker's land before it fails
                if (Arrays.stream(WorldUtils.getSurroundingTiles(mapController.getMapData(), task.getTaskLocation(), false))
                        .noneMatch(p -> navigationService.isReachable(currentLocation, p, creature))) {
                    continue;
                }

                boolean sameWork = (digging && isDigging(task)) || (claiming && isClaiming(task));
                int points = WorldUtils.calculateDistance(currentLocation, task.getTaskLocation()) + task.getPriority();
                int dropRing = dropArea != null ? getDropRing(task, dropArea.center) : Integer.MAX_VALUE;
                workerTasks.add(new TaskWorkerPriority(task, creature, points, dropRing, sameWork, workResult));
            }

            // Only the first search after the drop looks around it, then the worker is back to its usual work
            if (dropArea != null) {
                entityData.removeComponent(creature.getEntityId(), DropArea.class);
            }

            // A worker whose digging is done brings home the gold it carries before it takes other work
            boolean bringsGoldHome = digging && creature.getGold() > 0 && !hasMoreDigging(taskQueue, creature);
            if (bringsGoldHome && assignGoldToTreasuryTask(creature)) {
                setCurrentWork(creature, TaskType.CARRY_GOLD_TO_TREASURY);
                workResult.accept(Boolean.TRUE);
                goingHome.add(creature);
                continue;
            }
            priorizedTasks.addAll(workerTasks);
        }
        goingHome.forEach(unemployedWorkers::remove);

        // Go through the tasks and assign workers
        while (!unemployedWorkers.isEmpty() && !priorizedTasks.isEmpty()) {

            // With each iteration we sort to fill the jobs as even as possible (assignee count changes)
            Collections.sort(priorizedTasks, taskWorkerPriorityComparator);

            Iterator<TaskWorkerPriority> iter = priorizedTasks.iterator();
            while (iter.hasNext()) {
                TaskWorkerPriority taskWorkerPriority = iter.next();

                // See if the task is full or the worker is not unemployed anymore or that the task can't actually be handled by this creature
                if (!unemployedWorkers.containsKey(taskWorkerPriority.creature)
                        || taskWorkerPriority.task.isFull()
                        || !taskWorkerPriority.task.canAssign(taskWorkerPriority.creature)) {
                    iter.remove();
                    continue;
                }

                // Assign task
                taskWorkerPriority.task.assign(taskWorkerPriority.creature, true);
                setCurrentWork(taskWorkerPriority.creature, taskWorkerPriority.task.getTaskType());
                taskWorkerPriority.workResult.accept(Boolean.TRUE);

                // Prune list and re-sort tasks
                unemployedWorkers.remove(taskWorkerPriority.creature);
                iter.remove();
                break;
            }
        }

        // Finally anybody without assigment will get feedback
        unemployedWorkers.forEach((t, u) -> {
            setCurrentWork(t, null);
            u.accept(Boolean.FALSE);
        });
    }

    /**
     * Remembers whether the worker is claiming or digging, so that it carries
     * on with it
     *
     * @param creature the worker
     * @param taskType the task it got, {@code null} for none
     */
    private void setCurrentWork(ICreatureController creature, TaskType taskType) {
        if (taskType == TaskType.CLAIM_TILE || taskType == TaskType.CLAIM_ROOM) {
            entityData.setComponent(creature.getEntityId(), new ClaimingFloor());
        } else {
            entityData.removeComponent(creature.getEntityId(), ClaimingFloor.class);
        }
        if (taskType == TaskType.DIG_TILE) {
            entityData.setComponent(creature.getEntityId(), new DiggingTiles());
        } else {
            entityData.removeComponent(creature.getEntityId(), DiggingTiles.class);
        }
    }

    /**
     * The ring around the spot a worker was dropped on that the task lies in:
     * 0 on the spot itself, 1 for the eight tiles around it and so on, up to
     * {@link #DROP_AREA_RADIUS}. Any kind of work counts, walls and gold too.
     *
     * @param task the task
     * @param dropCenter the tile the worker was dropped on
     * @return the ring, {@link Integer#MAX_VALUE} if the task is further away
     */
    static int getDropRing(Task task, Point dropCenter) {
        Point tile = task.getTaskLocation();
        if (tile == null) {
            return Integer.MAX_VALUE;
        }
        int ring = Math.max(Math.abs(tile.x - dropCenter.x), Math.abs(tile.y - dropCenter.y));
        return ring <= DROP_AREA_RADIUS ? ring : Integer.MAX_VALUE;
    }

    /**
     * Whether there is more digging a worker can take. In the original's imp
     * idle state (DKII.EXE 0x463DF0) a digging imp that finds none takes any
     * gold it carries home first. Only digging it can reach counts, so this
     * stops at the first free dig task it can be assigned to.
     *
     * @param tasks the player's tasks
     * @param creature the worker
     * @return true if it can dig on
     */
    static boolean hasMoreDigging(Collection<Task> tasks, ICreatureController creature) {
        for (Task task : tasks) {
            if (isDigging(task) && !task.isFull() && task.canAssign(creature)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Digging, mining gold included
     *
     * @param task the task
     * @return true if the task digs
     */
    static boolean isDigging(Task task) {
        return task.getTaskType() == TaskType.DIG_TILE;
    }

    /**
     * Claiming, the floor and rooms alike, as the original's claiming job
     *
     * @param task the task
     * @return true if the task claims
     */
    static boolean isClaiming(Task task) {
        return task.getTaskType() == TaskType.CLAIM_TILE || task.getTaskType() == TaskType.CLAIM_ROOM;
    }

    /**
     * @param dropRing the ring around the spot this worker was dropped on
     * that the task lies in, see {@link #getDropRing(Task, Point)}
     * @param sameWork the worker is digging or claiming and the task is the
     * same kind of work
     */
    static record TaskWorkerPriority(Task task, ICreatureController creature, int points, int dropRing, boolean sameWork,
            Consumer<Boolean> workResult) {

    }

}
