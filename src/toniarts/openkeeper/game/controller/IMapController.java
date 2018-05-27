/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.map.IMapInformation;

/**
 * Map related actions available to all players
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IMapController extends IMapInformation {

    /**
     * Set some tiles selected/undelected
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param select select or unselect
     * @param playerId the player who selected the tile
     */
    void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId);

    /**
     * If you want to get notified about tile changes
     *
     * @param listener the listener
     */
    public void addListener(MapListener listener);

    /**
     * Stop listening to map updates
     *
     * @param listener the listener
     */
    public void removeListener(MapListener listener);

    /**
     * Get all the room controllers. FIXME: At least with the current design,
     * the clients do not have this data, so either separate interface or design
     * controllers so that they share data
     *
     * @return all the room controllers
     */
    public Collection<IRoomController> getRoomControllers();

    /**
     * Get room instance by coordinates
     *
     * @param p the coordinates
     * @return the room instance in the coordinates
     */
    public RoomInstance getRoomInstanceByCoordinates(Point p);

    /**
     * Get a room controller by a room instance. FIXME: At least with the
     * current design, the clients do not have this data, so either separate
     * interface or design controllers so that they share data
     *
     * @param roomInstance the room instance
     * @return room controller associaced to the given instance
     */
    public IRoomController getRoomController(RoomInstance roomInstance);

    /**
     * Get all the rooms that implement a certain function i.e. serves as a
     * storage for the given type etc. FIXME: At least with the current design,
     * the clients do not have this data, so either separate interface or design
     * controllers so that they share data
     *
     * @param objectType the object type (or function if you may)
     * @param playerId the owner of the room
     * @return list of room controllers that match your search criteria
     */
    public List<IRoomController> getRoomsByFunction(AbstractRoomController.ObjectType objectType, Short playerId);

    /**
     * Get all the coordinates that contain rooms and their instances
     *
     * @return room coordinates
     */
    public Map<Point, RoomInstance> getRoomCoordinates();

    /**
     * Get room controllers by the instances
     *
     * @return the room controllers by instances
     */
    public Map<RoomInstance, IRoomController> getRoomControllersByInstances();

    /**
     * Removes room instances
     *
     * @param instances the instances to remove
     */
    public void removeRoomInstances(RoomInstance... instances);

    /**
     * Update rooms in specified coordinates
     *
     * @param coordinates the coordinates
     */
    public void updateRooms(Point[] coordinates);

    /**
     * Damage a tile
     *
     * @param point the point
     * @param playerId the player applying the damage
     * @param creature optional, a creature that is damaging the tile
     * @return you might get gold out of this
     */
    int damageTile(Point point, short playerId, ICreatureController creature);

    /**
     * Attempt to claim the tile or room, applies either damage of heal,
     * depending whose tile is it
     *
     * @param point tile coordinate
     * @param playerId for the player
     */
    void applyClaimTile(Point point, short playerId);

    /**
     * Heal a tile
     *
     * @param point the point
     * @param playerId the player applying the healing
     */
    void healTile(Point point, short playerId);

    /**
     * Alter terrain type
     *
     * @param pos the coordinates
     * @param terrainId the new terrain id
     * @param playerId the new tile owner
     */
    void alterTerrain(Point pos, short terrainId, short playerId);

}
