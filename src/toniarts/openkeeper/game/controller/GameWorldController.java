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
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.awt.Point;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomGoldControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Game world controller, controls the game world related actions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameWorldController implements IGameWorldController, IPlayerActions {

    public final Object goldLock = new Object();
    private final KwdFile kwdFile;
    private final EntityData entityData;
    private IObjectsController objectsController;
    private ICreaturesController creaturesController;
    private IDoorsController doorsController;
    private ITrapsController trapsController;
    private final Map<Short, IPlayerController> playerControllers;
    private final SortedMap<Short, Keeper> players;
    private final IGameTimer gameTimer;

    private IMapController mapController;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final SafeArrayList<PlayerActionListener> listeners = new SafeArrayList<>(PlayerActionListener.class);

    public GameWorldController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, SortedMap<Short, Keeper> players, Map<Short, IPlayerController> playerControllers, IGameTimer gameTimer) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.playerControllers = playerControllers;
        this.players = players;
    }

    public void createNewGame(IGameController gameController) {

        // Load objects
        objectsController = new ObjectsController(kwdFile, entityData, gameSettings);

        // Load creatures
        creaturesController = new CreaturesController(kwdFile, entityData, gameSettings, gameTimer, gameController);

        // Load the map
        mapController = new MapController(kwdFile, objectsController, gameSettings);

        // Load the doors
        doorsController = new DoorsController(kwdFile, entityData, gameSettings, mapController);

        // Load the traps
        trapsController = new TrapsController(kwdFile, entityData, gameSettings);

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
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    @Override
    public int addGold(short playerId, int sum) {
        synchronized (goldLock) {
            return addGold(playerId, null, sum);
        }
    }

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param p a point where to drop the gold, can be {@code  null}
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    @Override
    public int addGold(short playerId, Point p, int sum) {

        synchronized (goldLock) {

            // Gold to specified point/room
            int moneyLeft = sum;
            if (p != null) {

                // Get a room in point
                RoomInstance roomInstance = mapController.getRoomInstanceByCoordinates(p);
                if (roomInstance != null) {
                    IRoomController room = mapController.getRoomController(roomInstance);
                    if (room.canStoreGold()) {
                        RoomGoldControl control = room.getObjectControl(ObjectType.GOLD);
                        moneyLeft = control.addItem(sum, p);
                    }
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

        synchronized (goldLock) {

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

    private void substractGoldCapacityFromPlayer(RoomInstance instance) {
        synchronized (goldLock) {
            IRoomController roomController = mapController.getRoomController(instance);
            if (roomController.canStoreGold()) {
                RoomGoldControl roomGoldControl = roomController.getObjectControl(ObjectType.GOLD);
                PlayerGoldControl playerGoldControl = playerControllers.get(instance.getOwnerId()).getGoldControl();
                playerGoldControl.setGoldMax(playerGoldControl.getGoldMax() - roomGoldControl.getMaxCapacity());
            }
        }
    }

    private void addGoldCapacityToPlayer(RoomInstance instance) {
        synchronized (goldLock) {
            IRoomController roomController = mapController.getRoomController(instance);
            if (roomController.canStoreGold()) {
                RoomGoldControl roomGoldControl = roomController.getObjectControl(ObjectType.GOLD);
                PlayerGoldControl playerGoldControl = playerControllers.get(instance.getOwnerId()).getGoldControl();
                playerGoldControl.setGoldMax(playerGoldControl.getGoldMax() + roomGoldControl.getMaxCapacity());
            }
        }
    }

    @Override
    public void build(Vector2f start, Vector2f end, short playerId, short roomId) {
        Set<Point> updatableTiles = new HashSet<>();
        Set<Point> buildPlots = new HashSet<>();
        List<Point> instancePlots = new ArrayList<>();
        for (int x = (int) Math.max(0, start.x); x < Math.min(kwdFile.getMap().getWidth(), end.x + 1); x++) {
            for (int y = (int) Math.max(0, start.y); y < Math.min(kwdFile.getMap().getHeight(), end.y + 1); y++) {

                // See that is this valid
                if (!mapController.isBuildable(x, y, playerId, roomId)) {
                    continue;
                }

                Point p = new Point(x, y);
                instancePlots.add(p);
                buildPlots.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, false)));
                updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
            }
        }

        // See that can we afford the building
        Room room = kwdFile.getRoomById(roomId);
        int cost = instancePlots.size() * room.getCost();
        if (instancePlots.size() * room.getCost() > players.get(playerId).getGold()) {
            return;
        }
        substractGold(cost, playerId);

        // Build
        List<MapTile> buildTiles = new ArrayList<>(instancePlots.size());
        for (Point p : instancePlots) {
            MapTile tile = mapController.getMapData().getTile(p);
            tile.setOwnerId(playerId);
            tile.setTerrainId(room.getTerrainId());
            buildTiles.add(tile);
        }

        // See if we hit any of the adjacent rooms
        Set<RoomInstance> adjacentInstances = new LinkedHashSet<>();
        for (Point p : buildPlots) {
            RoomInstance adjacentInstance = mapController.getRoomInstanceByCoordinates(p);
            if (adjacentInstance != null && adjacentInstance.getRoom().equals(room) && !adjacentInstances.contains(adjacentInstance)) {

                // Same room, see that we own it
                MapTile tile = mapController.getMapData().getTile(p.x, p.y);
                if (tile.getOwnerId() == playerId) {

                    // Bingo!
                    adjacentInstances.add(adjacentInstance);
                }
            }
        }

        // If any hits, merge to the first one, and update whole room
        if (!adjacentInstances.isEmpty()) {

            // Add the mergeable rooms to updatable tiles as well
            RoomInstance firstInstance = null;
            for (RoomInstance instance : adjacentInstances) {

                // Merge to the first found room instance
                if (firstInstance == null) {
                    firstInstance = instance;
                    substractGoldCapacityFromPlayer(firstInstance); // Important to update the gold here
                    firstInstance.addCoordinates(instancePlots);
                    for (Point p : instancePlots) {
                        mapController.getRoomCoordinates().put(p, firstInstance);
                    }

                    // Update the merged room
                    mapController.getRoomController(instance).construct();
                } else {
                    removeRoomInstance(instance);
                    mapController.removeRoomInstances(instance);
                }

                for (Point p : instance.getCoordinates()) {
                    updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
                    if (!firstInstance.equals(instance)) {
                        firstInstance.addCoordinate(p);
                        mapController.getRoomCoordinates().put(p, firstInstance);
                    }
                }
            }
            // TODO: The room health! We need to make sure that the health is distributed evenly
            addGoldCapacityToPlayer(firstInstance);
        }

        // Update
        mapController.updateRooms(updatableTiles.toArray(new Point[updatableTiles.size()]));

        // New room, calculate gold capacity
        RoomInstance instance = mapController.getRoomCoordinates().get(instancePlots.get(0));
        if (adjacentInstances.isEmpty()) {
            addGoldCapacityToPlayer(instance);
            //notifyOnBuild(instance.getOwnerId(), mapController.getRoomActuals().get(instance));
        }

        // Add any loose gold to the building
        attachLooseGoldToRoom(mapController.getRoomController(instance), instance);

        // Notify the build
        notifyOnBuild(playerId, buildTiles);
    }

    private void attachLooseGoldToRoom(IRoomController roomController, RoomInstance instance) {
        if (roomController.canStoreGold()) {
            synchronized (goldLock) {
                for (Entity entity : entityData.getEntities(Gold.class, Position.class, ObjectComponent.class)) {
                    // TODO: how do we detect loose gold and differentiate it from room gold?
                    // Room component probably needed, for now, use the object id
                    ObjectComponent objectComponent = entityData.getComponent(entity.getId(), ObjectComponent.class);
                    Position position = entityData.getComponent(entity.getId(), Position.class);
                    if (objectComponent.objectId == ObjectsController.OBJECT_GOLD_ID && instance.hasCoordinate(WorldUtils.vectorToPoint(position.position))) {
                        Gold gold = entityData.getComponent(entity.getId(), Gold.class);
                        int goldLeft = (int) roomController.getObjectControl(ObjectType.GOLD).addItem(gold.gold, WorldUtils.vectorToPoint(position.position));
                        playerControllers.get(instance.getOwnerId()).getGoldControl().addGold(gold.gold - goldLeft);
                        if (goldLeft == 0) {
                            entityData.removeEntity(entity.getId());
                        } else {
                            gold.gold = goldLeft;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void sell(Vector2f start, Vector2f end, short playerId) {
        List<MapTile> soldTiles = new ArrayList<>();
        Set<Point> updatableTiles = new HashSet<>();
        Set<RoomInstance> soldInstances = new HashSet<>();
        List<Point> roomCoordinates = new ArrayList<>();
        List<Map.Entry<Point, Integer>> moneyToReturnByPoint = new ArrayList<>();
        for (int x = (int) Math.max(0, start.x); x < Math.min(kwdFile.getMap().getWidth(), end.x + 1); x++) {
            for (int y = (int) Math.max(0, start.y); y < Math.min(kwdFile.getMap().getHeight(), end.y + 1); y++) {

                // See that is this valid
                if (!mapController.isSellable(x, y, playerId)) {
                    continue;
                }

                // Sell
                Point p = new Point(x, y);
                MapTile tile = mapController.getMapData().getTile(p);
                if (tile == null) {
                    continue;
                }
                soldTiles.add(tile);

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
                    moneyToReturnByPoint.add(new AbstractMap.SimpleImmutableEntry<Point, Integer>(p, (int) (room.getCost() * (gameSettings.get(Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST).getValue() / 100))));
                }

                // Get the instance
                soldInstances.add(mapController.getRoomCoordinates().get(p));
                updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
            }
        }

        // See if we did anything at all
        if (soldTiles.isEmpty()) {
            return;
        }

        // Remove the sold instances (will be regenerated) and add them to updatable
        for (RoomInstance roomInstance : soldInstances) {
            for (Point p : roomInstance.getCoordinates()) {
                updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
            }
            roomCoordinates.addAll(roomInstance.getCoordinates());
            removeRoomInstance(roomInstance);
        }
        mapController.removeRoomInstances(soldInstances.toArray(new RoomInstance[soldInstances.size()]));

        // Update
        mapController.updateRooms(updatableTiles.toArray(new Point[updatableTiles.size()]));

        // See if any of the rooms survived
        Set<RoomInstance> newInstances = new HashSet<>();
        for (Point p : roomCoordinates) {
            RoomInstance instance = mapController.getRoomCoordinates().get(p);
            if (instance != null && !newInstances.contains(instance)) {
                newInstances.add(instance);
                addGoldCapacityToPlayer(instance);
                attachLooseGoldToRoom(mapController.getRoomController(instance), instance);
            }
        }

        // Finally we have all the rooms and such, return the revenue to the player
        // Do it this point to avoid placing the profit to the actual room we were selling
        synchronized (goldLock) {
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

    /**
     * Mainly deals with gold removal when room is removed
     *
     * @param roomInstance the room to delete
     */
    private void removeRoomInstance(RoomInstance roomInstance) {

        // Gold
        substractGoldCapacityFromPlayer(roomInstance);
        IRoomController roomController = mapController.getRoomController(roomInstance);
        if (roomController.canStoreGold()) {
            RoomGoldControl roomGoldControl = roomController.getObjectControl(ObjectType.GOLD);
            PlayerGoldControl playerGoldControl = playerControllers.get(roomInstance.getOwnerId()).getGoldControl();
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

    private void notifyOnBuild(short playerId, List<MapTile> buildTiles) {
        for (PlayerActionListener listener : listeners.getArray()) {
            listener.onBuild(playerId, buildTiles);
        }
    }

    private void notifyOnSold(short playerId, List<MapTile> soldTiles) {
        for (PlayerActionListener listener : listeners.getArray()) {
            listener.onSold(playerId, soldTiles);
        }
    }

    @Override
    public void pickUp(EntityId entity, short playerId) {
        PlayerHandControl playerHandControl = playerControllers.get(playerId).getHandControl();
        if (!playerHandControl.isFull() && canPickUpEntity(entity, playerId, entityData)) {
            playerHandControl.push(entity);

            // Lose the position component on the entity, do it here since we have the knowledge on locations etc. keep the "hand" simple
            // And also no need to create a system for this which saves resources
            entityData.removeComponent(entity, Position.class);
            entityData.removeComponent(entity, CreatureAi.class);
            entityData.removeComponent(entity, Navigation.class);
        }
    }

    private static boolean canPickUpEntity(EntityId entityId, short playerId, EntityData entityData) {
        // TODO: Somewhere common shared static, can share the rules with the UI client

        // Check if picked up already
        InHand inHand = entityData.getComponent(entityId, InHand.class);
        if (inHand != null) {
            return false;
        }

        // The owner only
        Owner owner = entityData.getComponent(entityId, Owner.class);
        if (owner == null || owner.ownerId != playerId) {
            return false;
        }

        // Is it iteractable?
        Interaction interaction = entityData.getComponent(entityId, Interaction.class);
        if (interaction == null) {
            return false;
        }

        // TODO: Was it so that it can be only picked up from own land?
        return interaction.pickUppable;
    }

    @Override
    public void interact(EntityId entity, short playerId) {
    }

    @Override
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity, short playerId) {
        PlayerHandControl playerHandControl = playerControllers.get(playerId).getHandControl();
        if (playerHandControl.peek() == entity && canDropEntity(entity, playerId, entityData, mapController.getMapData().getTile(tile))) {
            playerHandControl.pop();

            // Stuff drop differently
            if (entityData.getComponent(entity, CreatureComponent.class) != null) {

                // Add position
                // Maybe in the future the physics deal with this and we just need to detect that we are airborne
                Vector3f pos = new Vector3f(coordinates.x, 2, coordinates.y);
                entityData.setComponent(entity, new Position(0, pos));

                // Add the dropping component to the creature
                entityData.setComponent(entity, new CreatureFall());
            } else {

                // TODO: handle giving item to creature & dropping to room
                Vector3f pos = new Vector3f(coordinates.x, 1, coordinates.y);
                entityData.setComponent(entity, new Position(0, pos));
            }
        }
    }

    private boolean canDropEntity(EntityId entityId, short playerId, EntityData entityData, MapTile tile) {
        // TODO: Somewhere common shared static, can share the rules with the UI client

        if (tile == null) {
            return false;
        }

        // Check if picked up in the first place
        InHand inHand = entityData.getComponent(entityId, InHand.class);
        if (inHand == null) {
            return false;
        }

        // The owner only
        Owner owner = entityData.getComponent(entityId, Owner.class);
        if (owner == null || owner.ownerId != playerId) {
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

    public ICreaturesController getCreaturesController() {
        return creaturesController;
    }

    public IDoorsController getDoorsController() {
        return doorsController;
    }

    public IObjectsController getObjectsController() {
        return objectsController;
    }

    public ITrapsController getTrapsController() {
        return trapsController;
    }

}
