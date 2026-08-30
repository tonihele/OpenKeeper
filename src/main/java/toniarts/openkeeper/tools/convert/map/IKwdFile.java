/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

import toniarts.openkeeper.tools.convert.map.Variable.Availability;
import toniarts.openkeeper.tools.convert.map.Variable.CreaturePool;
import toniarts.openkeeper.tools.convert.map.Variable.CreatureStats;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.PlayerAlliance;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a DK II map file, the KWD is the file name of the main map identifier, reads the KLDs actually<br>
 * The files are LITTLE ENDIAN I might say<br>
 * Some values are 3D coordinates or scale values presented in fixed point integers. They are automatically
 * converted to floats (divided by 2^12 = 4096 or 2^16 = 65536)<br>
 * Many parts adapted from C code by:
 * <li>George Gensure (werkt)</li>
 * And another C code implementation by:
 * <li>Thomasz Lis</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IKwdFile extends IKwdInfo {

    /**
     * Get the terrain with the specified ID
     *
     * @param id the id of terrain
     * @return the terrain
     */
    Terrain getTerrain(short id);

    /**
     * Get list of different terrain tiles
     *
     * @return list of terrain tiles
     */
    Collection<Terrain> getTerrainList();

    /**
     * Get list of different objects
     *
     * @return list of objects
     */
    Collection<GameObject> getObjectList();

    /**
     * Get list of different creatures
     *
     * @return list of creatures
     */
    Collection<Creature> getCreatureList();

    /**
     * Get the player with the specified ID
     *
     * @param id the id of player
     * @return the player
     */
    Player getPlayer(short id);

    Map<Short, Player> getPlayers();

    /**
     * Get the creature with the specified ID
     *
     * @param id the id of creature
     * @return the creature
     */
    Creature getCreature(short id);

    /**
     * Bridges are a bit special, identifies one and returns the terrain that should be under it
     *
     * @param type tile BridgeTerrainType
     * @param terrain the terrain tile
     * @return returns null if this is not a bridge, otherwise returns pretty much either water or lava
     */
    Terrain getTerrainBridge(Tile.BridgeTerrainType type, Terrain terrain);

    Terrain getTerrainBridge(Tile.BridgeTerrainType type, Room room);

    /**
     * Get the room with the specified terrain ID
     *
     * @param id the id of terrain
     * @return the room associated with the terrain ID
     */
    Room getRoomByTerrain(short id);

    /**
     * Get list of things by certain type
     *
     * @param <T> the instance type of the things you want
     * @param thingClass the class of things you want
     * @return things list of things you want
     */
    <T extends Thing> List<T> getThings(Class<T> thingClass);

    /**
     * Get the trigger/action with the specified ID
     *
     * @param id the id of trigger/action
     * @return the trigger/action
     */
    Trigger getTrigger(int id);

    Map<Integer, Trigger> getTriggers();

    /**
     * Get the object with the specified ID
     *
     * @param id the id of object
     * @return the object
     */
    GameObject getObject(int id);

    /**
     * Get the room with the specified ID
     *
     * @param id the id of room
     * @return the room
     */
    Room getRoomById(int id);

    /**
     * Get the keeper spell with the specified ID
     *
     * @param id the id of keeper spell
     * @return the keeper spell
     */
    KeeperSpell getKeeperSpellById(int id);

    /**
     * Get the trap with the specified ID
     *
     * @param id the id of trap
     * @return the trap
     */
    Trap getTrapById(int id);

    /**
     * Get the door with the specified ID
     *
     * @param id the id of door
     * @return the door
     */
    Door getDoorById(int id);

    /**
     * Get the list of all rooms
     *
     * @return list of all rooms
     */
    List<Room> getRooms();

    /**
     * Get the list of all keeper spells
     *
     * @return list of all keeper spells
     */
    List<KeeperSpell> getKeeperSpells();

    /**
     * Get the list of all doors
     *
     * @return list of all doors
     */
    List<Door> getDoors();

    /**
     * Get the list of all shots
     *
     * @return list of all shots
     */
    List<Shot> getShots();

    Shot getShotById(short shotId);

    /**
     * Get the list of all traps
     *
     * @return list of all traps
     */
    List<Trap> getTraps();

    CreatureSpell getCreatureSpellById(short spellId);

    Effect getEffect(int effectId);

    Map<Integer, Effect> getEffects();

    EffectElement getEffectElement(int effectElementId);

    Map<Integer, EffectElement> getEffectElements();

    Map<MiscVariable.MiscType, MiscVariable> getVariables();

    List<Availability> getAvailabilities();

    Set<PlayerAlliance> getPlayerAlliances();

    /**
     * Get player specific creature pool
     *
     * @param playerId the player id
     * @return the creature pool
     */
    Map<Integer, CreaturePool> getCreaturePool(short playerId);

    Creature getImp();

    Creature getDwarf();

    Room getPortal();

    Room getDungeonHeart();

    GameObject getLevelGem();

    /**
     * Get the creature stats by level. There might not be a record for every level. Then should just default
     * to 100% stat.
     *
     * @param level the creature level
     * @return the creature stats on given level
     */
    Map<CreatureStats.StatType, CreatureStats> getCreatureStats(int level);

}
