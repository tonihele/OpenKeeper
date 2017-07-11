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

import com.simsilica.es.EntityData;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomGoldControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Game controller, hosts and connects general game functionality
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameController {

    private final KwdFile kwdFile;
    private final EntityData entityData;
    private final SortedMap<Short, Keeper> players = new TreeMap<>();
    private final Map<Short, IPlayerController> playerControllers = new HashMap<>();

    private IMapController mapController;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;

    public GameController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
    }

    public void createNewGame(Collection<Keeper> players) {

        // Load objects
        IObjectsController objectController = new ObjectsController(kwdFile, entityData, gameSettings);

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
        return addGold(playerId, null, sum);
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

    /**
     * Substract gold from player
     *
     * @param amount the amount to try to substract
     * @param playerId the player id
     * @return amount of money that could not be substracted from the player
     */
    public int substractGold(int amount, short playerId) {

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
                toniarts.openkeeper.world.room.control.RoomGoldControl control = room.getObjectControl(ObjectType.GOLD);
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
        playerControllers.get(playerId).getGoldControl().subGold(moneySubstracted);

        return moneySubstracted;
    }

    public IMapController getMapController() {
        return mapController;
    }

    public Collection<IPlayerController> getPlayerControllers() {
        return playerControllers.values();
    }

}
