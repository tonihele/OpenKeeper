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
package toniarts.openkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.world.room.RoomInstance;

/**
 * Handles the handling of game world, physics & visual wise
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class WorldHandler {

    private final MapLoader mapLoader;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final Node worldNode;

    public WorldHandler(final AssetManager assetManager, final KwdFile kwdFile, final BulletAppState bulletAppState) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;

        // World node
        worldNode = new Node("World");

        // Create the actual map
        this.mapLoader = new MapLoader(assetManager, kwdFile) {
            @Override
            protected void updateProgress(int progress, int max) {
                WorldHandler.this.updateProgress(progress, max);
            }
        };
        worldNode.attachChild(mapLoader.load(assetManager, kwdFile));

        // Things
        worldNode.attachChild(new ThingLoader().load(bulletAppState, assetManager, kwdFile));
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress
     * @param max max progress
     */
    protected abstract void updateProgress(int progress, int max);

    /**
     * Get the world node
     *
     * @return world node
     */
    public Node getWorld() {
        return worldNode;
    }

    /**
     * Set some tiles selected/undelected
     *
     * @param selectionArea the selection area
     * @param select select or unselect
     */
    public void selectTiles(SelectionArea selectionArea, boolean select) {
        List<Point> updatableTiles = new ArrayList<>();
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getHeight(), selectionArea.getEnd().y + 1); y++) {
                TileData tile = mapLoader.getTile(x, y);
                Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
                if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                    continue;
                }
                tile.setSelected(select);
                updatableTiles.add(new Point(x, y));
            }
        }
        mapLoader.updateTiles(updatableTiles.toArray(new Point[updatableTiles.size()]));
    }

    /**
     * Determine if a tile at x & y is selected or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selected
     */
    public boolean isSelected(int x, int y) {
        return mapLoader.getTile(x, y).isSelected();
    }

    /**
     * Determine if a tile at x & y is selectable or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selectable
     */
    public boolean isTaggable(int x, int y) {
        TileData tile = mapLoader.getTile(x, y);
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    /**
     * Determine if a tile at x & y is buildable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player
     * @param room the room to be build
     * @return is the tile buildable
     */
    public boolean isBuildable(int x, int y, Player player, Room room) {
        TileData tile = mapLoader.getTile(x, y);
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        // Ownable tile is needed for land building (and needs to be owned by us)
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND) && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && tile.getPlayerId() == player.getPlayerId()) {
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

    /**
     * Determine if a tile (maybe a room) at x & y is claimable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player
     * @return is the tile claimable by you
     */
    public boolean isClaimable(int x, int y, Player player) {
        TileData tile = mapLoader.getTile(x, y);
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        // Claimed & not owned by us
        if (tile.getPlayerId() != player.getPlayerId() && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            return true;
        }
        // i.e. Dirt path, not water nor lava, not taggable
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.LAVA) && !terrain.getFlags().contains(Terrain.TerrainFlag.WATER) && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return true;
        }

        return false;
    }

    /**
     * Dig a tile at x & y
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void digTile(int x, int y) {
        if (isTaggable(x, y)) {
            TileData tile = mapLoader.getTile(x, y);
            Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
            tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
            mapLoader.updateTiles(getSurroundingTiles(new Point(x, y), true));
        }
    }

    /**
     * Claim a tile at x & y to the player's name
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player, the new owner
     */
    public void claimTile(int x, int y, Player player) {
        if (isClaimable(x, y, player)) {
            TileData tile = mapLoader.getTile(x, y);
            Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

            // TODO: Claim a room
            // Claim
            if (!terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                tile.setTerrainId(terrain.getMaxHealthTypeTerrainId());

            }
            terrain = kwdFile.getTerrain(tile.getTerrainId());
            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                tile.setPlayerId(player.getPlayerId());
            }
            mapLoader.updateTiles(getSurroundingTiles(new Point(x, y), true));
        }
    }

    private Point[] getSurroundingTiles(Point point, boolean diagonal) {

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
        if ((x >= 0 && x < kwdFile.getWidth() && y >= 0 && y < kwdFile.getHeight())) {
            tileCoords.add(new Point(x, y));
        }
    }

    /**
     * Build a building to the wanted area
     *
     * @param selectionArea the selection area
     * @param player the player, the new owner
     * @param room room to build
     */
    public void build(SelectionArea selectionArea, Player player, Room room) {
        Set<Point> updatableTiles = new HashSet<>();
        Set<Point> buildPlots = new HashSet<>();
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getHeight(), selectionArea.getEnd().y + 1); y++) {

                // See that is this valid
                if (!isBuildable(x, y, player, room)) {
                    continue;
                }

                // Build
                TileData tile = mapLoader.getTile(x, y);
                tile.setPlayerId(player.getPlayerId());
                tile.setTerrainId(room.getTerrainId());

                Point p = new Point(x, y);
                buildPlots.addAll(Arrays.asList(getSurroundingTiles(p, false)));
                updatableTiles.addAll(Arrays.asList(getSurroundingTiles(p, true)));
            }
        }

        // See if we hit any of the adjacent rooms
        Set<RoomInstance> adjacentInstances = new HashSet<>();
        for (Point p : buildPlots) {
            if (mapLoader.getRoomCoordinates().containsKey(p) && mapLoader.getRoomCoordinates().get(p).getRoom().equals(room)) {

                // Same room, see that we own it
                TileData tile = mapLoader.getTile(p.x, p.y);
                if (tile.getPlayerId() == player.getPlayerId()) {

                    // Bingo!
                    adjacentInstances.add(mapLoader.getRoomCoordinates().get(p));
                }
            }
        }

        // If any hits, merge, and update whole room
        if (!adjacentInstances.isEmpty()) {

            // Add the mergeable rooms to updatable tiles as well
            for (RoomInstance instance : adjacentInstances) {
                for (Point p : instance.getCoordinates()) {
                    updatableTiles.addAll(Arrays.asList(getSurroundingTiles(p, true)));
                }
            }

            // Remove all the room instances (they will be regenerated)
            mapLoader.removeRoomInstances(adjacentInstances.toArray(new RoomInstance[adjacentInstances.size()]));
        }

        mapLoader.updateTiles(updatableTiles.toArray(new Point[updatableTiles.size()]));
    }

    /**
     * Is the tile (building) sellable by us
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player, the seller
     */
    public boolean isSellable(int x, int y, Player player) {
        TileData tile = mapLoader.getTile(x, y);
        Point p = new Point(x, y);
        if (tile.getPlayerId() == player.getPlayerId() && mapLoader.getRoomCoordinates().containsKey(p)) {

            // We own it, see if sellable
            RoomInstance instance = mapLoader.getRoomCoordinates().get(p);
            return instance.getRoom().getFlags().contains(Room.RoomFlag.BUILDABLE);
        }
        return false;
    }

    /**
     * Sell building(s) from the wanted area
     *
     * @param selectionArea the selection area
     * @param player the player, the new owner
     */
    public void sell(SelectionArea selectionArea, Player player) {
        Set<Point> updatableTiles = new HashSet<>();
        Set<RoomInstance> soldInstances = new HashSet<>();
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getHeight(), selectionArea.getEnd().y + 1); y++) {

                // See that is this valid
                if (!isSellable(x, y, player)) {
                    continue;
                }

                // Sell
                TileData tile = mapLoader.getTile(x, y);
                tile.setTerrainId(kwdFile.getClaimedPath().getTerrainId());

                // Get the instance
                Point p = new Point(x, y);
                soldInstances.add(mapLoader.getRoomCoordinates().get(p));
                updatableTiles.addAll(Arrays.asList(getSurroundingTiles(p, true)));
            }
        }

        // Remove the sold instances (will be regenerated) and add them to updatable
        for (RoomInstance roomInstance : soldInstances) {
            for (Point p : roomInstance.getCoordinates()) {
                updatableTiles.addAll(Arrays.asList(getSurroundingTiles(p, true)));
            }
        }
        mapLoader.removeRoomInstances(soldInstances.toArray(new RoomInstance[soldInstances.size()]));

        mapLoader.updateTiles(updatableTiles.toArray(new Point[updatableTiles.size()]));
    }
}
