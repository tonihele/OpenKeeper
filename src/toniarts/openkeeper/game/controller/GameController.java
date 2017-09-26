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
import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomGoldControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Game controller, hosts and connects general game functionality
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameController implements IPlayerActions {

    public final Object goldLock = new Object();
    private final KwdFile kwdFile;
    private final EntityData entityData;
    private final SortedMap<Short, Keeper> players = new TreeMap<>();
    private final Map<Short, IPlayerController> playerControllers = new HashMap<>();
    private IObjectsController objectController;

    private IMapController mapController;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final SafeArrayList<PlayerActionListener> listeners = new SafeArrayList<>(PlayerActionListener.class);

    public GameController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
    }

    public void createNewGame(Collection<Keeper> players) {

        // Load objects
        objectController = new ObjectsController(kwdFile, entityData, gameSettings);

        // Load the map
        mapController = new MapController(kwdFile, objectController, gameSettings);

        // Setup players
        if (players != null) {
            for (Keeper keeper : players) {
                this.players.put(keeper.getId(), keeper);
            }
        }
        setupPlayers();

        // Setup player stuff
        initPlayerMoney();
        initPlayerRooms();
    }

    private void setupPlayers() {

        // Setup players
        boolean addMissingPlayers = players.isEmpty(); // Add all if none is given (campaign...)
        for (Entry<Short, Player> entry : kwdFile.getPlayers().entrySet()) {
            Keeper keeper = null;
            if (players.containsKey(entry.getKey())) {
                keeper = players.get(entry.getKey());
                keeper.setPlayer(entry.getValue());
            } else if (addMissingPlayers || entry.getKey() < Player.KEEPER1_ID) {
                keeper = new Keeper(entry.getValue());
                players.put(entry.getKey(), keeper);
            }

            // Init
            if (keeper != null) {
                PlayerController playerController = new PlayerController(keeper, mapController, gameSettings);
                playerControllers.put(entry.getKey(), playerController);

                // Spells are all available for research unless otherwise stated
                for (KeeperSpell spell : kwdFile.getKeeperSpells()) {
                    if (spell.getBonusRTime() != 0) {
                        playerController.getSpellControl().setTypeAvailable(spell, true);
                    }
                }
            }
        }

        // Set player availabilities
        // TODO: the player customized game settings
        for (Variable.Availability availability : kwdFile.getAvailabilities()) {
            if (availability.getPlayerId() == 0 && availability.getType() != Variable.Availability.AvailabilityType.SPELL) {

                // All players
                for (Keeper player : players.values()) {
                    setAvailability(player, availability);
                }
            } else {
                Keeper player = players.get((short) availability.getPlayerId());

                // Not all the players are participating...
                if (player != null) {
                    setAvailability(player, availability);
                }
            }
        }
    }

    private void setAvailability(Keeper player, Variable.Availability availability) {
        IPlayerController playerController = playerControllers.get(player.getId());
        switch (availability.getType()) {
            case CREATURE: {
                playerController.getCreatureControl().setTypeAvailable(kwdFile.getCreature((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
                break;
            }
            case ROOM: {
                playerController.getRoomControl().setTypeAvailable(kwdFile.getRoomById((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
                break;
            }
            case SPELL: {
                if (availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE) {

                    // Enable the spell, no need to research it
                    playerController.getSpellControl().setSpellDiscovered(kwdFile.getKeeperSpellById(availability.getTypeId()), true);
                } else {
                    playerController.getSpellControl().setTypeAvailable(kwdFile.getKeeperSpellById(availability.getTypeId()), false);
                }
            }
        }
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
            if (rooms != null) {
                playerControllers.get(player.getId()).getRoomControl().init(rooms);
            }

            // Add the listener
            //addListener(player.getId(), player.getRoomControl());
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
    public int substractGold(int amount, short playerId) {

        synchronized (goldLock) {

            // See if the player has any gold even
            Keeper keeper = players.get(playerId);
            if (keeper.getGold() == 0) {
                return amount;
            }

            // The gold is subtracted evenly from all treasuries
            int moneySubstracted = amount;
            List<IRoomController> playersTreasuries = mapController.getRoomsByFunction(ObjectType.GOLD, playerId);
            while (moneySubstracted > 0 && !playersTreasuries.isEmpty()) {
                Iterator<IRoomController> iter = playersTreasuries.iterator();
                int goldToRemove = (int) Math.ceil((float) moneySubstracted / playersTreasuries.size());
                while (iter.hasNext()) {
                    IRoomController room = iter.next();
                    RoomGoldControl control = room.getObjectControl(ObjectType.GOLD);
                    goldToRemove = Math.min(moneySubstracted, goldToRemove); // Rounding...
                    moneySubstracted -= goldToRemove - control.removeGold(goldToRemove);
                    if (control.getCurrentCapacity() == 0) {
                        iter.remove();
                    }
                    if (moneySubstracted == 0) {
                        break;
                    }
                }
            }

            // Substract from the player
            playerControllers.get(playerId).getGoldControl().subGold(amount - moneySubstracted);

            return moneySubstracted;
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
                buildPlots.addAll(Arrays.asList(mapController.getSurroundingTiles(p, false)));
                updatableTiles.addAll(Arrays.asList(mapController.getSurroundingTiles(p, true)));
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
                } else {
                    mapController.removeRoomInstances(instance);
                }

                for (Point p : instance.getCoordinates()) {
                    updatableTiles.addAll(Arrays.asList(mapController.getSurroundingTiles(p, true)));
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
                for (Entity entity : entityData.getEntities(Gold.class, Position.class)) {
                    Position position = entityData.getComponent(entity.getId(), Position.class);
                    if (instance.hasCoordinate(WorldUtils.vectorToPoint(position.position))) {
                        Gold gold = entityData.getComponent(entity.getId(), Gold.class);
                        int goldLeft = (int) roomController.getObjectControl(ObjectType.GOLD).addItem(gold.gold, WorldUtils.vectorToPoint(position.position));
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
                    {
                        if (tile.getBridgeTerrainType() == Tile.BridgeTerrainType.LAVA) {
                            tile.setTerrainId(kwdFile.getMap().getLava().getTerrainId());
                        } else {
                            tile.setTerrainId(kwdFile.getMap().getWater().getTerrainId());
                        }
                    }

                    // Give money back
                    int goldLeft = addGold(playerId, (int) (room.getCost() * (gameSettings.get(Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST).getValue() / 100)));
                    if (goldLeft > 0) {

                        // Add loose gold to this tile
                        objectController.addLooseGold(playerId, p.x, p.y, goldLeft, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue());
                    }
                }

                // Get the instance
                soldInstances.add(mapController.getRoomCoordinates().get(p));
                updatableTiles.addAll(Arrays.asList(mapController.getSurroundingTiles(p, true)));
            }
        }

        // Remove the sold instances (will be regenerated) and add them to updatable
        for (RoomInstance roomInstance : soldInstances) {
            for (Point p : roomInstance.getCoordinates()) {
                updatableTiles.addAll(Arrays.asList(mapController.getSurroundingTiles(p, true)));
            }
            roomCoordinates.addAll(roomInstance.getCoordinates());
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

        // Notify
        notifyOnSold(playerId, soldTiles);
    }

    @Override
    public void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
        mapController.selectTiles(start, end, select, playerId);
    }

    public IMapController getMapController() {
        return mapController;
    }

    public Collection<IPlayerController> getPlayerControllers() {
        return playerControllers.values();
    }

    public Collection<Keeper> getPlayers() {
        return players.values();
    }

    /**
     * If you want to get notified about player actiosns
     *
     * @param listener the listener
     */
    public void addListener(PlayerActionListener listener) {
        listeners.add(listener);
    }

    /**
     * Stop listening to player actions
     *
     * @param listener the listener
     */
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

}
