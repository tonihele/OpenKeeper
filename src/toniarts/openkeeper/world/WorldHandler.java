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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath.Segment;
import com.badlogic.gdx.math.Vector2;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.selection.SelectionArea;
import toniarts.openkeeper.world.creature.pathfinding.MapDistance;
import toniarts.openkeeper.world.creature.pathfinding.MapIndexedGraph;
import toniarts.openkeeper.world.creature.pathfinding.MapPathFinder;
import toniarts.openkeeper.world.room.GenericRoom;
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
    private final MapIndexedGraph pathFindingMap;
    private final MapPathFinder pathFinder;
    private final MapDistance heuristic;
    private final Node thingsNode;

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

        // For path finding
        pathFindingMap = new MapIndexedGraph(this, kwdFile);
        pathFinder = new MapPathFinder(pathFindingMap, false);
        heuristic = new MapDistance();

        // Things
        thingsNode = (Node) new ThingLoader(this).load(bulletAppState, assetManager, kwdFile);
        worldNode.attachChild(thingsNode);
    }

    public AssetManager getAssetManager() {
        return assetManager;
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

    public KwdFile getLevelData() {
        return this.kwdFile;
    }

    public MapLoader getMapLoader() {
        return this.mapLoader;
    }

    /**
     * Set some tiles selected/undelected
     *
     * @param selectionArea the selection area
     * @param select select or unselect
     */
    public void selectTiles(SelectionArea selectionArea, boolean select) {
        List<Point> updatableTiles = new ArrayList<>();
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getMap().getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getMap().getHeight(), selectionArea.getEnd().y + 1); y++) {
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
        TileData tile = mapLoader.getTile(x, y);
        if (tile == null) {
            return false;
        }
        return tile.isSelected();
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
        if (tile == null) {
            return false;
        }
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
        if (tile == null) {
            return false;
        }

        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());

        // Check that this is not already a room
        if (mapLoader.getRoomCoordinates().containsKey(new Point(x, y))) {
            return false;
        }

        // Ownable tile is needed for land building (and needs to be owned by us)
        if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND) && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && tile.getPlayerId() == player.getPlayerId()) {
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
            tile.setSelected(false);
            mapLoader.updateTiles(mapLoader.getSurroundingTiles(new Point(x, y), true));

            // See if room walls are allowed and does this touch any rooms
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS)) {
                List<RoomInstance> wallUpdatesNeeded = new ArrayList<>();
                for (Point p : mapLoader.getSurroundingTiles(new Point(x, y), false)) {
                    RoomInstance room = mapLoader.getRoomCoordinates().get(p);
                    if (room != null && room.getRoom().getFlags().contains(Room.RoomFlag.HAS_WALLS)) {
                        wallUpdatesNeeded.add(room);
                    }
                }
                if (!wallUpdatesNeeded.isEmpty()) {
                    mapLoader.updateRoomWalls(wallUpdatesNeeded);
                }
            }
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

            // tile is claimed by another player and needs to be destroyed
            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && tile.getPlayerId() != player.getPlayerId()) {
                tile.setTerrainId(terrain.getDestroyedTypeTerrainId());
            }

            terrain = kwdFile.getTerrain(tile.getTerrainId());
            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                tile.setPlayerId(player.getPlayerId());
            }
            mapLoader.updateTiles(mapLoader.getSurroundingTiles(new Point(x, y), true));
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
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getMap().getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getMap().getHeight(), selectionArea.getEnd().y + 1); y++) {

                // See that is this valid
                if (!isBuildable(x, y, player, room)) {
                    continue;
                }

                // Build
                TileData tile = mapLoader.getTile(x, y);
                tile.setPlayerId(player.getPlayerId());
                tile.setTerrainId(room.getTerrainId());

                Point p = new Point(x, y);
                buildPlots.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, false)));
                updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
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
                    updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
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
        for (int x = (int) Math.max(0, selectionArea.getStart().x); x < Math.min(kwdFile.getMap().getWidth(), selectionArea.getEnd().x + 1); x++) {
            for (int y = (int) Math.max(0, selectionArea.getStart().y); y < Math.min(kwdFile.getMap().getHeight(), selectionArea.getEnd().y + 1); y++) {

                // See that is this valid
                if (!isSellable(x, y, player)) {
                    continue;
                }

                // Sell
                TileData tile = mapLoader.getTile(x, y);
                Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
                if (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
                    tile.setTerrainId(kwdFile.getMap().getClaimedPath().getTerrainId());
                } else {

                    // Water or lava
                    if (tile.getFlag() == Tile.BridgeTerrainType.LAVA) {
                        tile.setTerrainId(kwdFile.getMap().getLava().getTerrainId());
                    } else {
                        tile.setTerrainId(kwdFile.getMap().getWater().getTerrainId());
                    }
                }

                // Get the instance
                Point p = new Point(x, y);
                soldInstances.add(mapLoader.getRoomCoordinates().get(p));
                updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
            }
        }

        // Remove the sold instances (will be regenerated) and add them to updatable
        for (RoomInstance roomInstance : soldInstances) {
            for (Point p : roomInstance.getCoordinates()) {
                updatableTiles.addAll(Arrays.asList(mapLoader.getSurroundingTiles(p, true)));
            }
        }
        mapLoader.removeRoomInstances(soldInstances.toArray(new RoomInstance[soldInstances.size()]));

        mapLoader.updateTiles(updatableTiles.toArray(new Point[updatableTiles.size()]));
    }

    /**
     * Plays a positional sound in an given tile
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param soundFile the sound file
     */
    public void playSoundAtTile(int x, int y, String soundFile) {

        // We might cache these per file? Then they would be persistent, just move them, does it matter?
        // Since creation of new objects and all, I don't know if they stay in the scene graph..
        AudioNode audio = new AudioNode(assetManager, ConversionUtils.getCanonicalAssetKey(AssetsConverter.SOUNDS_FOLDER + soundFile));
        audio.setPositional(true);
        audio.setReverbEnabled(false);
        audio.setLocalTranslation(x, 0, y);
        worldNode.attachChild(audio);
        audio.play();
    }

    /**
     * Get a random tile, that is not a starting tile
     *
     * @param start starting coordinates
     * @param radius radius, in tiles
     * @param creature
     * @return a random tile if one is found
     */
    public Point findRandomAccessibleTile(Point start, int radius, Creature creature) {
        List<Point> tiles = new ArrayList<>(radius * radius - 1);
        for (int y = start.y - radius / 2; y < start.y + radius / 2; y++) {
            for (int x = start.x - radius / 2; x < start.x + radius / 2; x++) {

                // Skip start tile
                if (x == start.x && y == start.y) {
                    continue;
                }

                TileData tile = getMapLoader().getTile(x, y);
                if (tile != null && isAccessible(tile, creature)) {
                    tiles.add(new Point(x, y));
                }
            }
        }

        // Take a random point
        if (!tiles.isEmpty()) {
            return Utils.getRandomItem(tiles);
        }
        return null;
    }

    /**
     * FIXME: This can NOT be. Just for quick easy testing.
     *
     * @param start start point
     * @param end end point
     * @param creature the creature to find path for
     * @return output path
     */
    public GraphPath<TileData> findPath(Point start, Point end, Creature creature) {
        pathFindingMap.setCreature(creature);
        GraphPath<TileData> outPath = new DefaultGraphPath<>();
        pathFinder.searchNodePath(getMapLoader().getTile(start.x, start.y), getMapLoader().getTile(end.x, end.y), heuristic, outPath);
        return outPath;
    }

    /**
     * Get tile coordinates from 3D coordinates
     *
     * @param location position
     * @return tile coordinates
     */
    public Point getTileCoordinates(Vector3f location) {
        return new Point((int) Math.floor(location.x + 0.5f), (int) Math.floor(location.z + 0.5f));
    }

    /**
     * Check if given tile is accessible by the given creature
     *
     * @param tile the tile
     * @param creature creature
     * @return is accessible
     */
    public boolean isAccessible(TileData tile, Creature creature) {
        Terrain terrain = tile.getTerrain();
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {

            // TODO: Rooms, obstacles and what not, should create an universal isAccessible(Creature) to map loader / world handler maybe
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

                // Get room obstacles
                RoomInstance roomInstance = getMapLoader().getRoomCoordinates().get(new Point(tile.getX(), tile.getY()));
                GenericRoom room = getMapLoader().getRoomActuals().get(roomInstance);
                return room.isTileAccessible(tile.getX(), tile.getY());
            } else if (creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY)) {
                return true;
            } else if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA) && !creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA)) {
                return false;
            } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER) && !creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_WATER)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Debug drawing of path
     *
     * @param linePath
     */
    public void drawPath(LinePath<Vector2> linePath) {
        for (Segment<Vector2> segment : linePath.getSegments()) {

            Line line = new Line(new Vector3f(segment.getBegin().x, 0.25f, segment.getBegin().y), new Vector3f(segment.getEnd().x, 0.25f, segment.getEnd().y));
            line.setLineWidth(2);
            Geometry geometry = new Geometry("Bullet", line);
            Material orange = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            orange.setColor("Color", ColorRGBA.Red);
            orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            geometry.setCullHint(Spatial.CullHint.Never);
            geometry.setMaterial(orange);
            getWorld().attachChild(geometry);
        }
    }

    public Node getThingsNode() {
        return thingsNode;
    }

}
