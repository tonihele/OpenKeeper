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
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Map related actions available to all players
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IMapController {

    /**
     * Get the map data
     *
     * @return the map data
     */
    MapData getMapData();

    /**
     * Sets some specified map tiles in place (updates the map data)
     *
     * @param tiles tiles to set
     */
    void setTiles(List<MapTile> tiles);

    /**
     * Determine if a tile at x & y is buildable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player
     * @param room the room to be build
     * @return is the tile buildable
     */
    boolean isBuildable(int x, int y, Player player, Room room);

    /**
     * Determine if a tile (maybe a room) at x & y is claimable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId the player
     * @return is the tile claimable by you
     */
    boolean isClaimable(int x, int y, short playerId);

    /**
     * Determine if a tile at x & y is selected or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId selected by the player
     * @return is the tile selected
     */
    boolean isSelected(int x, int y, short playerId);

    /**
     * Determine if a tile at x & y is selectable or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selectable
     */
    boolean isTaggable(int x, int y);

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

    public Collection<IRoomController> getRoomControllers();

    public RoomInstance getRoomInstanceByCoordinates(Point p);

    public IRoomController getRoomController(RoomInstance roomInstance);

    public List<IRoomController> getRoomsByFunction(AbstractRoomController.ObjectType objectType, Short playerId);

}
