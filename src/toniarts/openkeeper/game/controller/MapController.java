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
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * This is controller for the map related functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapController implements Savable, IMapController {

    private MapData mapData;
    private KwdFile kwdFile;
    private final List<MapListener> listeners = new ArrayList<>();

    public MapController() {
        // For serialization
    }

    /**
     * Load map data from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     */
    public MapController(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
        this.mapData = new MapData(kwdFile);

        // Load rooms
        loadRooms();
    }

    /**
     * Instantiate a map controller from map data (loaded game)
     *
     * @param mapData the map data
     * @param kwdFile the KWD file
     */
    public MapController(MapData mapData, KwdFile kwdFile) {
        this.mapData = mapData;
        this.kwdFile = kwdFile;
    }

    private void loadRooms() {

        // Go through the tiles and detect any rooms
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                MapTile mapTile = mapData.getTile(x, y);
                if (kwdFile.getTerrain(mapTile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                    loadRoom(x, y);
                }
            }
        }
    }

    private void loadRoom(int x, int y) {

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
    public void addListener(MapListener listener) {
        listeners.add(listener);
    }

    /**
     * Stop listening to map updates
     *
     * @param listener the listener
     */
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
    public boolean isBuildable(int x, int y, Player player, Room room) {
        MapTile tile = getMapData().getTile(x, y);
        if (tile == null) {
            return false;
        }

        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        // Ownable tile is needed for land building (and needs to be owned by us)
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)
                && tile.getOwnerId() == player.getPlayerId()) {
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
        for (MapListener mapListener : listeners) {
            mapListener.onTilesChange(updatedTiles);
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
