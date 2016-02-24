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
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.control.FlashTileControl;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomConstructor;
import toniarts.openkeeper.world.room.RoomInstance;
import toniarts.openkeeper.world.room.WallSection;
import toniarts.openkeeper.world.room.WallSection.WallDirection;
import toniarts.openkeeper.world.terrain.Water;

/**
 * Loads whole maps, and handles the maps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class MapLoader implements ILoader<KwdFile> {

    public final static float TILE_WIDTH = 1;
    public final static float TILE_HEIGHT = 1;

    private final static int PAGE_SQUARE_SIZE = 8; // Divide the terrain to square "pages"
    private List<Node> pages;
    private final KwdFile kwdFile;
    private Node map;
    private final MapData mapData;
    private final AssetManager assetManager;
    private Node roomsNode;
    private final List<RoomInstance> rooms = new ArrayList<>(); // The list of rooms
    private final List<EntityInstance<Terrain>> waterBatches = new ArrayList<>(); // Lakes and rivers
    private final List<EntityInstance<Terrain>> lavaBatches = new ArrayList<>(); // Lakes and rivers, but hot
    private final HashMap<Point, RoomInstance> roomCoordinates = new HashMap<>(); // A quick glimpse whether room at specific coordinates is already "found"
    private final HashMap<RoomInstance, Spatial> roomNodes = new HashMap<>(); // Room instances by node
    private final HashMap<RoomInstance, GenericRoom> roomActuals = new HashMap<>(); // Rooms by room instance
    private final HashMap<Point, EntityInstance<Terrain>> terrainBatchCoordinates = new HashMap<>(); // A quick glimpse whether terrain batch at specific coordinates is already "found"
    private static final HashMap<String, Spatial> cachedModels = new HashMap<>();
    private static final Logger logger = Logger.getLogger(MapLoader.class.getName());

    public MapLoader(AssetManager assetManager, KwdFile kwdFile) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;

        // Create modifiable tiles
        mapData = new MapData(kwdFile);
    }

    @Override
    public Spatial load(AssetManager assetManager, KwdFile object) {

        //Create a root
        map = new Node("Map");
        Node terrain = new Node("Terrain");
        generatePages(terrain);
        roomsNode = new Node("Rooms");
        terrain.attachChild(roomsNode);

        // Go through the map
        int tilesCount = mapData.getWidth() * object.getMap().getHeight();

        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {

                try {
                    handleTile(mapData.getTile(x, y), terrain);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to handle tile at " + x + ", " + y + "!", e);
                }

                // Update progress
                updateProgress(y * object.getMap().getWidth() + x + 1, tilesCount);
            }
        }

        // Batch the terrain pages
        for (Node page : pages) {
            ((BatchNode) page.getChild(0)).batch();
            ((BatchNode) page.getChild(1)).batch();
            ((BatchNode) page.getChild(2)).batch();
        }
        map.attachChild(terrain);

        // Create the water
        if (!waterBatches.isEmpty()) {
            map.attachChild(Water.construct(assetManager, waterBatches));
        }

        // And the lava
        if (!lavaBatches.isEmpty()) {
            map.attachChild(Water.construct(assetManager, lavaBatches));
        }

        return map;
    }

    public MapData getMapData() {
        return mapData;
    }

    /**
     * Update the selected tiles (and neighbouring tiles if needed)
     *
     * @param mapNode the map node
     * @param mapData the actual map data
     * @param updatableTiles list of tiles to update
     */
    protected void updateTiles(Point... points) {
        
        // Reconstruct all tiles in the area
        Set<BatchNode> nodesNeedBatching = new HashSet<>();
        Node terrainNode = (Node) map.getChild(0);
        for (Point point : points) {

            // Reconstruct and mark for patching
            // The tile node needs to created anew, somehow the BatchNode just doesn't get it if I remove children from subnode
            Node pageNode = getPageNode(point, terrainNode);
            Node tileNode = getTileNode(point, (Node) pageNode.getChild(0));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(0)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(0));
            }
            tileNode = getTileNode(point, (Node) pageNode.getChild(1));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(1)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(1));
            }
            tileNode = getTileNode(point, (Node) pageNode.getChild(2));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(2)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(2));
            }

            // Reconstruct
            handleTile(mapData.getTile(point), (Node) map.getChild(0));
        }

        // Batch
        for (BatchNode batchNode : nodesNeedBatching) {
            batchNode.batch();
        }
    }

    private void setTaggedMaterialToGeometries(final Node node) {

        // Change the material on geometries
        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {

                    Material material = ((Geometry) spatial).getMaterial();
                    material.setColor("Ambient", new ColorRGBA(0, 0, 0.8f, 1));
                    material.setBoolean("UseMaterialColors", true);
                }
            }
        });
    }

    /**
     * Generate the page nodes
     *
     * @param root where to generate pages on
     */
    private void generatePages(Node root) {
        pages = new ArrayList<>(((int) Math.ceil(mapData.getHeight() / (float) PAGE_SQUARE_SIZE))
                * ((int) Math.ceil(mapData.getWidth() / (float) PAGE_SQUARE_SIZE)));
        for (int y = 0; y < (int) Math.ceil(mapData.getHeight() / (float) PAGE_SQUARE_SIZE); y++) {
            for (int x = 0; x < (int) Math.ceil(mapData.getWidth() / (float) PAGE_SQUARE_SIZE); x++) {
                Node page = new Node(x + "_" + y);

                // Create batch nodes for ceiling, floor and walls
                BatchNode floor = new BatchNode("floor");
                floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Floors don't cast
                generateTileNodes(floor, x, y);
                page.attachChild(floor);
                BatchNode wall = new BatchNode("wall");
                wall.setShadowMode(RenderQueue.ShadowMode.CastAndReceive); // Walls cast and receive shadows
                generateTileNodes(wall, x, y);
                page.attachChild(wall);
                BatchNode ceiling = new BatchNode("ceiling");
                ceiling.setShadowMode(RenderQueue.ShadowMode.Off); // No lights above ceilings
                generateTileNodes(ceiling, x, y);
                page.attachChild(ceiling);

                pages.add(page);
                root.attachChild(page);
            }
        }
    }

    /**
     * Create tile nodes inside a page
     *
     * @param pageBatch the page
     * @param pageX page x
     * @param pageY page y
     */
    private void generateTileNodes(BatchNode pageBatch, int pageX, int pageY) {
        for (int y = 0; y < PAGE_SQUARE_SIZE; y++) {
            for (int x = 0; x < PAGE_SQUARE_SIZE; x++) {
                pageBatch.attachChild(new Node((x + pageX * PAGE_SQUARE_SIZE) + "_" + (y + pageY * PAGE_SQUARE_SIZE)));
            }
        }
    }

    private Spatial getWallSpatial(TileData tile, WallDirection direction) {
        String modelName = tile.getTerrain().getSideResource().getName();
        Point p = tile.getLocation();
        TileData neigbourTile;
        switch (direction) {
            case NORTH:
                neigbourTile = mapData.getTile(p.x, p.y - 1);
                break;
            case SOUTH:
                neigbourTile = mapData.getTile(p.x, p.y + 1);
                break;
            case EAST:
                neigbourTile = mapData.getTile(p.x + 1, p.y);
                break;
            default: // WEST
                neigbourTile = mapData.getTile(p.x - 1, p.y);
                break;
        }
        // Check for out of bounds
        if (neigbourTile == null) {
            return loadModel(modelName, true);
        }

        if (neigbourTile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return null;
        }

        if (!(tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS))) {
            return loadModel(modelName, true);
        } else if (hasRoomWalls(neigbourTile)) {
            return getRoomWall(neigbourTile, direction);
        }

        return loadModel(modelName, true);
    }

    private Spatial getRoomWall(TileData tile, WallDirection direction) {
        Point p = tile.getLocation();
        Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
        RoomInstance roomInstance = handleRoom(p, room);
        GenericRoom gr = roomActuals.get(roomInstance);
        return gr.getWallSpatial(p, direction);
    }

    private Spatial transformWall(Spatial wall, int x, int y, float yAngle) {

        // Move the ceiling to a correct tile
        if (wall != null) {
            if (yAngle != 0) {
                wall.rotate(0, yAngle, 0);
            }
            wall.move(x * TILE_WIDTH, 0, y * TILE_WIDTH);           
        }
        return wall;
    }

    /**
     * Sets random material (from the list) to all the geometries that have been
     * tagged for this in this spatial
     *
     * @param assetManager the asset manager
     * @param spatial the spatial
     */
    private void setRandomTexture(final AssetManager assetManager, final Spatial spatial, final TileData tile) {

        // Check the data on geometry
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                Integer texCount = spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES_COUNT);
                if (texCount != null) {

                    // On redrawing, see if we already randomized this
                    // The principle is bit wrong, the random texture is tied to the tile, and not material etc.
                    // But it is probably just the tops of few tiles, so...
                    int tex;
                    if (tile.getRandomTextureIndex() != null) {
                        tex = tile.getRandomTextureIndex();
                    } else {
                        tex = FastMath.rand.nextInt(texCount);
                        tile.setRandomTextureIndex(tex);
                    }
                    if (tex != 0) { // 0 is the default anyway
                        Geometry g = (Geometry) spatial;
                        Material m = g.getMaterial();
                        String asset = m.getAssetName();

                        // Load new material
                        Material newMaterial = assetManager.loadMaterial(asset.substring(0,
                                asset.lastIndexOf(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR) + 1).concat(tex + ".j3m"));
                        g.setMaterial(newMaterial);
                    }
                }
            }
        });
    }

    /**
     * Loads the given asset and resets its scale and translation to match our
     * give grid
     *
     * @param assetManager the asset manager
     * @param asset the name and location of the asset (asset key)
     * @param wall is this wall? Used to distinquish between sea levels and
     * walls
     * @return the asset loaded & ready to rock
     */
    public static Spatial loadAsset(final AssetManager assetManager, final String asset, final boolean wall) {
        Spatial spatial = assetManager.loadModel(asset);

        // Set the transform and scale to our scale and 0 the transform
        spatial.breadthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Node && spatial.getParent() != null) {
                    Node n = (Node) spatial;

                    // "Reset"
                    n.setLocalTranslation(0, 0, 0);
                    n.setLocalScale(1f);

                    // Set the translation so that everything moves similarly
                    BoundingBox worldBound = (BoundingBox) n.getWorldBound();
                    Vector3f boundCenter = worldBound.getCenter();
                    n.setLocalTranslation(0 - boundCenter.x - worldBound.getXExtent(), 0 - boundCenter.y - (wall ? -worldBound.getYExtent() : worldBound.getYExtent()), 0 - boundCenter.z - worldBound.getZExtent());
                }
            }
        });
        return spatial;
    }

    private Spatial loadModel(final String model, final boolean wall) {

        if (!cachedModels.containsKey(model)) {
            String asset = AssetsConverter.MODELS_FOLDER + "/" + model + ".j3o";
            Spatial spatial = loadAsset(assetManager, asset, wall);
            cachedModels.put(model, spatial);
        }
        //return spatial;
        return cachedModels.get(model).deepClone();
    }

    /**
     * Handle single tile from the map, represented by the X & Y coordinates
     *
     * @param tiles the whole set of tile (for neighbours etc.)
     * @param x the tile X coordinate
     * @param y the tile Y coordinate
     * @param assetManager the asset manager instance
     * @param root the root node
     */
    private void handleTile(TileData tile, Node root) {
        // Get the terrain
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        Point p = tile.getLocation();
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            // Construct the actual room
            Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
            handleRoom(p, room);

            // Swap the terrain if this is a bridge
            terrain = kwdFile.getTerrainBridge(tile.getFlag(), room);
            if (terrain == null) {
                return;
            }
        }

        Node pageNode = getPageNode(p, root);
        handleTop(tile, pageNode);
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            handleSide(tile, pageNode);
        }
    }

    private RoomInstance handleRoom(Point p, Room room) {
        if (roomCoordinates.containsKey(p)) {
            RoomInstance roomInstance = roomCoordinates.get(p);
            updateRoomWalls(roomInstance);
            return roomInstance;
        }

        RoomInstance roomInstance = new RoomInstance(room);
        findRoom(p, roomInstance);
        findRoomWallSections(roomInstance);
        rooms.add(roomInstance);

        Spatial roomNode = handleRoom(roomInstance);
        roomsNode.attachChild(roomNode);

        // Add to registry
        roomNodes.put(roomInstance, roomNode);
        return roomInstance;
    }

    /**
     * Handle top construction on the tile
     *
     * @param terrain the terrain tile
     * @param tiles all the tiles
     * @param tile this tile
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param assetManager the asset manager instance
     * @param pageNode page node
     */
    private void handleTop(TileData tile, Node pageNode) {

        Terrain terrain = tile.getTerrain();
        ArtResource model = terrain.getCompleteResource();
        Point p = tile.getLocation();
        Spatial spatial;
        // For water construction type (lava & water), there are 8 pieces (0-7 suffix) in complete resource
        // And in the top resource there is the actual lava/water
        if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {

            // Store the batch instance
            if (!terrainBatchCoordinates.containsKey(p)) {
                EntityInstance<Terrain> entityInstance = new EntityInstance<>(terrain);
                findTerrainBatch(p, entityInstance);
                if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                    lavaBatches.add(entityInstance);
                } else {
                    waterBatches.add(entityInstance);
                }
            }

            spatial = new WaterConstructor(kwdFile).construct(mapData.getTiles(), p.x, p.y, terrain, assetManager, model.getName());

        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
            // If this resource is type quad, parse it together. With fixed Hero Lair
            String modelName = (model == null && terrain.getTerrainId() == 35) ? "hero_outpost_floor" : model.getName();
            spatial = new QuadConstructor(kwdFile).construct(mapData.getTiles(), p.x, p.y, terrain, assetManager, modelName);

        } else {

            if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                model = terrain.getTopResource();
            }
            spatial = loadModel(model.getName(), false);
        }

        if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
            setRandomTexture(assetManager, spatial, tile);
        }

        Node topTileNode;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            topTileNode = getTileNode(p, (Node) pageNode.getChild(2));
            spatial.setLocalTranslation(0, TILE_HEIGHT, 0);

        } else {
            topTileNode = getTileNode(p, (Node) pageNode.getChild(0));
            spatial.setLocalTranslation(0, 0, 0);
        }
        topTileNode.attachChild(spatial);
        if (tile.isSelected()) { // Just set the selected material here if needed
            setTaggedMaterialToGeometries(topTileNode);
        }
        topTileNode.move(p.x * TILE_WIDTH, 0, p.y * TILE_WIDTH);
    }

    private void handleSide(TileData tile, Node pageNode) {
        Point p = tile.getLocation();
        Node sideTileNode = getTileNode(p, (Node) pageNode.getChild(1));
        

        // North
        Spatial wall = getWallSpatial(tile, WallDirection.NORTH);
        if (wall != null) {
            sideTileNode.attachChild(transformWall(wall, 0, -1, 0));
        }

        // South
        wall = getWallSpatial(tile, WallDirection.SOUTH);
        if (wall != null) {
            sideTileNode.attachChild(transformWall(wall, -1, 0, FastMath.PI));
        }

        // East
        wall = getWallSpatial(tile, WallDirection.EAST);
        if (wall != null) {
            sideTileNode.attachChild(transformWall(wall, 0, 0, -FastMath.HALF_PI));
        }

        // West
        wall = getWallSpatial(tile, WallDirection.WEST);
        if (wall != null) {
            sideTileNode.attachChild(transformWall(wall, -1, -1, FastMath.HALF_PI));
        }

        if (tile.isSelected()) {
            setTaggedMaterialToGeometries(sideTileNode);
        }

        sideTileNode.setLocalTranslation(p.x * TILE_WIDTH, 0, p.y * TILE_WIDTH);
    }

    public void flashTile(int x, int y, int time, boolean enabled) {

        Point p = new Point(x, y);
        Node terrainNode = (Node) map.getChild(0);
        Node pageNode = getPageNode(p, terrainNode);

        Node tileNode = getTileNode(p, (Node) pageNode.getChild(0));
        if (tileNode != null) {
            if (enabled) {
                tileNode.addControl(new FlashTileControl(time));
            } else {
                tileNode.removeControl(FlashTileControl.class);
            }
        }

        tileNode = getTileNode(p, (Node) pageNode.getChild(1));
        if (tileNode != null) {
            if (enabled) {
                tileNode.addControl(new FlashTileControl(time));
            } else {
                tileNode.removeControl(FlashTileControl.class);
            }
        }

        tileNode = getTileNode(p, (Node) pageNode.getChild(2));
        if (tileNode != null) {
            if (enabled) {
                tileNode.addControl(new FlashTileControl(time));
            } else {
                tileNode.removeControl(FlashTileControl.class);
            }
        }
    }

    /**
     * Get the terrain tile node
     *
     * @param x the tile x
     * @param y the tile y
     * @param root the page node
     * @return tile node
     */
    private Node getTileNode(Point p, Node page) {
        return (Node) page.getChild(getTileNodeIndex(p));
    }

    /**
     * Get index for the tile node, where it should be
     *
     * @param x the tile x
     * @param y the tile y
     * @return the index inside the page
     */
    private int getTileNodeIndex(Point p) {
        int tileX = p.x - ((int) Math.floor(p.x / (float) PAGE_SQUARE_SIZE)) * PAGE_SQUARE_SIZE;
        int tileY = p.y - ((int) Math.floor(p.y / (float) PAGE_SQUARE_SIZE)) * PAGE_SQUARE_SIZE;
        return tileY * PAGE_SQUARE_SIZE + tileX;
    }

    /**
     * Get the terrain "page" we are on
     *
     * @param x the tile x
     * @param y the tile y
     * @param root the root node
     * @return page node
     */
    protected Node getPageNode(Point p, Node root) {
        int pageX = (int) Math.floor(p.x / (float) PAGE_SQUARE_SIZE);
        int pageY = (int) Math.floor(p.y / (float) PAGE_SQUARE_SIZE);

        // Get the page index
        int index = pageX;
        if (pageY > 0) {
            int pagesPerRow = (int) Math.ceil(mapData.getWidth() / (float) PAGE_SQUARE_SIZE);
            index += pagesPerRow * pageY;
        }
        return (Node) root.getChild(index);
    }

    /**
     * Checks if this terrain piece is actually a room and the room type has
     * walls
     *
     * @param terrain the terrain piece
     * @return true if this is a room and it has its own walls
     */
    private boolean hasRoomWalls(TileData tile) {
        Terrain terrain = tile.getTerrain();
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
            return hasRoomWalls(room);
        }
        return false;
    }

    public static boolean hasRoomWalls(Room room) {
        return room.getFlags().contains(Room.RoomFlag.HAS_WALLS)
                || room.getTileConstruction() == Room.TileConstruction.HERO_GATE_FRONT_END
                || room.getTileConstruction() == Room.TileConstruction.HERO_GATE_3_BY_1;
    }

    /**
     * Find the room starting from a certain point, rooms are never diagonally
     * attached
     *
     * @param tiles the tiles
     * @param p starting point
     * @param roomInstance the room instance
     */
    private void findRoom(Point p, RoomInstance roomInstance) {
        TileData tile = mapData.getTile(p);
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

    /**
     * Find a terrain batch starting from a certain point, they are never
     * diagonally attached
     *
     * @param tiles the tiles
     * @param p starting point
     * @param entityInstance the batch instance
     */
    private void findTerrainBatch(Point p, EntityInstance<Terrain> entityInstance) {
        TileData tile = mapData.getTile(p);

        if (!terrainBatchCoordinates.containsKey(p)) {

            // Get the terrain
            Terrain terrain = tile.getTerrain();
            Terrain bridgeTerrain = kwdFile.getTerrainBridge(tile.getFlag(), terrain);
            if (bridgeTerrain != null) {
                terrain = bridgeTerrain;
            }

            if (entityInstance.getEntity().equals(terrain)) {

                // Add the coordinate
                terrainBatchCoordinates.put(p, entityInstance);
                entityInstance.addCoordinate(p);

                // Find north
                findTerrainBatch(new Point(p.x, p.y - 1), entityInstance);

                // Find east
                findTerrainBatch(new Point(p.x + 1, p.y), entityInstance);

                // Find south
                findTerrainBatch(new Point(p.x, p.y + 1), entityInstance);

                // Find west
                findTerrainBatch(new Point(p.x - 1, p.y), entityInstance);
            }
        }
    }

    /**
     * Constructs the given room
     *
     * @param assetManager the asset manager instance
     * @param roomInstance the room instance
     */
    private Spatial handleRoom(RoomInstance roomInstance) {
        GenericRoom room = RoomConstructor.constructRoom(roomInstance, assetManager, kwdFile);
        roomActuals.put(roomInstance, room);
        updateRoomWalls(roomInstance);
        return room.construct();
    }

    /**
     * Get a standard camera position vector on given map point
     *
     * @param x tile x coordinate
     * @param y tile y coordinate
     * @return camera location
     */
    public static Vector3f getCameraPositionOnMapPoint(final int x, final int y) {
        return new Vector3f((x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2),
                0f,
                (y * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2));
    }

    /**
     * Sets material lighting accorting to the terrain setting
     *
     * @param material the material to adjust
     * @param terrain the terrain data
     */
    public static void setTerrainMaterialLighting(Material material, Terrain terrain) {

        // Ambient light
        if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_LIGHT)) {
            Color c = terrain.getAmbientLight();
            int r = c.getRed();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_RED)) {
                r += 256;
            }
            int g = c.getGreen();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_GREEN)) {
                g += 256;
            }
            int b = c.getBlue();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_BLUE)) {
                b += 256;
            }
            material.setColor("Ambient", new ColorRGBA(r / 255f, g / 255f, b / 255f, 0));
        }

        // Not sure what the terrain light is supposed to be
        if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_LIGHT)) {
            Color c = terrain.getTerrainLight();
            int r = c.getRed();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_RED)) {
                r += 256;
            }
            int g = c.getGreen();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_GREEN)) {
                g += 256;
            }
            int b = c.getBlue();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_BLUE)) {
                b += 256;
            }
            material.setColor("Specular", new ColorRGBA(r / 255f, g / 255f, b / 255f, 0));
        }
        material.setColor("Diffuse", ColorRGBA.White);
        material.setBoolean("UseMaterialColors", false); // Hmm...
    }

    /**
     * Get coordinate / room instance mapping
     *
     * @return mapping
     */
    public HashMap<Point, RoomInstance> getRoomCoordinates() {
        return roomCoordinates;
    }

    /**
     * Get list of room instances
     *
     * @return room instances
     */
    protected List<RoomInstance> getRooms() {
        return rooms;
    }

    /**
     * Remove all the given room instances (and actually removes them from the
     * scene)
     *
     * @param instances room instances to remove
     */
    protected void removeRoomInstances(RoomInstance... instances) {
        for (RoomInstance instance : instances) {
            roomsNode.detachChild(roomNodes.get(instance));
            roomNodes.remove(instance);
            rooms.remove(instance);
            roomActuals.remove(instance);
            for (Point p : instance.getCoordinates()) {
                roomCoordinates.remove(p);
            }
        }
    }

    /**
     * Find room wall sections, continuous sections facing the same way
     *
     * @param tiles tiles
     * @param roomInstance room instance
     */
    private void findRoomWallSections(RoomInstance roomInstance) {
        if (hasRoomWalls(roomInstance.getRoom())) {
            List<WallSection> sections = new ArrayList<>();
            Map<Point, Set<WallDirection>> alreadyWalledPoints = new HashMap<>();
            for (Point p : roomInstance.getCoordinates()) {

                // Traverse in all four directions to find wallable sections facing the same direction
                traverseRoomWalls(p, roomInstance, WallDirection.NORTH, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.EAST, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.SOUTH, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.WEST, sections, alreadyWalledPoints);
            }
            roomInstance.setWallSections(sections);
        }
    }

    /**
     * Traverses room walls facing in certain direction from a room point. Finds
     * a section.
     *
     * @param p starting room point
     * @param tiles tiles
     * @param roomInstance the room instance of which walls we need to find
     * @param direction the direction of which the want the walls to face
     * @param sections list of already find sections
     * @param alreadyWalledPoints already found wall points
     */
    private void traverseRoomWalls(Point p, RoomInstance roomInstance, WallDirection direction,
            List<WallSection> sections, Map<Point, Set<WallDirection>> alreadyWalledPoints) {

        Set<WallDirection> alreadyTraversedDirections = alreadyWalledPoints.get(p);
        if (alreadyTraversedDirections == null || !alreadyTraversedDirections.contains(direction)) {

            List<Point> section = getRoomWalls(p, roomInstance, direction);
            if (section != null) {

                // Add section
                sections.add(new WallSection(direction, section));

                // Add to known points
                for (Point sectionPoint : section) {
                    Set<WallDirection> directions = alreadyWalledPoints.get(sectionPoint);
                    if (directions == null) {
                        directions = EnumSet.noneOf(WallDirection.class);
                    }
                    directions.add(direction);
                    alreadyWalledPoints.put(sectionPoint, directions);
                }
            }
        }
    }

    private List<Point> getRoomWalls(Point p, RoomInstance roomInstance, WallDirection wallDirection) {

        // See if the starting point has a wall to the given direction
        TileData tile;
        if (wallDirection == WallDirection.NORTH) {
            tile = mapData.getTile(p.x, p.y + 1);
        } else if (wallDirection == WallDirection.EAST) {
            tile = mapData.getTile(p.x - 1, p.y);
        } else if (wallDirection == WallDirection.SOUTH) {
            tile = mapData.getTile(p.x, p.y - 1);
        } else {
            tile = mapData.getTile(p.x + 1, p.y); // West
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS)) {

            // Found wallable
            List<Point> wallPoints = new ArrayList<>();
            wallPoints.add(p);

            // Traverse possible directions, well one direction, "to the right"
            List<Point> adjacentWallPoints = null;
            if (wallDirection == WallDirection.NORTH) {
                Point nextPoint = new Point(p.x + 1, p.y); // East
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            } else if (wallDirection == WallDirection.EAST) {
                Point nextPoint = new Point(p.x, p.y + 1); // South
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            } else if (wallDirection == WallDirection.SOUTH) {
                Point nextPoint = new Point(p.x + 1, p.y); // East, sorting, so right is left now
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            } else {
                Point nextPoint = new Point(p.x, p.y + 1); // South, sorting, so right is left now
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            }

            // Add the point(s)
            if (adjacentWallPoints != null) {
                wallPoints.addAll(adjacentWallPoints);
            }
            return wallPoints;
        }

        return null;
    }

    protected Point[] getSurroundingTiles(Point point, boolean diagonal) {

        // Get all surrounding tiles
        List<Point> tileCoords = new ArrayList<>(diagonal ? 8 : 4);
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
        if ((x >= 0 && x < mapData.getWidth() && y >= 0 && y < mapData.getHeight())) {
            tileCoords.add(new Point(x, y));
        }
    }

    /**
     * Update the selected rooms' walls
     *
     * @param rooms the rooms to update
     */
    protected void updateRoomWalls(RoomInstance... rooms) {
        for (RoomInstance room : rooms) {
            findRoomWallSections(room);
            RoomConstructor.constructRoom(room, assetManager, kwdFile);            
        }
    }

    protected void updateRoomWalls(List<RoomInstance> rooms) {
        updateRoomWalls(rooms.toArray(new RoomInstance[rooms.size()]));
    }

    /**
     * Get a rooms by knowing its instance
     *
     * @return room
     */
    public HashMap<RoomInstance, GenericRoom> getRoomActuals() {
        return roomActuals;
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress
     * @param max max progress
     */
    protected abstract void updateProgress(final int progress, final int max);
}
