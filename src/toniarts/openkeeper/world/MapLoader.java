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
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Map;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.room.FiveByFiveRotated;
import toniarts.openkeeper.world.room.HeroGateFrontEnd;
import toniarts.openkeeper.world.room.Normal;
import toniarts.openkeeper.world.room.RoomInstance;
import toniarts.openkeeper.world.room.ThreeByThree;

/**
 * Loads whole maps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapLoader implements ILoader<KwdFile> {

    public final static float TILE_WIDTH = 1;
    public final static float TILE_HEIGHT = 1;
    private final static float WATER_DEPTH = 0.3525f;
    private KwdFile kwdFile;
    private List<RoomInstance> rooms = new ArrayList<>(); // The list of rooms
    private HashMap<Point, RoomInstance> roomCoordinates = new HashMap<>(); // A quick glimpse whether room at specific coordinates is already "found"

    @Override
    public Spatial load(AssetManager assetManager, KwdFile object) {
        this.kwdFile = object;

        //Create a root
        BatchNode root = new BatchNode("Map");

        // Go through the map
        Map[][] tiles = object.getTiles();
        for (int x = 0; x < object.getWidth(); x++) {
            for (int y = 0; y < object.getHeight(); y++) {
                handleTile(tiles, x, y, assetManager, root);
            }
        }

        // Batch it
        root.batch();

        return root;
    }

    private ArtResource getWallNorth(int x, int y, Map[][] tiles, Terrain terrain) {
        return getWall(x, y - 1, tiles, terrain);
    }

    private ArtResource getWallSouth(int x, int y, Map[][] tiles, Terrain terrain) {
        return getWall(x, y + 1, tiles, terrain);
    }

    private ArtResource getWallEast(int x, int y, Map[][] tiles, Terrain terrain) {
        return getWall(x + 1, y, tiles, terrain);
    }

    private ArtResource getWallWest(int x, int y, Map[][] tiles, Terrain terrain) {
        return getWall(x - 1, y, tiles, terrain);
    }

    private ArtResource getWall(int x, int y, Map[][] tiles, Terrain terrain) {

        // Check for out of bounds
        if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[x].length) {
            return terrain.getSideResource();
        }

        // The tile next to this needs to have its own ceiling
        Terrain neigbourTerrain = kwdFile.getTerrain(tiles[x][y].getTerrainId());
        if (getCeilingResource(neigbourTerrain) == null) {

            // Rooms are built separately, so just ignore any room walls
            if (!(terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS) && hasRoomWalls(neigbourTerrain))) {

                // Use our terrain wall
                return terrain.getSideResource();
            }
        }

        return null;
    }

    private void addWall(Spatial wall, Node root, int x, int y) {

        // Move the ceiling to a correct tile
        if (wall != null) {
            wall.move(x * TILE_WIDTH, 0, y * TILE_WIDTH);
            wall.setShadowMode(RenderQueue.ShadowMode.CastAndReceive); // Walls cast and receive shadows
            root.attachChild(wall);
        }
    }

    private ArtResource getCeilingResource(Terrain terrain) {
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
                return terrain.getCompleteResource(); // Claimed top
            } else {
                return terrain.getTopResource(); // Normal
            }
        }
        return null;
    }

    /**
     * Compares the given terrain tile to terrain tile at the given coordinates
     *
     * @param tiles the tiles
     * @param x the x
     * @param y the y
     * @param terrain terrain tile to compare with
     * @return are the tiles same
     */
    private boolean hasSameTile(Map[][] tiles, int x, int y, Terrain terrain) {

        // Check for out of bounds
        if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[x].length) {
            return false;
        }
        return tiles[x][y].getTerrainId() == terrain.getTerrainId();
    }

    /**
     * Sets random material (from the list) to all the geometries that have been
     * tagged for this in this spatial
     *
     * @param assetManager the asset manager
     * @param spatial the spatial
     */
    private void setRandomTexture(final AssetManager assetManager, Spatial spatial) {

        // Check the data on geometry
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                Integer texCount = spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES_COUNT);
                if (texCount != null) {
                    int tex = FastMath.rand.nextInt(texCount);
                    if (tex != 0) { // 0 is the default anyway
                        Geometry g = (Geometry) spatial;
                        Material m = g.getMaterial();
                        String asset = m.getAssetName();

                        // Load new material
                        Material newMaterial = assetManager.loadMaterial(asset.substring(0, asset.lastIndexOf(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR) + 1).concat(tex + ".j3m"));
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

    /**
     * Constructs a quad tile type (2x2 pieces forms one tile), i.e. claimed top
     * and floor
     *
     * @param tiles the tiles
     * @param terrain the terrain
     * @param tile the tile
     * @param x x
     * @param y y
     * @param assetManager the asset manager instance
     * @param modelName the model name to load
     * @return the loaded model
     */
    private Spatial constructTerrainQuad(Map[][] tiles, final Terrain terrain, Map tile, int x, int y, final AssetManager assetManager, String modelName) {
        boolean ceiling = false;
        if ("CLAIMED TOP".equals(modelName)) {
            modelName = "Claimed Top";
            ceiling = true;
        } else if ("CLAIMED FLOOR".equals(modelName)) {
            modelName = "Claimed Floor";
        }

        // If ownable, playerId is first
        if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
            modelName += tile.getPlayerId() - 1;
        }

        // It needs to be parsed together from tiles

        // Figure out which peace by seeing the neighbours
        // This is slightly different with the top
        boolean N = (hasSameTile(tiles, x, y - 1, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x][y - 1].getTerrainId())) != null));
        boolean NE = (hasSameTile(tiles, x + 1, y - 1, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x + 1][y - 1].getTerrainId())) != null));
        boolean E = (hasSameTile(tiles, x + 1, y, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x + 1][y].getTerrainId())) != null));
        boolean SE = (hasSameTile(tiles, x + 1, y + 1, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x + 1][y + 1].getTerrainId())) != null));
        boolean S = (hasSameTile(tiles, x, y + 1, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x][y + 1].getTerrainId())) != null));
        boolean SW = (hasSameTile(tiles, x - 1, y + 1, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x - 1][y + 1].getTerrainId())) != null));
        boolean W = (hasSameTile(tiles, x - 1, y, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x - 1][y].getTerrainId())) != null));
        boolean NW = (hasSameTile(tiles, x - 1, y - 1, terrain) || (ceiling && getCeilingResource(kwdFile.getTerrain(tiles[x - 1][y - 1].getTerrainId())) != null));

        // 2x2
        Spatial model = new Node();
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {

                int pieceNumber = 0;
                Quaternion quat = null;
                Vector3f movement = null;

                // Determine the piece
                if (i == 0 && k == 0) { // North west corner
                    if (N && W && NW) {
                        pieceNumber = 3;
                    } else if (!N && W && NW) {
                        pieceNumber = 0;
                    } else if (!NW && N && W) {
                        pieceNumber = 2;
                    } else if (!N && !W) {
                        pieceNumber = 1;
                    } else if (!W && NW && N) {
                        pieceNumber = 4;
                    } else if (!W && !NW && N) {
                        pieceNumber = 4;
                    } else {
                        pieceNumber = 0;
                    }
                    quat = new Quaternion();
                    quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                    movement = new Vector3f(-TILE_WIDTH * 2, 0, 0);
                } else if (i == 1 && k == 0) { // North east corner
                    if (N && E && NE) {
                        pieceNumber = 3;
                    } else if (!N && E && NE) {
                        pieceNumber = 4;
                    } else if (!NE && N && E) {
                        pieceNumber = 2;
                    } else if (!N && !E) {
                        pieceNumber = 1;
                    } else if (!E && NE && N) {
                        pieceNumber = 0;
                    } else if (!E && !NE && N) {
                        pieceNumber = 0;
                    } else {
                        pieceNumber = 4;
                    }
                    quat = new Quaternion();
                    quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                } else if (i == 0 && k == 1) { // South west corner
                    if (S && W && SW) {
                        pieceNumber = 3;
                    } else if (!S && W && SW) {
                        pieceNumber = 4;
                    } else if (!SW && S && W) {
                        pieceNumber = 2;
                    } else if (!S && !W) {
                        pieceNumber = 1;
                    } else if (!W && SW && S) {
                        pieceNumber = 0;
                    } else if (!W && !SW && S) {
                        pieceNumber = 0;
                    } else {
                        pieceNumber = 4;
                    }
                    quat = new Quaternion();
                    quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                    movement = new Vector3f(-TILE_WIDTH * 2, 0, 0);
                } else if (i == 1 && k == 1) { // South east corner
                    if (S && E && SE) {
                        pieceNumber = 3;
                    } else if (!S && E && SE) {
                        pieceNumber = 0;
                    } else if (!SE && S && E) {
                        pieceNumber = 2;
                    } else if (!S && !E) {
                        pieceNumber = 1;
                    } else if (!E && SE && S) {
                        pieceNumber = 4;
                    } else if (!E && !SE && S) {
                        pieceNumber = 4;
                    } else {
                        pieceNumber = 0;
                    }
                }

                // Load the piece
                Spatial part = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + modelName + "_" + pieceNumber + ".j3o", false);
                if (quat != null) {
                    part.rotate(quat);
                }
                if (movement != null) {
                    part.move(movement);
                }
                part.move((i - 1) * -TILE_WIDTH, 0, (k - 1) * TILE_WIDTH);
                ((Node) model).attachChild(part);
            }
        }

        return model;
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
    private void handleTile(Map[][] tiles, int x, int y, AssetManager assetManager, BatchNode root) {
        Map tile = tiles[x][y];

        // Get the terrain
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        ArtResource ceilingResource = getCeilingResource(terrain);
        if (ceilingResource != null) {

            // Ceiling & wall
            handleCeilingAndWall(ceilingResource, terrain, tiles, tile, x, y, assetManager, root);
        } else if (terrain.getCompleteResource() != null) {

            // Floor, no ceiling, it has a floor
            ArtResource floorResource = terrain.getCompleteResource();
            handleFloor(terrain, tiles, x, y, assetManager, floorResource, root, tile);
        } else {

            // All is null, a room perhaps
            Point p = new Point(x, y);
            if (!roomCoordinates.containsKey(p)) {
                RoomInstance roomInstance = new RoomInstance(kwdFile.getRoomByTerrain(terrain.getTerrainId()));
                findRoom(tiles, p, roomInstance);
                rooms.add(roomInstance);

                // Construct the actual room
                handleRoom(tiles, assetManager, root, roomInstance);
            }
        }
    }

    /**
     * Handle ceiling and wall construction on the tile
     *
     * @param ceilingResource the ceiling resource
     * @param terrain the terrain tile
     * @param tiles all the tiles
     * @param tile this tile
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param assetManager the asset manager instance
     * @param root the root node
     */
    private void handleCeilingAndWall(ArtResource ceilingResource, Terrain terrain, Map[][] tiles, Map tile, int x, int y, AssetManager assetManager, BatchNode root) {

        // Ceiling
        String modelName = ceilingResource.getName();

        // If this resource is type quad, parse it together
        Spatial ceiling;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
            ceiling = constructTerrainQuad(tiles, terrain, tile, x, y, assetManager, modelName);
        } else {
            ceiling = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + modelName + ".j3o", false);

            if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
                setRandomTexture(assetManager, ceiling);
            }
        }
        ceiling.move(x * TILE_WIDTH, TILE_HEIGHT, y * TILE_WIDTH);
        ceiling.setShadowMode(RenderQueue.ShadowMode.Off); // Ceilings never cast or receive, only thing above them would be picked up creatures, and they are solid
        root.attachChild(ceiling);

        // See the wall status

        // North
        ArtResource wallNorth = getWallNorth(x, y, tiles, terrain);
        if (wallNorth != null) {
            Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallNorth.getName() + ".j3o", true);
            wall.move(0, 0, -TILE_WIDTH);
            addWall(wall, root, x, y);
        }

        // South
        ArtResource wallSouth = getWallSouth(x, y, tiles, terrain);
        if (wallSouth != null) {
            Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallSouth.getName() + ".j3o", true);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
            wall.rotate(quat);
            wall.move(-TILE_WIDTH, 0, 0);
            addWall(wall, root, x, y);
        }

        // East
        ArtResource wallEast = getWallEast(x, y, tiles, terrain);
        if (wallEast != null) {
            Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallEast.getName() + ".j3o", true);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
            wall.rotate(quat);
            addWall(wall, root, x, y);
        }

        // West
        ArtResource wallWest = getWallWest(x, y, tiles, terrain);
        if (wallWest != null) {
            Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallWest.getName() + ".j3o", true);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
            wall.rotate(quat);
            wall.move(-TILE_WIDTH, 0, -TILE_WIDTH);
            addWall(wall, root, x, y);
        }

        //
    }

    /**
     * Handles the floor tile construction
     *
     * @param terrain the terrain tile
     * @param tiles all the tiles
     * @param tile this tile
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param assetManager the asset manager instance
     * @param floorResource the floor resource
     * @param root the root node
     */
    private void handleFloor(Terrain terrain, Map[][] tiles, int x, int y, AssetManager assetManager, ArtResource floorResource, BatchNode root, Map tile) {

        // For water construction type (lava & water), there are 8 pieces (0-7 suffix) in complete resource
        // And in the top resource there is the actual lava/water
        if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {

            // Get the tile
            Spatial floor = handleWaterConstruction(tiles, x, y, terrain, assetManager, floorResource);

            // Finally add it
            floor.move(x * TILE_WIDTH, 0, y * TILE_WIDTH);
            floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Only receive
            root.attachChild(floor);

            ////
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
            String modelName = floorResource.getName();
            Spatial floor = constructTerrainQuad(tiles, terrain, tile, x, y, assetManager, modelName);
            floor.move(x * TILE_WIDTH, 0, y * TILE_WIDTH);
            floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Only receive
            root.attachChild(floor);
        } else {
            Spatial floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + ".j3o", false);
            floor.move(x * TILE_WIDTH, 0, y * TILE_WIDTH);
            floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Only receive
            root.attachChild(floor);
        }
    }

    /**
     * Contstruct a water / lava tile
     *
     * @param tiles the tiles
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param terrain the terrain tile
     * @param assetManager the asset manager instance
     * @param floorResource the floor resource
     * @return a water / lava tile
     */
    private Spatial handleWaterConstruction(Map[][] tiles, int x, int y, Terrain terrain, AssetManager assetManager, ArtResource floorResource) {

        // The bed
        // Figure out which peace by seeing the neighbours
        boolean waterN = hasSameTile(tiles, x, y - 1, terrain);
        boolean waterNE = hasSameTile(tiles, x + 1, y - 1, terrain);
        boolean waterE = hasSameTile(tiles, x + 1, y, terrain);
        boolean waterSE = hasSameTile(tiles, x + 1, y + 1, terrain);
        boolean waterS = hasSameTile(tiles, x, y + 1, terrain);
        boolean waterSW = hasSameTile(tiles, x - 1, y + 1, terrain);
        boolean waterW = hasSameTile(tiles, x - 1, y, terrain);
        boolean waterNW = hasSameTile(tiles, x - 1, y - 1, terrain);
        Spatial floor;
        //Sides
        if (!waterE && waterS && waterSW && waterW && waterNW && waterN) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
            floor.rotate(quat);
            floor.move(0, 0, -TILE_WIDTH);
        } else if (!waterS && waterW && waterNW && waterN && waterNE && waterE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
        } else if (!waterW && waterN && waterNE && waterE && waterSE && waterS) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!waterN && waterE && waterSE && waterS && waterSW && waterW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } //
        // Just one corner
        else if (!waterSW && waterS && waterSE && waterE && waterW && waterN && waterNE && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } else if (!waterNE && waterS && waterSE && waterE && waterW && waterN && waterSW && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
        } else if (!waterSE && waterS && waterSW && waterE && waterW && waterN && waterNE && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!waterNW && waterS && waterSW && waterE && waterW && waterN && waterNE && waterSE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
            floor.rotate(quat);
            floor.move(0, 0, -TILE_WIDTH);
        } //
        // Land corner
        else if (!waterN && !waterNW && !waterW && waterS && waterSE && waterE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!waterN && !waterNE && !waterE && waterSW && waterS && waterW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } else if (!waterS && !waterSE && !waterE && waterN && waterW && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
            floor.rotate(quat);
            floor.move(0, 0, -TILE_WIDTH);
        } else if (!waterS && !waterSW && !waterW && waterN && waterNE && waterE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
        }//
        // Just a seabed
        else if (waterS && waterSW && waterW && waterSE && waterN && waterNE && waterE && waterNW) { // Just a seabed
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "3" + ".j3o", false);
            floor.move(0, -WATER_DEPTH, 0); // Water bed is flat
        }//
        // We have only the one tilers left, they need to be constructed similar to quads, but unfortunately not just the same
        else {

            // 2x2
            floor = new Node();
            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {

                    int pieceNumber = 7;
                    Quaternion quat = null;
                    Vector3f movement = null;

                    // Determine the piece
                    if (i == 0 && k == 0) { // North west corner
                        if (!waterN && waterW) { // Side
                            pieceNumber = 4;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0)); //
                        } else if (!waterW && waterN) { // Side
                            pieceNumber = 4;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2); //
                        } else if (!waterNW && waterN && waterW) { // Corner surrounded by water
                            pieceNumber = 6;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0); //
                        } else if (!waterN && !waterW) { // Corner surrounded by land
                            pieceNumber = 5;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2); //
                        } else { // Seabed
                            movement = new Vector3f(TILE_WIDTH / 2, 0, TILE_WIDTH / 2);
                        }
                    } else if (i == 1 && k == 0) { // North east corner
                        if (!waterN && waterE) { // Side
                            pieceNumber = 4;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                            movement = new Vector3f(-TILE_WIDTH / 2, 0, 0); //
                        } else if (!waterE && waterN) { // Side
                            pieceNumber = 4;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0)); //
                        } else if (!waterNE && waterN && waterE) { // Corner surrounded by water
                            pieceNumber = 6;
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2); //
                        } else if (!waterN && !waterE) { // Corner surrounded by land
                            pieceNumber = 5;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                            movement = new Vector3f(-TILE_WIDTH / 2, 0, 0); //
                        } else { // Seabed
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2);
                        }
                    } else if (i == 0 && k == 1) { // South west corner
                        if (!waterS && waterW) { // Side
                            pieceNumber = 4;
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0); //
                        } else if (!waterW && waterS) { // Side
                            pieceNumber = 4;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0)); //
                        } else if (!waterSW && waterS && waterW) { // Corner surrounded by water
                            pieceNumber = 6;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                            movement = new Vector3f(0, 0, -TILE_WIDTH / 2); //
                        } else if (!waterS && !waterW) { // Corner surrounded by land
                            pieceNumber = 5;
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0); //
                        } else { // Seabed
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0);
                        }
                    } else if (i == 1 && k == 1) { // South east corner
                        if (!waterS && waterE) { // Side
                            pieceNumber = 4;
                        } else if (!waterE && waterS) { // Side
                            pieceNumber = 4;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                            movement = new Vector3f(0, 0, -TILE_WIDTH / 2); //
                        } else if (!waterSE && waterS && waterE) { // Corner surrounded by water
                            pieceNumber = 6;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                            movement = new Vector3f(-TILE_WIDTH / 2, 0, 0); //
                        } else if (!waterS && !waterE) { // Corner surrounded by land
                            pieceNumber = 5;
                            quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                            movement = new Vector3f(0, 0, -TILE_WIDTH / 2); //
                        }
                    }

                    // Load the piece
                    Spatial part = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + pieceNumber + ".j3o", false);
                    if (quat != null) {
                        part.rotate(quat);
                    }
                    if (movement != null) {
                        part.move(movement);
                    }
                    part.move((i - 1) * TILE_WIDTH, -(pieceNumber == 7 ? WATER_DEPTH : 0), (k - 1) * TILE_WIDTH);
                    ((Node) floor).attachChild(part);
                }
            }
        }
        //
        return floor;
    }

    /**
     * Checks if this terrain piece is actually a room and the room type has
     * walls
     *
     * @param terrain the terrain piece
     * @return true if this is a room and it has its own walls
     */
    private boolean hasRoomWalls(Terrain terrain) {
        ArtResource ceilingResource = getCeilingResource(terrain);
        if (ceilingResource == null && terrain.getCompleteResource() == null) {

            // All is null, a room perhaps
            Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
            return room.getFlags().contains(Room.RoomFlag.HAS_WALLS) || room.getTileConstruction() == Room.TileConstruction.HERO_GATE_FRONT_END;
        }
        return false;
    }

    /**
     * Find the room starting from a certain point, rooms are never diagonally
     * attached
     *
     * @param tiles the tiles
     * @param p starting point
     * @param roomInstance the room instance
     */
    private void findRoom(Map[][] tiles, Point p, RoomInstance roomInstance) {
        Map tile = tiles[p.x][p.y];

        // Get the terrain
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        ArtResource ceilingResource = getCeilingResource(terrain);
        if (ceilingResource == null && terrain.getCompleteResource() == null) {

            // All is null, a room perhaps
            if (!roomCoordinates.containsKey(p)) {
                if (roomInstance.getRoom().equals(kwdFile.getRoomByTerrain(terrain.getTerrainId()))) {

                    // Add the coordinate
                    roomCoordinates.put(p, roomInstance);
                    roomInstance.addCoordinate(p);

                    // Find north
                    findRoom(tiles, new Point(p.x, p.y - 1), roomInstance);

                    // Find east
                    findRoom(tiles, new Point(p.x + 1, p.y), roomInstance);

                    // Find south
                    findRoom(tiles, new Point(p.x, p.y + 1), roomInstance);

                    // Find west
                    findRoom(tiles, new Point(p.x - 1, p.y), roomInstance);
                }
            }
        }
    }

    /**
     * Constructs the given room
     *
     * @param tiles the tiles
     * @param assetManager the asset manager instance
     * @param root the root node
     * @param roomInstance the room instance
     */
    private void handleRoom(Map[][] tiles, AssetManager assetManager, BatchNode root, RoomInstance roomInstance) {
        switch (roomInstance.getRoom().getTileConstruction()) {
            case _3_BY_3: {
                root.attachChild(ThreeByThree.construct(assetManager, roomInstance));
                break;
            }
            case HERO_GATE_FRONT_END: {
                root.attachChild(HeroGateFrontEnd.construct(assetManager, roomInstance));
                break;
            }
            case _5_BY_5_ROTATED: {
                root.attachChild(FiveByFiveRotated.construct(assetManager, roomInstance));
                break;
            }
            case NORMAL: {
                root.attachChild(Normal.construct(assetManager, roomInstance));
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     * Get a standard camera position vector on given map point
     *
     * @param x tile x coordinate
     * @param y tile y coordinate
     * @return camera location
     */
    public static Vector3f getCameraPositionOnMapPoint(final int x, final int y) {
        return new Vector3f((x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2), 0f, (y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2));
    }
}
