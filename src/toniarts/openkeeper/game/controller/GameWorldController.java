/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.controller.room.storage.RoomGoldControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger.Level;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import toniarts.openkeeper.game.component.TileBuildOrSell;

/**
 * Game world controller, controls the game world related actions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameWorldController implements IGameWorldController, IPlayerActions {

    private static final System.Logger logger = System.getLogger(GameWorldController.class.getName());

    /**
     * When dealing with gold... We currently better lock it. Logic stuff happens in single thread. But player
     * actions are prosessed in real time by possibly other threads. We must not lose any gold from the world.
     */
    public static final Object GOLD_LOCK = new Object();

    private final KwdFile kwdFile;
    private final EntityData entityData;
    private IObjectsController objectsController;
    private ICreaturesController creaturesController;
    private IDoorsController doorsController;
    private ITrapsController trapsController;
    private IShotsController shotsController;
    private IEntityPositionLookup entityPositionLookup;
    private final Map<Short, IPlayerController> playerControllers;
    private final Map<Short, Keeper> players;
    private final IGameTimer gameTimer;

    private IMapController mapController;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final SafeArrayList<PlayerActionListener> listeners = new SafeArrayList<>(PlayerActionListener.class);

    public GameWorldController(IGameController gameController, ILevelInfo levelInfo, EntityData entityData,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            Map<Short, IPlayerController> playerControllers, IGameTimer gameTimer) {

        this.kwdFile = levelInfo.getLevelData();
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.playerControllers = playerControllers;
        this.players = levelInfo.getPlayers();

        this.createNewGame(gameController, levelInfo);
    }

    private void createNewGame(IGameController gameController, ILevelInfo levelInfo) {

        // Load objects
        objectsController = new ObjectsController(kwdFile, entityData, gameSettings, gameTimer, gameController, levelInfo);

        // Load the map
        mapController = new MapController(kwdFile, objectsController, gameSettings, gameTimer, entityData, levelInfo);

        // Load creatures
        creaturesController = new CreaturesController(kwdFile, entityData, gameSettings, gameTimer, gameController, mapController, levelInfo);

        // Load the doors
        doorsController = new DoorsController(kwdFile, entityData, gameSettings, mapController, gameController, levelInfo);

        // Load the traps
        trapsController = new TrapsController(kwdFile, entityData, gameSettings, gameController, levelInfo);

        // Init handlers
        shotsController = new ShotsController(kwdFile, entityData, gameSettings, gameTimer, gameController, mapController, levelInfo, objectsController, creaturesController);

        // Setup player stuff
        initPlayerMoney();
        initPlayerRooms();
    }

    private void initPlayerMoney() {

        // The max money$$$
        for (IRoomController room : mapController.getRoomControllers()) {
            if (room.canStoreGold()) {
                IPlayerController playerController = playerControllers.get(room.getRoomInstance().getOwnerId());
                if (playerController != null) {
                    playerController.getGoldControl().setGoldMax(playerController.getGoldControl().getGoldMax() + room.getObjectControl(ObjectType.GOLD).getMaxCapacity());
                }
            }
        }

        // Set up the money$$$
        for (Keeper keeper : players.values()) {
            Player player = kwdFile.getPlayer(keeper.getId());
            if (player.getStartingGold() > 0) {
                addGold(keeper.getId(), player.getStartingGold());
            }
        }
    }

    private void initPlayerRooms() {

        // Add the initial creatures and add the listeners
        Map<Short, List<IRoomController>> playerRooms = mapController.getRoomControllers().stream().collect(Collectors.groupingBy(entry -> entry.getRoomInstance().getOwnerId()));
        for (Keeper player : players.values()) {
            List<IRoomController> rooms = playerRooms.get(player.getId());
            IPlayerController playerController = playerControllers.get(player.getId());
            if (rooms != null) {
                playerController.getRoomControl().init(rooms);
            }

            // Add the listener
            mapController.addListener(player.getId(), playerController.getRoomControl());
        }
    }

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available rooms
     *
     * @param playerId for the player
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    @Override
    public int addGold(short playerId, int sum) {
        synchronized (GOLD_LOCK) {
            return addGold(playerId, null, sum);
        }
    }

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available rooms
     *
     * @param playerId for the player
     * @param p a point where to drop the gold, can be {@code  null}
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    @Override
    public int addGold(short playerId, Point p, int sum) {

        synchronized (GOLD_LOCK) {

            // Gold to specified point/room
            int moneyLeft = sum;
            if (p != null) {

                // Get a room in point
                IRoomController room = mapController.getRoomControllerByCoordinates(p);
                if (room != null && room.canStoreGold()) {
                    RoomGoldControl control = room.getObjectControl(ObjectType.GOLD);
                    moneyLeft = control.addItem(sum, p);
                }
            } else {

                // Distribute the gold
                for (IRoomController roomController : mapController.getRoomControllers()) {
                    if (roomController.getRoomInstance().getOwnerId() == playerId && roomController.canStoreGold()) {
                        RoomGoldControl control = roomController.getObjectControl(ObjectType.GOLD);
                        moneyLeft = control.addItem(sum, p);
                        if (moneyLeft == 0) {
                            break;
                        }
                    }
                }
            }

            // Add to the player
            playerControllers.get(playerId).getGoldControl().addGold(sum - moneyLeft);

            return moneyLeft;
        }
    }

    /**
     * Substract gold from player
     *
     * @param amount the amount to try to substract
     * @param playerId the player id
     * @return amount of money that could not be substracted from the player
     */
    @Override
    public int substractGold(int amount, short playerId) {

        synchronized (GOLD_LOCK) {

            // See if the player has any gold even
            Keeper keeper = players.get(playerId);
            if (keeper.getGold() == 0) {
                return amount;
            }

            // The gold is subtracted evenly from all treasuries
            int moneyToSubstract = amount;
            List<IRoomController> playersTreasuries = mapController.getRoomsByFunction(ObjectType.GOLD, playerId);
            while (moneyToSubstract > 0 && !playersTreasuries.isEmpty()) {
                Iterator<IRoomController> iter = playersTreasuries.iterator();
                int goldToRemove = (int) Math.ceil((float) moneyToSubstract / playersTreasuries.size());
                while (iter.hasNext()) {
                    IRoomController room = iter.next();
                    RoomGoldControl control = room.getObjectControl(ObjectType.GOLD);
                    goldToRemove = Math.min(moneyToSubstract, goldToRemove); // Rounding...
                    moneyToSubstract -= goldToRemove - control.removeGold(goldToRemove);
                    if (control.getCurrentCapacity() == 0) {
                        iter.remove();
                    }
                    if (moneyToSubstract == 0) {
                        break;
                    }
                }
            }

            // Substract from the player
            playerControllers.get(playerId).getGoldControl().subGold(amount - moneyToSubstract);

            return moneyToSubstract;
        }
    }

    private void substractGoldCapacityFromPlayer(EntityId instance) {
        synchronized (GOLD_LOCK) {
            IRoomController roomController = mapController.getRoomController(instance);
            if (roomController.canStoreGold()) {
                RoomGoldControl roomGoldControl = roomController.getObjectControl(ObjectType.GOLD);
                PlayerGoldControl playerGoldControl = playerControllers.get(roomController.getOwnerId()).getGoldControl();
                playerGoldControl.setGoldMax(playerGoldControl.getGoldMax() - roomGoldControl.getMaxCapacity());
            }
        }
    }

    private void addGoldCapacityToPlayer(EntityId instance) {
        synchronized (GOLD_LOCK) {
            IRoomController roomController = mapController.getRoomController(instance);
            if (roomController.canStoreGold()) {
                RoomGoldControl roomGoldControl = roomController.getObjectControl(ObjectType.GOLD);
                PlayerGoldControl playerGoldControl = playerControllers.get(roomController.getOwnerId()).getGoldControl();
                playerGoldControl.setGoldMax(playerGoldControl.getGoldMax() + roomGoldControl.getMaxCapacity());
            }
        }
    }

    @Override
    public void build(Set<Point> points, short playerId, short roomId) {
        List<Point> instancePlots = new ArrayList<>();

        for (Point p : points) {
            // See that is this valid
            if (mapController.isBuildable(p, playerId, roomId)) {
                instancePlots.add(p);
            }
        }

        // If no plots, no building
        if (instancePlots.isEmpty()) {
            return;
        }

        Room room = kwdFile.getRoomById(roomId);
        if ((room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER))
                || room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA)) {
//            instancePlots = new ArrayList<>(mapController.getTerrainBatches(instancePlots, x1, x2, y1, y2));
        }

        synchronized (GOLD_LOCK) {
            int cost = instancePlots.size() * room.getCost();
            if (instancePlots.size() * room.getCost() > players.get(playerId).getGold()) {
                return;
            }
            substractGold(cost, playerId);
        }

        // Build & mark
        List<Point> buildTiles = new ArrayList<>(instancePlots.size());
        Set<Point> updatableTiles = new HashSet<>();
        Set<Point> buildPlots = new HashSet<>();
        for (Point p : instancePlots) {

            buildPlots.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, false)));
            updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));

            IMapTileController tile = mapController.getMapData().getTile(p);
            tile.setOwnerId(playerId);
            tile.setTerrainId(room.getTerrainId());
            buildTiles.add(tile.getLocation());
        }

        // See if we hit any of the adjacent rooms
        Set<EntityId> adjacentInstances = new LinkedHashSet<>();
        for (Point p : buildPlots) {
            IRoomController adjacentInstance = mapController.getRoomControllerByCoordinates(p);
            if (adjacentInstance == null || !adjacentInstance.getRoom().equals(room) || adjacentInstances.contains(adjacentInstance.getEntityId())) {
                continue;
            }

            // Same room, see that we own it
            IMapTileController tile = mapController.getMapData().getTile(p.x, p.y);
            if (tile.getOwnerId() == playerId) {

                // Bingo!
                adjacentInstances.add(adjacentInstance.getEntityId());
            }
        }

        // If any hits, merge to the first one, and update whole room
        if (!adjacentInstances.isEmpty()) {

            // Add the mergeable rooms to updatable tiles as well
            EntityId firstInstance = null;
            for (EntityId instance : adjacentInstances) {

                // Merge to the first found room instance
                if (firstInstance == null) {
                    firstInstance = instance;
                    substractGoldCapacityFromPlayer(firstInstance); // Important to update the gold here
                    mapController.addRoomCoordinate(firstInstance, instancePlots);
                }

                for (Point p : mapController.getRoomController(instance).getRoomInstance().getCoordinates()) {
                    updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
                    if (!firstInstance.equals(instance)) {
                        mapController.addRoomCoordinate(firstInstance, Collections.singletonList(p));
                    }
                }

                // Remove the merged room
                if (!firstInstance.equals(instance)) {
                    removeRoomInstance(instance);
                    mapController.removeRoomInstances(instance);
                }
            }
            // TODO: The room health! We need to make sure that the health is distributed evenly

            // Update the merged room
            mapController.getRoomController(firstInstance).construct();
            addGoldCapacityToPlayer(firstInstance);
        }

        // Update
        mapController.updateRooms(updatableTiles.toArray(Point[]::new));

        // New room, calculate gold capacity
        IRoomController instance = mapController.getRoomControllerByCoordinates(instancePlots.getFirst());
        if (adjacentInstances.isEmpty()) {
            addGoldCapacityToPlayer(instance.getEntityId());
            //notifyOnBuild(instance.getOwnerId(), mapController.getRoomActuals().get(instance));
        }

        // Notify the build
        notifyOnBuild(playerId, buildTiles);
    }

    @Override
    public void build(Vector2f start, Vector2f end, short playerId, short roomId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new TileBuildOrSell(start, end, playerId, roomId));
    }

    @Override
    public void sell(Set<Point> points, short playerId) {
        List<Point> soldTiles = new ArrayList<>();
        Set<Point> updatableTiles = new HashSet<>();
        Set<EntityId> soldInstances = new HashSet<>();
        List<Point> roomCoordinates = new ArrayList<>();
        List<Map.Entry<Point, Integer>> moneyToReturnByPoint = new ArrayList<>();
        for (Point p : points) {
            // See that is this valid
            if (!mapController.isSellable(p, playerId)) {
                continue;
            }

            // Sell
            IMapTileController tile = mapController.getMapData().getTile(p);
            if (tile == null) {
                continue;
            }
            soldTiles.add(tile.getLocation());

            Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
                if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
                    tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
                } else // Water or lava
                if (tile.getBridgeTerrainType() == Tile.BridgeTerrainType.LAVA) {
                    tile.setTerrainId(kwdFile.getMap().getLava().getTerrainId());
                } else {
                    tile.setTerrainId(kwdFile.getMap().getWater().getTerrainId());
                }

                // Money back
                moneyToReturnByPoint.add(new AbstractMap.SimpleImmutableEntry<>(p, (int) (room.getCost() * (gameSettings.get(Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST).getValue() / 100))));
            }

            // Get the instance
            soldInstances.add(tile.getRoomId());
            updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
        }

        // See if we did anything at all
        if (soldTiles.isEmpty()) {
            return;
        }

        // Remove the sold instances (will be regenerated) and add them to updatable
        for (EntityId roomInstance : soldInstances) {
            IRoomController roomController = mapController.getRoomController(roomInstance);
            for (Point p : roomController.getRoomInstance().getCoordinates()) {
                updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
            }
            roomCoordinates.addAll(roomController.getRoomInstance().getCoordinates());
            removeRoomInstance(roomInstance);
        }
        mapController.removeRoomInstances(soldInstances.toArray(new EntityId[0]));

        // Update
        mapController.updateRooms(updatableTiles.toArray(new Point[0]));

        // See if any of the rooms survived
        Set<EntityId> newInstances = new HashSet<>();
        for (Point p : roomCoordinates) {
            EntityId instance = mapController.getMapData().getTile(p).getRoomId();
            if (instance != null && !newInstances.contains(instance)) {
                newInstances.add(instance);
                addGoldCapacityToPlayer(instance);
            }
        }

        // Finally we have all the rooms and such, return the revenue to the player
        // Do it this point to avoid placing the profit to the actual room we were selling
        synchronized (GOLD_LOCK) {
            for (Map.Entry<Point, Integer> moneyToReturn : moneyToReturnByPoint) {
                int goldLeft = addGold(playerId, moneyToReturn.getValue());
                if (goldLeft > 0) {

                    // Add loose gold to this tile
                    objectsController.addLooseGold(playerId, moneyToReturn.getKey().x, moneyToReturn.getKey().y, goldLeft, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue());
                }
            }
        }

        // Notify
        notifyOnSold(playerId, soldTiles);
    }

    @Override
    public void sell(Vector2f start, Vector2f end, short playerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new TileBuildOrSell(start, end, playerId));
    }

    /**
     * Mainly deals with gold removal when room is removed
     *
     * @param roomInstance the room to delete
     */
    private void removeRoomInstance(EntityId roomInstance) {

        // Gold
        substractGoldCapacityFromPlayer(roomInstance);
        IRoomController roomController = mapController.getRoomController(roomInstance);
        if (roomController.canStoreGold()) {
            RoomGoldControl roomGoldControl = roomController.getObjectControl(ObjectType.GOLD);
            PlayerGoldControl playerGoldControl = playerControllers.get(roomController.getOwnerId()).getGoldControl();
            playerGoldControl.subGold(roomGoldControl.getCurrentCapacity());
        }
    }

    @Override
    public void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
        mapController.selectTiles(start, end, select, playerId);
    }

    @Override
    public IMapController getMapController() {
        return mapController;
    }

    /**
     * If you want to get notified about player actions
     *
     * @param listener the listener
     */
    @Override
    public void addListener(PlayerActionListener listener) {
        listeners.add(listener);
    }

    /**
     * Stop listening to player actions
     *
     * @param listener the listener
     */
    @Override
    public void removeListener(PlayerActionListener listener) {
        listeners.remove(listener);
    }

    private void notifyOnBuild(short playerId, List<Point> buildTiles) {
        for (PlayerActionListener listener : listeners.getArray()) {
            listener.onBuild(playerId, buildTiles);
        }
    }

    private void notifyOnSold(short playerId, List<Point> soldTiles) {
        for (PlayerActionListener listener : listeners.getArray()) {
            listener.onSold(playerId, soldTiles);
        }
    }

    @Override
    public void pickUp(EntityId entity, short playerId) {
        PlayerHandControl playerHandControl = playerControllers.get(playerId).getHandControl();
        if (!playerHandControl.isFull() && canPickUpEntity(entity, playerId, entityData)) {
            putToKeeperHand(playerHandControl, entity, playerId);
        }
    }

    private void putToKeeperHand(PlayerHandControl playerHandControl, EntityId entity, short playerId) {
        playerHandControl.push(entity);

        /**
         * TODO: Basically here we can have a concurrency problem, especially visible with things that have
         * AI. The AI or other systems may be still processing and manipulating stuff. One way to fix would be
         * to add this to game loop queue to be executed. That would introduce a delay though...
         */
        // Lose the position component on the entity, do it here since we have the knowledge on locations etc. keep the "hand" simple
        // And also no need to create a system for this which saves resources
        Position position = entityData.getComponent(entity, Position.class);
        entityData.removeComponent(entity, Navigation.class);
        entityData.removeComponent(entity, Position.class);
        entityData.removeComponent(entity, CreatureAi.class);
        entityData.removeComponent(entity, TaskComponent.class);
        entityData.removeComponent(entity, CreatureRecuperating.class);
        entityData.removeComponent(entity, CreatureTortured.class);
        entityData.removeComponent(entity, CreatureImprisoned.class);
        entityData.removeComponent(entity, AttackTarget.class);
        entityData.removeComponent(entity, FollowTarget.class);
        entityData.removeComponent(entity, HauledBy.class);

        // TODO: Should we some sort of room component and notify the room handlers instead?
        // Handle stored stuff
        Stored roomStorage = entityData.getComponent(entity, Stored.class);
        IRoomController roomController = mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(position.position));
        if (roomController != null && roomStorage != null) {
            IRoomObjectControl roomObjectControl = roomController.getObjectControl(roomStorage.objectType);
            roomObjectControl.removeItem(entity);

            // If it was gold... substract it from the player
            if (roomStorage.objectType == ObjectType.GOLD) {
                synchronized (GOLD_LOCK) {
                    playerControllers.get(playerId).getGoldControl().subGold(entityData.getComponent(entity, Gold.class).gold);
                }
            }
        }
    }

    private static boolean canPickUpEntity(EntityId entityId, short playerId, EntityData entityData) {
        // TODO: Somewhere common shared static, can share the rules with the UI client

        // Check if picked up already
        InHand inHand = entityData.getComponent(entityId, InHand.class);
        if (inHand != null) {
            return false;
        }

        Owner owner = entityData.getComponent(entityId, Owner.class);
        if (owner == null || owner.controlId != playerId) {
            return false;
        }

        // Is it iteractable?
        Interaction interaction = entityData.getComponent(entityId, Interaction.class);
        if (interaction == null) {
            return false;
        }

        // TODO: Was it so that it can be only picked up from own land?
        return interaction.pickUppable && !isEntityIncapacitated(entityId, entityData);
    }

    @Override
    public void interact(EntityId entity, short playerId) {
        if (!canInteract(entity, playerId, entityData)) {
            return;
        }

        // Doors
        DoorComponent doorComponent = entityData.getComponent(entity, DoorComponent.class);
        if (doorComponent != null && !doorComponent.blueprint) {
            entityData.setComponent(entity, new DoorComponent(doorComponent.doorId, !doorComponent.locked, doorComponent.blueprint));
            DoorViewState doorViewState = entityData.getComponent(entity, DoorViewState.class);
            if (doorViewState != null) {
                entityData.setComponent(entity, new DoorViewState(doorViewState.doorId, !doorComponent.locked, doorViewState.blueprint, !doorComponent.locked ? false : doorViewState.open));
            }

            return;
        }

        Interaction interaction = entityData.getComponent(entity, Interaction.class);

        // Creatures (slapping)
        // TODO: Slap limit
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);
        if (creatureComponent != null && interaction.slappable) {
            entityData.setComponent(entity, new Slapped(gameTimer.getGameTime()));
            return;
        }

        // Objects
        ObjectComponent objectComponent = entityData.getComponent(entity, ObjectComponent.class);
        if (objectComponent != null && interaction.slappable) {
            entityData.setComponent(entity, new Slapped(gameTimer.getGameTime()));
            return;
        }
    }

    @Override
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity, short playerId) {
        PlayerHandControl playerHandControl = playerControllers.get(playerId).getHandControl();
        if (playerHandControl.peek().equals(entity) && canDropEntity(entity, playerId, entityData, mapController.getMapData().getTile(tile))) {
            playerHandControl.pop();

            // Stuff drop differently
            IRoomController roomController = mapController.getRoomControllerByCoordinates(tile);
            if (entityData.getComponent(entity, CreatureComponent.class) != null) {
                dropCreature(roomController, entity, tile, coordinates, playerId);
            } else {

                // TODO: handle giving item to creature
                // See if it is gold added to a room
                ObjectComponent objectComponent = entityData.getComponent(entity, ObjectComponent.class);
                if (objectComponent != null && kwdFile.getObject(objectComponent.objectId).getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
                    dropGold(entity, roomController, playerId, tile, coordinates);
                } else {
                    Vector3f pos = new Vector3f(coordinates.x, 1, coordinates.y);
                    entityData.setComponent(entity, new Position(0, pos));
                }
            }
        }
    }

    private void dropCreature(IRoomController roomController, EntityId entity, Point tile, Vector2f coordinates, short playerId) {
        boolean tortureOrImprisonment = false;
        boolean torture = false;
        boolean imprison = false;
        CreatureState creatureState = null;

        // TODO: Evict & sacrifice
        // TODO: Duplicated code with CreaturesController
        // TODO: capacities
        if (roomController != null) {
            Owner owner = entityData.getComponent(entity, Owner.class);
            if (roomController.getRoomInstance().getOwnerId() != owner.ownerId) {
                if (roomController.hasObjectControl(AbstractRoomController.ObjectType.PRISONER)) {
                    roomController.getObjectControl(AbstractRoomController.ObjectType.PRISONER).addItem(entity, tile);
                    creatureState = CreatureState.IMPRISONED;
                    imprison = true;

                    // Set the component, continue the jail time if such is possible
                    CreatureImprisoned imprisoned = entityData.getComponent(entity, CreatureImprisoned.class);
                    entityData.setComponent(entity, new CreatureImprisoned(imprisoned != null ? imprisoned.startTime : gameTimer.getGameTime(), gameTimer.getGameTime()));
                }
                if (roomController.hasObjectControl(AbstractRoomController.ObjectType.TORTUREE)) {

                    // TODO: you need to exactly drop the creature to a torture device, otherwise you just set him free in your base
                    roomController.getObjectControl(AbstractRoomController.ObjectType.TORTUREE).addItem(entity, tile);
                    creatureState = CreatureState.TORTURED;
                    torture = true;

                    // Set the component, continue the torturing time if such is possible
                    CreatureTortured tortured = entityData.getComponent(entity, CreatureTortured.class);
                    entityData.setComponent(entity, new CreatureTortured(tortured != null ? tortured.timeTortured : 0.0d, gameTimer.getGameTime(), gameTimer.getGameTime()));
                }
            }
            tortureOrImprisonment = imprison || torture;
        }

        // Add position
        // Maybe in the future the physics deal with this and we just need to detect that we are airborne
        Vector3f pos = new Vector3f(coordinates.x, (tortureOrImprisonment ? WorldUtils.FLOOR_HEIGHT : WorldUtils.DROP_HEIGHT), coordinates.y);
        entityData.setComponent(entity, new Position(0, pos));

        // If we got state, add it
        if (creatureState != null) {
            CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);
            entityData.setComponent(entity, new CreatureAi(gameTimer.getGameTime(), creatureState, creatureComponent.creatureId));
        }

        // Add the dropping component to the creature
        if (!tortureOrImprisonment) {
            entityData.setComponent(entity, new CreatureFall());
        }

        // Also get rid of possible jailed or tortured status, we don't immediately remove them on picking up since the creature might be just displaced to another jail/torture room
        if (imprison) {
            entityData.removeComponent(entity, CreatureTortured.class);
        }
        if (torture) {
            entityData.removeComponent(entity, CreatureImprisoned.class);
        }

        // Set the control
        Owner owner = entityData.getComponent(entity, Owner.class);
        if (tortureOrImprisonment) {
            entityData.setComponent(entity, new Owner(owner.ownerId, playerId));
        } else {
            entityData.setComponent(entity, new Owner(owner.ownerId, owner.ownerId));
        }
    }

    private void dropGold(EntityId entity, IRoomController roomController, short playerId, Point tile, Vector2f coordinates) {
        Gold gold = entityData.getComponent(entity, Gold.class);
        if (roomController != null && roomController.canStoreGold()) {
            int leftOverGold = addGold(playerId, tile, gold.gold);

            // TODO: if I remember correctly... DK II drops left overs as loose gold next to the treasury
            if (leftOverGold > 0) {
                objectsController.addLooseGold(playerId, tile.x, tile.y, leftOverGold, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue());
            }
        } else {
            EntityId newGold = objectsController.addLooseGold(playerId, tile.x, tile.y, gold.gold, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue());
            Vector3f pos = new Vector3f(coordinates.x, 1, coordinates.y);
            entityData.setComponent(newGold, new Position(0, pos));
        }
        entityData.removeEntity(entity);
    }

    private static boolean canDropEntity(EntityId entityId, short playerId, EntityData entityData, IMapTileInformation tile) {
        // TODO: Somewhere common shared static, can share the rules with the UI client

        if (tile == null) {
            return false;
        }

        // Check if picked up in the first place
        InHand inHand = entityData.getComponent(entityId, InHand.class);
        if (inHand == null) {
            return false;
        }

        // Is it iteractable?
        Interaction interaction = entityData.getComponent(entityId, Interaction.class);
        if (interaction == null) {
            return false;
        }

        // Own land rule
        if (!interaction.canBeDroppedOnAnyLand && tile.getOwnerId() != playerId) {
            return false;
        }

        return true;
    }

    @Override
    public void getGold(int amount, short playerId) {
        int maxGold = (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue();
        int realAmount = Math.min(amount, maxGold);
        PlayerHandControl playerHandControl = playerControllers.get(playerId).getHandControl();
        if (!playerHandControl.isFull()) {
            synchronized (GOLD_LOCK) {
                int leftOverRequest = substractGold(realAmount, playerId);
                realAmount -= leftOverRequest;
                if (realAmount > 0) {
                    EntityId goldEntity = objectsController.addLooseGold(playerId, 0, 0, realAmount, maxGold);
                    putToKeeperHand(playerHandControl, goldEntity, playerId);
                }
            }
        }
    }

    private static boolean canInteract(EntityId entityId, short playerId, EntityData entityData) {

        // The owner only
        Owner owner = entityData.getComponent(entityId, Owner.class);
        if (owner == null || owner.ownerId != playerId) {
            return false;
        }

        // Is it interactable?
        Interaction interaction = entityData.getComponent(entityId, Interaction.class);
        if (interaction == null) {
            return false;
        }

        return !isEntityIncapacitated(entityId, entityData);
    }

    private static boolean isEntityIncapacitated(EntityId entityId, EntityData entityData) {
        Health health = entityData.getComponent(entityId, Health.class);
        if (health == null || entityData.getComponent(entityId, Unconscious.class) != null) {
            return true;
        }

        return false;
    }

    @Override
    public void castKeeperSpell(short keeperSpellId, EntityId target, Point tile, Vector2f position, short playerId) {
        KeeperSpell keeperSpell = kwdFile.getKeeperSpellById(keeperSpellId);
        if (keeperSpell == null) {
            logger.log(Level.WARNING, "Invalid spell ID for spell casting received, was: {0}", keeperSpellId);
            return;
        }

        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        if (target != null) {
            mapTile = entityPositionLookup.getEntityLocation(target);
        }
        if (mapTile == null) {
            logger.log(Level.WARNING, "Invalid map location for spell casting received, was: {0}", tile);
            return;
        }

        Keeper player = players.get(playerId);
        if (player == null) {
            logger.log(Level.WARNING, "Invalid player for spell casting received, was: {0}", playerId);
            return;
        }

        ResearchableEntity researchableEntity = playerControllers.get(playerId).getSpellControl().getTypes().get(keeperSpell);
        if (researchableEntity == null || !researchableEntity.isDiscovered()) {
            logger.log(Level.WARNING, "Player spell is not available", playerId);
            return;
        }

        // Validate
        if (!KeeperSpellCastValidator.isValidCast(keeperSpell, kwdFile, mapController, mapTile, player, entityData, target)) {
            return;
        }

        // Deduct the mana
        playerControllers.get(playerId).getManaControl().addMana(-keeperSpell.getManaCost());

        // Cast the spell
        boolean spellUpgraded = researchableEntity.isUpgraded();
        int shotData1 = spellUpgraded ? keeperSpell.getBonusShotData1() : keeperSpell.getShotData1();
        int shotData2 = spellUpgraded ? keeperSpell.getBonusShotData2() : keeperSpell.getShotData2();
        shotsController.createShot(keeperSpell.getShotTypeId(), shotData1, shotData2, playerId, WorldUtils.vector2fToVector3(position), target);
    }

    @Override
    public void placeDoor(short doorId, Point tile, short playerId) {
        Door door = kwdFile.getDoorById(doorId);
        if (door == null) {
            logger.log(Level.WARNING, "Invalid door ID for door placement received, was: {0}", door);
            return;
        }

        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        if (mapTile == null) {
            logger.log(Level.WARNING, "Invalid map location for door placement received, was: {0}", tile);
            return;
        }

        Keeper player = players.get(playerId);
        if (player == null) {
            logger.log(Level.WARNING, "Invalid player for door placement received, was: {0}", playerId);
            return;
        }

        // Validate
    }

    @Override
    public void placeTrap(short trapId, Point tile, short playerId) {
        Trap trap = kwdFile.getTrapById(trapId);
        if (trap == null) {
            logger.log(Level.WARNING, "Invalid trap ID for trap placement received, was: {0}", trap);
            return;
        }

        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        if (mapTile == null) {
            logger.log(Level.WARNING, "Invalid map location for trap placement received, was: {0}", tile);
            return;
        }

        Keeper player = players.get(playerId);
        if (player == null) {
            logger.log(Level.WARNING, "Invalid player for trap placement received, was: {0}", playerId);
            return;
        }

        // Validate
    }

    @Override
    public ICreaturesController getCreaturesController() {
        return creaturesController;
    }

    public IDoorsController getDoorsController() {
        return doorsController;
    }

    @Override
    public IObjectsController getObjectsController() {
        return objectsController;
    }

    public ITrapsController getTrapsController() {
        return trapsController;
    }

    @Override
    public IShotsController getShotsController() {
        return shotsController;
    }

    public void setEntityPositionLookup(IEntityPositionLookup entityPositionLookup) {
        this.entityPositionLookup = entityPositionLookup;
    }
}
