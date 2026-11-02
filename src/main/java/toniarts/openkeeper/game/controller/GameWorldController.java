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
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
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
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.component.WoodenBridgeDecay;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.controller.room.storage.RoomGoldControl;
import toniarts.openkeeper.game.controller.room.storage.RoomLairControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private static final short WOODEN_BRIDGE_ROOM_ID = 8;
    private static final int RECEIVE_IMPS_LEVEL = 5;

    private final IKwdFile kwdFile;
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
    private final GameEventService gameEventService;

    private IMapController mapController;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final SafeArrayList<PlayerActionListener> listeners = new SafeArrayList<>(PlayerActionListener.class);

    public GameWorldController(IGameController gameController, ILevelInfo levelInfo, EntityData entityData,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            Map<Short, IPlayerController> playerControllers, IGameTimer gameTimer, GameEventService gameEventService) {

        this.kwdFile = levelInfo.getLevelData();
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.playerControllers = playerControllers;
        this.players = levelInfo.getPlayers();
        this.gameEventService = gameEventService;

        this.createNewGame(gameController, levelInfo);
    }

    private void createNewGame(IGameController gameController, ILevelInfo levelInfo) {

        // Load objects
        objectsController = new ObjectsController(kwdFile, entityData, gameSettings, gameTimer, gameController, levelInfo);

        // Load the map
        mapController = new MapController(kwdFile, objectsController, gameSettings, gameTimer, entityData, levelInfo, gameEventService);

        // Load creatures
        creaturesController = new CreaturesController(kwdFile, entityData, gameSettings, gameTimer, gameController, mapController, levelInfo);

        // Load the doors
        doorsController = new DoorsController(kwdFile, entityData, gameSettings, mapController, gameController, levelInfo);

        // Load the traps
        trapsController = new TrapsController(kwdFile, entityData, gameSettings, gameController, levelInfo);

        // Init handlers
        shotsController = new ShotsController(kwdFile, entityData, gameSettings, gameTimer, gameController, mapController, levelInfo, objectsController, creaturesController, gameEventService);

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
    public void build(Vector2f start, Vector2f end, short playerId, short roomId) {
        Keeper player = players.get(playerId);
        if (player == null) {
            return;
        }

        RoomPlacementValidator.Result placement = RoomPlacementValidator.validate(
                kwdFile, mapController, getConstructionBlockingTiles(), start, end,
                playerId, roomId, player.getGold());
        if (!placement.isValid()) {
            return;
        }

        Room room = kwdFile.getRoomById(roomId);
        if (!payForRoom(placement.plots().size(), room, playerId)) {
            return;
        }

        // Skipped tiles may split the selection, build each connected part as its own room
        List<Point> buildTiles = new ArrayList<>(placement.plots().size());
        for (List<Point> instancePlots : getConnectedPlots(placement.plots())) {
            buildRoomInstance(instancePlots, room, playerId, buildTiles);
        }

        // Notify the build
        notifyOnBuild(playerId, buildTiles);
    }

    private void buildRoomInstance(List<Point> instancePlots, Room room, short playerId, List<Point> buildTiles) {

        // Build & mark
        Set<Point> updatableTiles = new HashSet<>();
        Set<Point> buildPlots = new HashSet<>();
        buildRoomTiles(instancePlots, room, playerId, buildTiles, buildPlots, updatableTiles);

        // See if we hit any of the adjacent rooms
        Set<EntityId> adjacentInstances = findAdjacentRoomInstances(buildPlots, room, playerId);
        mergeRoomInstances(adjacentInstances, instancePlots, updatableTiles);

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
        notifyBuildEvents(room, instancePlots, buildTiles);
    }

    /**
     * One event for the whole placement, at its top left tile, and one for each tile that appears
     */
    private void notifyBuildEvents(Room room, List<Point> plots, List<Point> buildTiles) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Point plot : plots) {
            minX = Math.min(minX, plot.x);
            minY = Math.min(minY, plot.y);
        }
        gameEventService.emit(GameEventType.ROOM_TILES_LAID, ThingFlag.ROOM, room.getRoomId(), new Point(minX, minY));
        for (Point tile : buildTiles) {
            gameEventService.emit(GameEventType.ROOM_TILE_BUILT, ThingFlag.ROOM, room.getRoomId(), tile);
        }
    }

    /**
     * Splits the plots into parts that are connected to each other, rooms are
     * never diagonally attached
     *
     * @param plots the plots
     * @return the connected parts
     */
    static List<List<Point>> getConnectedPlots(Collection<Point> plots) {
        Set<Point> remaining = new LinkedHashSet<>(plots);
        List<List<Point>> parts = new ArrayList<>();
        while (!remaining.isEmpty()) {
            Point start = remaining.iterator().next();
            remaining.remove(start);
            List<Point> part = new ArrayList<>();
            ArrayDeque<Point> open = new ArrayDeque<>();
            open.add(start);
            while (!open.isEmpty()) {
                Point p = open.removeFirst();
                part.add(p);
                addConnectedPlot(p.x, p.y - 1, remaining, open); // North
                addConnectedPlot(p.x + 1, p.y, remaining, open); // East
                addConnectedPlot(p.x, p.y + 1, remaining, open); // South
                addConnectedPlot(p.x - 1, p.y, remaining, open); // West
            }
            parts.add(part);
        }
        return parts;
    }

    private static void addConnectedPlot(int x, int y, Set<Point> remaining, ArrayDeque<Point> open) {
        Point plot = new Point(x, y);
        if (remaining.remove(plot)) {
            open.addLast(plot);
        }
    }

    private Set<Point> getConstructionBlockingTiles() {
        Set<Point> blockedTiles = new HashSet<>();
        addConstructionBlockingPositions(blockedTiles, ObjectComponent.class, this::isConstructionBlockingObject);
        addConstructionBlockingPositions(blockedTiles, DoorComponent.class, entityId -> true);
        addConstructionBlockingPositions(blockedTiles, TrapComponent.class, entityId -> true);
        return blockedTiles;
    }

    private boolean isConstructionBlockingObject(EntityId entityId) {
        ObjectComponent objectComponent = entityData.getComponent(entityId, ObjectComponent.class);
        return RoomPlacementValidator.isConstructionBlockingObject(kwdFile.getObject(objectComponent.objectId));
    }

    private void addConstructionBlockingPositions(Set<Point> blockedTiles,
            Class<? extends EntityComponent> componentType, Predicate<EntityId> isBlocking) {
        for (EntityId entityId : entityData.findEntities(null, componentType, Position.class)) {
            if (!isBlocking.test(entityId)) {
                continue;
            }
            Position position = entityData.getComponent(entityId, Position.class);
            if (position != null) {
                blockedTiles.add(WorldUtils.vectorToPoint(position.position));
            }
        }
    }

    private boolean payForRoom(int plotCount, Room room, short playerId) {
        // Keep shared gold operations atomic until player actions are processed by the game loop.
        // skipcq: JAVA-E1061
        synchronized (GOLD_LOCK) {
            int cost = plotCount * room.getCost();
            if (cost > players.get(playerId).getGold()) {
                return false;
            }
            substractGold(cost, playerId);
            return true;
        }
    }

    private void buildRoomTiles(List<Point> instancePlots, Room room, short playerId,
            List<Point> buildTiles, Set<Point> buildPlots, Set<Point> updatableTiles) {
        for (Point p : instancePlots) {
            buildPlots.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, false)));
            updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));

            IMapTileController tile = mapController.getMapData().getTile(p);
            tile.setOwnerId(playerId);
            tile.setTerrainId(room.getTerrainId());
            if (room.getRoomId() == WOODEN_BRIDGE_ROOM_ID
                    && tile.getBridgeTerrainType() == Tile.BridgeTerrainType.LAVA) {
                double lifetime = gameSettings.get(Variable.MiscVariable.MiscType.WOOD_BRIDGE_LIFE_ON_LAVA_SECONDS).getValue();
                entityData.setComponent(tile.getEntityId(), new WoodenBridgeDecay(gameTimer.getGameTime() + lifetime));
            }
            buildTiles.add(tile.getLocation());
        }
    }

    private Set<EntityId> findAdjacentRoomInstances(Set<Point> buildPlots, Room room, short playerId) {
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
        return adjacentInstances;
    }

    private void mergeRoomInstances(Set<EntityId> adjacentInstances, List<Point> instancePlots,
            Set<Point> updatableTiles) {
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
                    transferLairs(mapController.getRoomController(instance), mapController.getRoomController(firstInstance));
                    removeRoomInstance(instance);
                    mapController.removeRoomInstances(instance);
                }
            }
            // TODO: The room health! We need to make sure that the health is distributed evenly

            // Update the merged room
            mapController.getRoomController(firstInstance).construct();
            addGoldCapacityToPlayer(firstInstance);
        }
    }

    @Override
    public void sell(Vector2f start, Vector2f end, short playerId) {
        // A trap is sold on its own, one at a time and never together with rooms
        Point startTile = new Point((int) start.x, (int) start.y);
        if (startTile.equals(new Point((int) end.x, (int) end.y)) && sellTrap(startTile, playerId)) {
            return;
        }

        Sale sale = sellTiles(start, end, playerId);
        if (sale.soldTiles.isEmpty()) {
            return;
        }

        List<EntityId> detachedLairs = removeSoldRoomInstances(sale);
        mapController.updateRooms(sale.updatableTiles.toArray(Point[]::new));
        reattachLairs(detachedLairs);
        restoreGoldCapacity(sale.roomCoordinates);
        returnSaleRevenue(playerId, sale.moneyToReturnByPoint);
        notifyOnSold(playerId, sale.soldTiles);
    }

    private boolean sellTrap(Point tile, short playerId) {
        if (mapController.getMapData().getTile(tile) == null) {
            return false;
        }

        // Only the entities on the tile, not every trap on the map
        for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
            Entity entity = entityData.getEntity(entityId, TrapComponent.class, Owner.class);
            TrapComponent trapComponent = entity.get(TrapComponent.class);
            Owner owner = entity.get(Owner.class);
            if (trapComponent == null || owner == null || !TrapSellRules.isSellable(owner.ownerId, playerId)) {
                continue;
            }

            Trap trap = kwdFile.getTrapById(trapComponent.trapId);
            int saleValue = TrapSellRules.getSellValue(trap.getGoldCost(),
                    gameSettings.get(Variable.MiscVariable.MiscType.TRAP_SELL_VALUE_PERCENTAGE_OF_COST).getValue());
            trapsController.createController(entityId).remove();
            returnSaleRevenue(playerId, List.of(new AbstractMap.SimpleImmutableEntry<>(tile, saleValue)));
            return true;
        }
        return false;
    }

    private Sale sellTiles(Vector2f start, Vector2f end, short playerId) {
        Sale sale = new Sale();
        for (int x = (int) Math.max(0, start.x); x < Math.min(kwdFile.getMap().getWidth(), end.x + 1); x++) {
            for (int y = (int) Math.max(0, start.y); y < Math.min(kwdFile.getMap().getHeight(), end.y + 1); y++) {
                Point p = new Point(x, y);
                if (!mapController.isSellable(p, playerId)) {
                    continue;
                }
                sellTile(p, sale);
            }
        }
        return sale;
    }

    private void sellTile(Point p, Sale sale) {
        IMapTileController tile = mapController.getMapData().getTile(p);
        if (tile == null) {
            return;
        }
        entityData.removeComponent(tile.getEntityId(), WoodenBridgeDecay.class);
        sale.soldTiles.add(tile.getLocation());

        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
        gameEventService.emit(GameEventType.ROOM_SOLD, ThingFlag.ROOM, room.getRoomId(), p);
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
            tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
        } else if (tile.getBridgeTerrainType() == Tile.BridgeTerrainType.LAVA) {
            tile.setTerrainId(kwdFile.getMap().getLava().getTerrainId());
        } else {
            tile.setTerrainId(kwdFile.getMap().getWater().getTerrainId());
        }

        int saleValue = (int) (room.getCost()
                * (gameSettings.get(Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST).getValue() / 100));
        sale.moneyToReturnByPoint.add(new AbstractMap.SimpleImmutableEntry<>(p, saleValue));
        sale.soldInstances.add(tile.getRoomId());
        sale.updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
    }

    private List<EntityId> removeSoldRoomInstances(Sale sale) {
        List<EntityId> detachedLairs = new ArrayList<>();
        for (EntityId roomInstance : sale.soldInstances) {
            IRoomController roomController = mapController.getRoomController(roomInstance);
            for (Point p : roomController.getRoomInstance().getCoordinates()) {
                sale.updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), p, true)));
            }
            sale.roomCoordinates.addAll(roomController.getRoomInstance().getCoordinates());
            detachedLairs.addAll(detachLairs(roomController));
            removeRoomInstance(roomInstance);
        }
        mapController.removeRoomInstances(sale.soldInstances.toArray(EntityId[]::new));
        return detachedLairs;
    }

    private void reattachLairs(List<EntityId> detachedLairs) {
        // Reattach beds on surviving lair tiles. Beds on sold tiles are
        // deleted; their creatures will observe the missing entity and wake.
        for (EntityId lair : detachedLairs) {
            Position position = entityData.getComponent(lair, Position.class);
            IRoomController roomController = position != null
                    ? mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(position.position)) : null;
            if (roomController != null && roomController.hasObjectControl(ObjectType.LAIR)) {
                RoomLairControl roomLairControl = roomController.getObjectControl(ObjectType.LAIR);
                roomLairControl.addExistingItem(lair, WorldUtils.vectorToPoint(position.position));
            } else {
                removeLair(lair);
            }
        }
    }

    private void restoreGoldCapacity(List<Point> roomCoordinates) {
        Set<EntityId> newInstances = new HashSet<>();
        for (Point p : roomCoordinates) {
            EntityId instance = mapController.getMapData().getTile(p).getRoomId();
            if (instance != null && !newInstances.contains(instance)) {
                newInstances.add(instance);
                addGoldCapacityToPlayer(instance);
            }
        }
    }

    private void returnSaleRevenue(short playerId, List<Map.Entry<Point, Integer>> moneyToReturnByPoint) {
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
    }

    private static final class Sale {

        private final List<Point> soldTiles = new ArrayList<>();
        private final Set<Point> updatableTiles = new HashSet<>();
        private final Set<EntityId> soldInstances = new HashSet<>();
        private final List<Point> roomCoordinates = new ArrayList<>();
        private final List<Map.Entry<Point, Integer>> moneyToReturnByPoint = new ArrayList<>();
    }

    @Override
    public void destroyRoomTiles(Collection<Point> points) {
        Set<Point> updatableTiles = new HashSet<>();
        Set<EntityId> destroyedInstances = new HashSet<>();
        Map<Short, List<Point>> destroyedTilesByOwner = new java.util.HashMap<>();

        for (Point point : points) {
            destroyRoomTile(point, destroyedInstances, updatableTiles, destroyedTilesByOwner);
        }

        if (destroyedTilesByOwner.isEmpty()) {
            return;
        }

        List<Point> roomCoordinates = removeDestroyedRoomInstances(destroyedInstances, updatableTiles);
        mapController.removeRoomInstances(destroyedInstances.toArray(EntityId[]::new));
        mapController.updateRooms(updatableTiles.toArray(Point[]::new));

        restoreGoldCapacity(roomCoordinates);
        notifyDestroyedTiles(destroyedTilesByOwner);
    }

    private void destroyRoomTile(Point point, Set<EntityId> destroyedInstances,
            Set<Point> updatableTiles, Map<Short, List<Point>> destroyedTilesByOwner) {
        IMapTileController tile = mapController.getMapData().getTile(point);
        if (tile == null) {
            return;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            return;
        }

        Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
        if (room == null) {
            return;
        }
        destroyedTilesByOwner.computeIfAbsent(tile.getOwnerId(), ignored -> new ArrayList<>()).add(tile.getLocation());
        destroyedInstances.add(tile.getRoomId());
        entityData.removeComponent(tile.getEntityId(), WoodenBridgeDecay.class);

        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
            tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
        } else if (tile.getBridgeTerrainType() == Tile.BridgeTerrainType.LAVA) {
            tile.setTerrainId(kwdFile.getMap().getLava().getTerrainId());
        } else {
            tile.setTerrainId(kwdFile.getMap().getWater().getTerrainId());
        }
        updatableTiles.addAll(Arrays.asList(
                WorldUtils.getSurroundingTiles(mapController.getMapData(), point, true)));
    }

    private List<Point> removeDestroyedRoomInstances(Set<EntityId> destroyedInstances,
            Set<Point> updatableTiles) {
        List<Point> roomCoordinates = new ArrayList<>();
        for (EntityId roomInstance : destroyedInstances) {
            IRoomController roomController = mapController.getRoomController(roomInstance);
            if (roomController == null) {
                continue;
            }
            for (Point point : roomController.getRoomInstance().getCoordinates()) {
                updatableTiles.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(mapController.getMapData(), point, true)));
            }
            roomCoordinates.addAll(roomController.getRoomInstance().getCoordinates());
            removeRoomInstance(roomInstance);
        }
        return roomCoordinates;
    }

    private void notifyDestroyedTiles(Map<Short, List<Point>> destroyedTilesByOwner) {
        for (Map.Entry<Short, List<Point>> entry : destroyedTilesByOwner.entrySet()) {
            notifyOnSold(entry.getKey(), entry.getValue());
        }
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

    private static Collection<EntityId> detachLairs(IRoomController roomController) {
        if (!roomController.hasObjectControl(ObjectType.LAIR)) {
            return Collections.emptyList();
        }
        RoomLairControl roomLairControl = roomController.getObjectControl(ObjectType.LAIR);
        return roomLairControl.detachAllItems();
    }

    private void transferLairs(IRoomController source, IRoomController destination) {
        if (!source.hasObjectControl(ObjectType.LAIR) || !destination.hasObjectControl(ObjectType.LAIR)) {
            return;
        }
        RoomLairControl destinationControl = destination.getObjectControl(ObjectType.LAIR);
        for (EntityId lair : detachLairs(source)) {
            Position position = entityData.getComponent(lair, Position.class);
            if (position != null) {
                destinationControl.addExistingItem(lair, WorldUtils.vectorToPoint(position.position));
            } else {
                removeLair(lair);
            }
        }
    }

    private void removeLair(EntityId lair) {
        for (EntityId creature : entityData.findEntities(new FieldFilter(CreatureSleep.class, "lairObjectId", lair), CreatureSleep.class)) {
            CreatureSleep sleep = entityData.getComponent(creature, CreatureSleep.class);
            if (lair.equals(sleep.lairObjectId)) {
                CreatureSleep updated = new CreatureSleep(sleep);
                updated.lairObjectId = null;
                entityData.setComponent(creature, updated);
                break;
            }
        }
        entityData.removeEntity(lair);
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
        if (activateSpecialOnOwnLand(entity, playerId)) {
            return;
        }

        PlayerHandControl playerHandControl = playerControllers.get(playerId).getHandControl();
        if (!playerHandControl.isFull() && canPickUpEntity(entity, playerId, entityData)) {
            putToKeeperHand(playerHandControl, entity, playerId);
        }
    }

    /**
     * Specials execute their effect immediately and are consumed when clicked while sitting on the
     * clicking player's own land, instead of being picked up into the keeper's hand
     *
     * @param entity the entity clicked on
     * @param playerId the player who clicked
     * @return {@code true} if the entity was a special whose effect was activated
     */
    private boolean activateSpecialOnOwnLand(EntityId entity, short playerId) {
        ObjectComponent objectComponent = entityData.getComponent(entity, ObjectComponent.class);
        if (objectComponent == null || !kwdFile.getObject(objectComponent.objectId).getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_SPECIAL)) {
            return false;
        }

        Position position = entityData.getComponent(entity, Position.class);
        if (position == null) {
            return false;
        }
        IMapTileController tile = mapController.getMapData().getTile(WorldUtils.vectorToPoint(position.position));
        if (tile == null || tile.getOwnerId() != playerId) {
            return false;
        }

        if (!activateSpecial(objectComponent.objectId, playerId, position)) {
            return false;
        }

        entityData.removeEntity(entity);
        return true;
    }

    /**
     * Runs the effect for a known special object
     *
     * @param objectId the special's object ID
     * @param playerId the player whose land the special was activated on
     * @param position where the special was standing, for effects that spawn something on the spot
     * @return {@code true} if the object ID was a recognized special and its effect ran
     */
    private boolean activateSpecial(short objectId, short playerId, Position position) {
        switch (objectId) {
            case ObjectsController.OBJECT_SPECIAL_INCREASE_LEVEL_ID ->
                creaturesController.increaseLevelOfCreatures(playerId, 1);
            case ObjectsController.OBJECT_SPECIAL_REVEAL_MAP_ID ->
                mapController.disableFogOfWar(playerId);
            case ObjectsController.OBJECT_SPECIAL_MAKE_SAFE_ID ->
                makeSafe(playerId);
            case ObjectsController.OBJECT_SPECIAL_DESTROY_WALLS_ID ->
                destroyEnemyWalls(playerId);
            case ObjectsController.OBJECT_SPECIAL_INCREASE_GOLD_ID ->
                addGold(playerId, (int) gameSettings.get(Variable.MiscVariable.MiscType.SPECIAL_INCREASE_GOLD_AMOUNT).getValue());
            case ObjectsController.OBJECT_SPECIAL_MANA_BOOST_ID ->
                playerControllers.get(playerId).getManaControl().addMana((int) gameSettings.get(Variable.MiscVariable.MiscType.SPECIAL_INCREASE_MANA_AMOUNT).getValue());
            case ObjectsController.OBJECT_SPECIAL_STUN_IMPS_ID ->
                stunEnemyImps(playerId);
            case ObjectsController.OBJECT_SPECIAL_RECEIVE_IMPS_ID ->
                receiveImps(playerId, position);
            case ObjectsController.OBJECT_SPECIAL_MAKE_HAPPY_ID ->
                creaturesController.makeCreaturesHappy(playerId);
            case ObjectsController.OBJECT_SPECIAL_MAKE_UNHAPPY_ID ->
                creaturesController.angerEnemyCreatures(playerId);
            case ObjectsController.OBJECT_SPECIAL_HEAL_ALL_ID ->
                creaturesController.healCreatures(playerId);
            default -> {
                logger.log(Level.WARNING, "Unknown special, objectId: {0}", objectId);
                return false;
            }
        }
        return true;
    }

    /**
     * The "Make Safe" special: claims every unclaimed solid rock wall adjacent to the player's own land
     *
     * @param playerId the player claiming the walls
     */
    private void makeSafe(short playerId) {
        var mapData = mapController.getMapData();
        for (int x = 0; x < mapData.getWidth(); x++) {
            for (int y = 0; y < mapData.getHeight(); y++) {
                Point p = new Point(x, y);
                IMapTileController tile = mapData.getTile(p);
                if (tile == null || tile.getOwnerId() != Player.NEUTRAL_PLAYER_ID) {
                    continue;
                }

                Terrain terrain = mapController.getTerrain(tile);
                if (terrain.getMaxHealthTypeTerrainId() == 0 || !mapController.isClaimableWall(p, playerId)) {
                    continue;
                }

                mapController.alterTerrain(p, terrain.getMaxHealthTypeTerrainId(), playerId);
            }
        }
    }

    /**
     * The "Destroy Walls" special: unclaims every solid wall owned by another player on the whole map and
     * resets it back to its neutral rock terrain
     *
     * @param playerId the player activating the special, whose own walls are left untouched
     */
    private void destroyEnemyWalls(short playerId) {
        var mapData = mapController.getMapData();
        for (int x = 0; x < mapData.getWidth(); x++) {
            for (int y = 0; y < mapData.getHeight(); y++) {
                Point p = new Point(x, y);
                IMapTileController tile = mapData.getTile(p);
                if (tile == null) {
                    continue;
                }

                short ownerId = tile.getOwnerId();
                if (ownerId == Player.NEUTRAL_PLAYER_ID || ownerId == playerId) {
                    continue;
                }

                Terrain terrain = mapController.getTerrain(tile);
                if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) || !terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    continue;
                }

                Terrain unclaimedTerrain = getUnclaimedWallTerrain(terrain);
                if (unclaimedTerrain == null) {
                    continue;
                }

                mapController.alterTerrain(p, unclaimedTerrain.getTerrainId(), Player.NEUTRAL_PLAYER_ID);
            }
        }
    }

    /**
     * Finds the base, unclaimed wall terrain that heals into the given (already claimed) wall terrain
     *
     * @param claimedTerrain the claimed wall terrain
     * @return the matching unclaimed terrain, or {@code null} if none was found
     */
    private Terrain getUnclaimedWallTerrain(Terrain claimedTerrain) {
        for (Terrain candidate : kwdFile.getTerrainList()) {
            if (candidate.getMaxHealthTypeTerrainId() == claimedTerrain.getTerrainId()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * The "Stun enemy imps" special: freezes every imp not owned by the activating player for a short time
     *
     * @param playerId the player activating the special
     */
    private void stunEnemyImps(short playerId) {
        for (IPlayerController otherPlayerController : playerControllers.values()) {
            short otherPlayerId = otherPlayerController.getKeeper().getId();
            if (otherPlayerId == playerId) {
                continue;
            }
            creaturesController.stunImps(otherPlayerId);
        }
    }

    /**
     * The "Receive imps" special: conjures new level 5 imps for the player on the special's spot. The original
     * skips the spell for these, so there is no cast sound.
     *
     * @param playerId the player receiving the imps
     * @param position where the special was
     */
    private void receiveImps(short playerId, Position position) {
        short impId = kwdFile.getImp().getCreatureId();
        Vector2f spot = WorldUtils.vector3fToVector2f(position.position);
        int amount = (int) gameSettings.get(Variable.MiscVariable.MiscType.SPECIAL_RECEIVE_IMPS_AMOUNT).getValue();
        for (int i = 0; i < amount; i++) {
            creaturesController.spawnCreature(impId, playerId, RECEIVE_IMPS_LEVEL, spot, ICreaturesController.SpawnType.CONJURE);
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
        gameEventService.emit(GameEventType.PICKED_UP, entity, position.position);
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

    private boolean canPickUpEntity(EntityId entityId, short playerId, EntityData entityData) {
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

        if (!interaction.pickUppable || isEntityIncapacitated(entityId, entityData)) {
            return false;
        }

        return EntityPickupValidator.isValidLocation(
                entityData.getComponent(entityId, ObjectComponent.class),
                entityData.getComponent(entityId, Position.class),
                playerId,
                mapController.getMapData());
    }

    @Override
    public void interact(EntityId entity, short playerId) {

        // Creatures can only be slapped, also enemies held in our prison
        // TODO: Slap limit
        if (entityData.getComponent(entity, CreatureComponent.class) != null) {
            if (canSlapCreature(entity, playerId)) {
                entityData.setComponent(entity, new Slapped(gameTimer.getGameTime()));
                notifySlap(entity, playerId);
            }
            return;
        }

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

        // Objects
        ObjectComponent objectComponent = entityData.getComponent(entity, ObjectComponent.class);
        if (objectComponent != null && interaction.slappable) {
            entityData.setComponent(entity, new Slapped(gameTimer.getGameTime()));
            notifySlap(entity, playerId);
            return;
        }
    }

    private void notifySlap(EntityId entity, short playerId) {
        // The slap comes from the player's hand, so it has no place and only the player gets it
        gameEventService.emitToPlayer(GameEventType.HAND_SLAP, playerId);
        gameEventService.emit(GameEventType.SLAPPED, entity);
    }

    @Override
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity, short playerId) {
        PlayerHandControl playerHandControl = playerControllers.get(playerId).getHandControl();
        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        Terrain terrain = mapTile != null ? mapController.getTerrain(mapTile) : null;
        if (playerHandControl.peek().equals(entity) && canDropEntity(entity, playerId, entityData, mapTile, terrain)) {
            playerHandControl.pop();
            Vector3f dropPosition = new Vector3f(coordinates.x, 0, coordinates.y);
            gameEventService.emit(GameEventType.DROPPED, entity, dropPosition);

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
        CreatureState targetState = null;
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
            targetState = getLairRestTarget(roomController, entity, owner.ownerId, tortureOrImprisonment);
        }

        // Show the fall animation right away, before the position brings the model back to the map
        if (!tortureOrImprisonment) {
            CreatureViewState viewState = entityData.getComponent(entity, CreatureViewState.class);
            entityData.setComponent(entity, new CreatureViewState(viewState.creatureId, gameTimer.getGameTime(), Creature.AnimationType.FALLBACK));
        }

        // Add position
        // Maybe in the future the physics deal with this and we just need to detect that we are airborne
        Vector3f pos = new Vector3f(coordinates.x, (tortureOrImprisonment ? WorldUtils.FLOOR_HEIGHT : WorldUtils.DROP_HEIGHT), coordinates.y);
        entityData.setComponent(entity, new Position(0, pos));

        restoreDroppedCreatureAi(entity, creatureState, targetState);

        // Add the dropping component to the creature
        if (!tortureOrImprisonment) {
            entityData.setComponent(entity, new CreatureFall());
        } else {

            // No fall, so no touchdown for the fall system to announce
            gameEventService.emit(GameEventType.CREATURE_LAND, entity, pos);
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

    /** Restores captive AI or a pending rest target; falling creatures wait for landing. */
    private void restoreDroppedCreatureAi(EntityId entity, CreatureState creatureState, CreatureState targetState) {
        if (creatureState != null || targetState != null) {
            CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);
            entityData.setComponent(entity, new CreatureAi(gameTimer.getGameTime(),
                    creatureState != null ? creatureState : CreatureState.FALLEN,
                    creatureComponent.creatureId, targetState));
        }
    }

    /**
     * Requests rest for a non-captive drop into an owned Lair. The creature
     * controller validates its current room and assigned bed after getting up.
     */
    private CreatureState getLairRestTarget(IRoomController roomController, EntityId entity, short ownerId, boolean captive) {
        return !captive && roomController.getOwnerId() == ownerId
                && roomController.hasObjectControl(ObjectType.LAIR)
                && entityData.getComponent(entity, CreatureSleep.class) != null
                        ? CreatureState.GO_TO_SLEEP : null;
    }

    private void dropGold(EntityId entity, IRoomController roomController, short playerId, Point tile, Vector2f coordinates) {
        Gold gold = entityData.getComponent(entity, Gold.class);
        if (roomController != null && roomController.canStoreGold()) {
            int leftOverGold = addGold(playerId, tile, gold.gold);

            // The treasury is filled first, the rest forms a loose pile in the treasury
            if (leftOverGold > 0) {
                dropLooseGold(playerId, tile, null, leftOverGold);
            }
        } else {
            dropLooseGold(playerId, tile, coordinates, gold.gold);
        }
        entityData.removeEntity(entity);
    }

    /**
     * Drops loose gold from the hand. Like in DK II, the gold is added to a
     * loose gold pile already lying on the tile, there is no limit to the pile
     * size. Gold can only be dropped on own land, and in DK II loose gold
     * belongs to the owner of the land, so the pile becomes ours
     *
     * @param playerId the player dropping the gold
     * @param tile the tile to drop the gold to
     * @param coordinates exact coordinates for a new pile, {@code null} for
     * anywhere on the tile
     * @param amount the amount of gold
     */
    private void dropLooseGold(short playerId, Point tile, Vector2f coordinates, int amount) {
        synchronized (GOLD_LOCK) {
            EntityId goldPile = findLooseGoldPile(entityData, entityPositionLookup, tile);
            if (goldPile != null) {
                Gold gold = entityData.getComponent(goldPile, Gold.class);
                entityData.setComponent(goldPile, new Gold(gold.gold + amount, gold.maxGold));
                entityData.setComponent(goldPile, new Owner(playerId, playerId));
                return;
            }
        }

        EntityId newGold = objectsController.addLooseGold(playerId, tile.x, tile.y, amount, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue());
        if (coordinates != null) {
            Vector3f pos = new Vector3f(coordinates.x, 1, coordinates.y);
            entityData.setComponent(newGold, new Position(0, pos));
        }
    }

    /**
     * Finds a loose gold pile lying on the given tile. Gold stored in rooms
     * and gold carried by creatures don't count. Only the entities on the
     * tile are looked at, not all the gold on the map
     *
     * @param entityData the entity data
     * @param entityPositionLookup the entities by tile
     * @param tile the tile to search
     * @return the gold pile, {@code null} if there is none
     */
    static EntityId findLooseGoldPile(EntityData entityData, IEntityPositionLookup entityPositionLookup, Point tile) {
        for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
            ObjectComponent objectComponent = entityData.getComponent(entityId, ObjectComponent.class);
            if (objectComponent != null && objectComponent.objectType == ObjectType.GOLD
                    && entityData.getComponent(entityId, Gold.class) != null
                    && entityData.getComponent(entityId, Stored.class) == null) {
                return entityId;
            }
        }
        return null;
    }

    private static boolean canDropEntity(EntityId entityId, short playerId, EntityData entityData, IMapTileInformation tile, Terrain terrain) {

        // Check if picked up in the first place
        InHand inHand = entityData.getComponent(entityId, InHand.class);
        if (inHand == null) {
            return false;
        }

        // The same rules as the client
        Interaction interaction = entityData.getComponent(entityId, Interaction.class);
        return DropValidator.canDrop(interaction, tile, terrain, playerId);
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

    private boolean canSlapCreature(EntityId entityId, short playerId) {
        Owner owner = entityData.getComponent(entityId, Owner.class);
        Interaction interaction = entityData.getComponent(entityId, Interaction.class);
        if (owner == null || interaction == null || !interaction.slappable || isEntityIncapacitated(entityId, entityData)) {
            return false;
        }

        boolean imprisoned = entityData.getComponent(entityId, CreatureImprisoned.class) != null;
        return SlapRules.canSlapCreature(playerId, owner.ownerId, owner.controlId, imprisoned);
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

        // The workers get more expensive the more of them there are
        int manaCost = keeperSpell.getManaCost();
        if (keeperSpell.getFlags().contains(KeeperSpell.KeeperSpellFlag.CREATE_WORKER)) {
            int minimumImpCount = (int) gameSettings.get(Variable.MiscVariable.MiscType.MINIMUM_IMP_THRESHOLD).getValue();
            int impCount = playerControllers.get(playerId).getCreatureControl().getImpCount();
            manaCost *= Math.max(1, impCount + 1 - minimumImpCount);
            if (manaCost > player.getMana() - player.getManaLoose()) {
                return;
            }
        }

        // Deduct the mana
        playerControllers.get(playerId).getManaControl().addMana(-manaCost);

        // Cast the spell
        boolean spellUpgraded = researchableEntity.isUpgraded();
        int shotData1 = spellUpgraded ? keeperSpell.getBonusShotData1() : keeperSpell.getShotData1();
        int shotData2 = spellUpgraded ? keeperSpell.getBonusShotData2() : keeperSpell.getShotData2();
        gameEventService.emit(GameEventType.SHOT_LAUNCH, ThingFlag.KEEPER_SPELL, keeperSpell.getKeeperSpellId(), WorldUtils.vector2fToVector3(position));
        short shotTypeId = spellUpgraded && kwdFile.getShotById(keeperSpell.getBonusShotTypeId()) != null
                ? keeperSpell.getBonusShotTypeId() : keeperSpell.getShotTypeId();
        shotsController.createShot(shotTypeId, shotData1, shotData2, playerId, WorldUtils.vector2fToVector3(position), target, null);
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
            logger.log(Level.WARNING, "Invalid trap ID for trap placement received, was: {0}", trapId);
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

        ResearchableEntity researchableEntity = playerControllers.get(playerId).getTrapControl().getTypes().get(trap);
        if (researchableEntity == null || !researchableEntity.isDiscovered()) {
            logger.log(Level.WARNING, "Player trap is not available", playerId);
            return;
        }

        // Validate
        if (TrapPlacementValidator.validate(mapController, getTrapBlockingTiles(tile), tile, playerId, trap, player.getGold())
                != TrapPlacementValidator.Failure.NONE) {
            return;
        }

        if (!payForTrap(trap, playerId)) {
            return;
        }

        // The blueprint, the workshop manufactures the trap for it
        trapsController.addTrapBlueprint(trapId, tile, playerId);
    }

    private boolean payForTrap(Trap trap, short playerId) {
        // Keep shared gold operations atomic until player actions are processed by the game loop.
        // skipcq: JAVA-E1061
        synchronized (GOLD_LOCK) {
            int cost = trap.getGoldCost();
            if (cost > players.get(playerId).getGold()) {
                return false;
            }
            substractGold(cost, playerId);
            return true;
        }
    }

    /**
     * The tile if a trap or a door already stands on it. Only the entities on
     * the tile are looked at, not every trap and door on the map
     *
     * @param tile the tile to place a trap on
     * @return the tile if it is taken, otherwise nothing
     */
    private Set<Point> getTrapBlockingTiles(Point tile) {
        for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
            Entity entity = entityData.getEntity(entityId, TrapComponent.class, DoorComponent.class);
            if (entity.get(TrapComponent.class) != null || entity.get(DoorComponent.class) != null) {
                return Set.of(tile);
            }
        }
        return Set.of();
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
