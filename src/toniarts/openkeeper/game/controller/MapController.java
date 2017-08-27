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
package toniarts.openkeeper.game.controller;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * This is controller for the map related functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapController implements Savable, IMapController {

    private MapData mapData;
    private KwdFile kwdFile;
    private IObjectsController objectsController;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final Map<Point, RoomInstance> roomCoordinates = new HashMap<>();
    private final Map<RoomInstance, IRoomController> roomControllers = new HashMap<>();
    private final SafeArrayList<MapListener> listeners = new SafeArrayList<>(MapListener.class);

    public MapController() {
        // For serialization
    }

    /**
     * Load map data from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param objectsController objects controller
     * @param gameSettings the game settings
     */
    public MapController(KwdFile kwdFile, IObjectsController objectsController, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.kwdFile = kwdFile;
        this.objectsController = objectsController;
        this.mapData = new MapData(kwdFile);
        this.gameSettings = gameSettings;

        // Load rooms
        loadRooms();
    }

    /**
     * Instantiate a map controller from map data (loaded game)
     *
     * @param mapData the map data
     * @param kwdFile the KWD file
     * @param gameSettings the game settings
     */
    public MapController(MapData mapData, KwdFile kwdFile, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.mapData = mapData;
        this.kwdFile = kwdFile;
        this.gameSettings = gameSettings;
    }

    public MapController(MapData mapData, KwdFile kwdFile) {
        this.mapData = mapData;
        this.kwdFile = kwdFile;
    }

    private void loadRooms() {

        // Go through the tiles and detect any rooms
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                loadRoom(new Point(x, y));
            }
        }
    }

    private void loadRoom(Point p) {
        MapTile mapTile = mapData.getTile(p);
        if (!kwdFile.getTerrain(mapTile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            return;
        }

        if (roomCoordinates.containsKey(p)) {
            return;
        }

        // Find it
        RoomInstance roomInstance = new RoomInstance(kwdFile.getRoomByTerrain(mapTile.getTerrainId()));
        roomInstance.setOwnerId(mapTile.getOwnerId());
        findRoom(p, roomInstance);

        // Create a controller for it
        IRoomController roomController = RoomControllerFactory.constructRoom(roomInstance, objectsController, gameSettings);
        roomController.construct();
        roomControllers.put(roomInstance, roomController);
    }

    /**
     * Find the room starting from a certain point, rooms are never diagonally
     * attached
     *
     * @param p starting point
     * @param roomInstance the room instance
     */
    private void findRoom(Point p, RoomInstance roomInstance) {
        MapTile tile = getMapData().getTile(p);

        // Get the terrain
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            if (!roomCoordinates.containsKey(p)) {
                if (roomInstance.getRoom().equals(kwdFile.getRoomByTerrain(terrain.getTerrainId()))) {

                    // Add the coordinate
                    roomCoordinates.put(p, roomInstance);
                    roomInstance.addCoordinate(p);

                    // Find north
                    findRoom(new Point(p.x, p.y - 1), roomInstance);

                    // Find east
                    findRoom(new Point(p.x + 1, p.y), roomInstance);

                    // Find south
                    findRoom(new Point(p.x, p.y + 1), roomInstance);

                    // Find west
                    findRoom(new Point(p.x - 1, p.y), roomInstance);
                }
            }
        }
    }

    @Override
    public MapData getMapData() {
        return mapData;
    }

    public void setMapData(MapData mapData) {
        this.mapData = mapData;
    }

    public void setKwdFile(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    /**
     * If you want to get notified about tile changes
     *
     * @param listener the listener
     */
    @Override
    public void addListener(MapListener listener) {
        listeners.add(listener);
    }

    /**
     * Stop listening to map updates
     *
     * @param listener the listener
     */
    @Override
    public void removeListener(MapListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
        List<MapTile> updatableTiles = new ArrayList<>();
        for (int x = (int) Math.max(0, start.x); x < Math.min(kwdFile.getMap().getWidth(), end.x + 1); x++) {
            for (int y = (int) Math.max(0, start.y); y < Math.min(kwdFile.getMap().getHeight(), end.y + 1); y++) {
                MapTile tile = getMapData().getTile(x, y);
                if (tile == null) {
                    continue;
                }
                Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
                if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                    continue;
                }
                tile.setSelected(select, playerId);
                updatableTiles.add(tile);
            }
        }
        //Point[] tiles = updatableTiles.toArray(new Point[updatableTiles.size()]);
        //mapLoader.updateTiles(tiles);

        // Notify
        notifyTileChange(updatableTiles);
    }

    @Override
    public boolean isSelected(int x, int y, short playerId) {
        MapTile tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }
        return tile.isSelected(playerId);
    }

    @Override
    public boolean isTaggable(int x, int y) {
        MapTile tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    @Override
    public boolean isBuildable(int x, int y, short playerId, short roomId) {
        MapTile tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }

        Room room = kwdFile.getRoomById(roomId);
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        // Ownable tile is needed for land building (and needs to be owned by us)
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)
                && tile.getOwnerId() == playerId) {
            return true;
        }

        // See if we are dealing with bridges
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER) && terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            return true;
        }
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA) && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isClaimable(int x, int y, short playerId) {
        MapTile tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }

        // See if the terrain is claimable at all
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        boolean claimable = false;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            if (tile.getOwnerId() != playerId) {
                claimable = true;
            }
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            if (tile.getOwnerId() != playerId) {
                claimable = true;
            }
        } else {
            claimable = (terrain.getMaxHealthTypeTerrainId() != terrain.getTerrainId());
        }

        // In order to claim, it needs to be adjacent to your own tiles
        if (claimable) {
            for (Point p : getSurroundingTiles(new Point(x, y), false)) {
                MapTile neighbourTile = getMapData().getTile(p);
                if (neighbourTile != null) {
                    Terrain neighbourTerrain = kwdFile.getTerrain(neighbourTile.getTerrainId());
                    if (neighbourTile.getOwnerId() == playerId && neighbourTerrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && !neighbourTerrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isSellable(int x, int y, short playerId) {
        MapTile tile = getMapData().getTile(x, y);
        Point p = new Point(x, y);
        if (tile.getOwnerId() == playerId && getRoomCoordinates().containsKey(p)) {

            // We own it, see if sellable
            RoomInstance instance = roomCoordinates.get(p);
            return instance.getRoom().getFlags().contains(Room.RoomFlag.BUILDABLE);
        }
        return false;
    }

    @Override
    public Point[] getSurroundingTiles(Point point, boolean diagonal) {

        // Get all surrounding tiles
        List<Point> tileCoords = new ArrayList<>(diagonal ? 9 : 5);
        tileCoords.add(point);

        addIfValidCoordinate(point.x, point.y - 1, tileCoords); // North
        addIfValidCoordinate(point.x + 1, point.y, tileCoords); // East
        addIfValidCoordinate(point.x, point.y + 1, tileCoords); // South
        addIfValidCoordinate(point.x - 1, point.y, tileCoords); // West
        if (diagonal) {
            addIfValidCoordinate(point.x - 1, point.y - 1, tileCoords); // NW
            addIfValidCoordinate(point.x + 1, point.y - 1, tileCoords); // NE
            addIfValidCoordinate(point.x - 1, point.y + 1, tileCoords); // SW
            addIfValidCoordinate(point.x + 1, point.y + 1, tileCoords); // SE
        }

        return tileCoords.toArray(new Point[tileCoords.size()]);
    }

    private void addIfValidCoordinate(final int x, final int y, List<Point> tileCoords) {
        MapTile tile = mapData.getTile(x, y);
        if (tile != null) {
            tileCoords.add(tile.getLocation());
        }
    }

    @Override
    public void setTiles(List<MapTile> tiles) {
        mapData.setTiles(tiles);
    }

    private void notifyTileChange(List<MapTile> updatedTiles) {
        for (MapListener mapListener : listeners.getArray()) {
            mapListener.onTilesChange(updatedTiles);
        }
    }

    @Override
    public Collection<IRoomController> getRoomControllers() {
        return roomControllers.values();
    }

    @Override
    public RoomInstance getRoomInstanceByCoordinates(Point p) {
        return roomCoordinates.get(p);
    }

    @Override
    public IRoomController getRoomController(RoomInstance roomInstance) {
        return roomControllers.get(roomInstance);
    }

    @Override
    public Map<Point, RoomInstance> getRoomCoordinates() {
        return roomCoordinates;
    }

    @Override
    public Map<RoomInstance, IRoomController> getRoomControllersByInstances() {
        return roomControllers;
    }

    @Override
    public void removeRoomInstances(RoomInstance... instances) {
        for (RoomInstance instance : instances) {

            // Signal the room
            IRoomController roomController = getRoomController(instance);
            roomController.destroy();

            roomControllers.remove(instance);
            for (Point p : instance.getCoordinates()) {
                roomCoordinates.remove(p);
            }
        }
    }

    /**
     * Get rooms by function.<br> FIXME: Should the player have ready lists?
     *
     * @param objectType the function
     * @param playerId the player id, can be null
     * @return list of rooms that match the criteria
     */
    @Override
    public List<IRoomController> getRoomsByFunction(ObjectType objectType, Short playerId) {
        List<IRoomController> roomsList = new ArrayList<>();
        for (Map.Entry<RoomInstance, IRoomController> entry : roomControllers.entrySet()) {
            if (playerId != null && entry.getKey().getOwnerId() != playerId) {
                continue;
            }
            if (entry.getValue().hasObjectControl(objectType)) {
                roomsList.add(entry.getValue());
            }
        }
        return roomsList;
    }

    @Override
    public void updateRooms(Point[] coordinates) {
        for (Point p : coordinates) {
            loadRoom(p);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(mapData, "mapData", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        mapData = (MapData) in.readSavable("mapData", null);
    }

}
