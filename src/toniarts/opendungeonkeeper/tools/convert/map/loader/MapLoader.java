/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map.loader;

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
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.KmfModelLoader;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource;
import toniarts.opendungeonkeeper.tools.convert.map.KwdFile;
import toniarts.opendungeonkeeper.tools.convert.map.Map;
import toniarts.opendungeonkeeper.tools.convert.map.Room;
import toniarts.opendungeonkeeper.tools.convert.map.Terrain;

/**
 * Loads whole maps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapLoader implements ILoader<KwdFile> {

    private final static float TILE_WIDTH = 0.70f; // Impenetrable rock top is just little over this, "x & y", square
    private final static float TILE_HEIGHT = 0.70f; // Impenetrable rock wall is just little over this, "z", ground level is at 0
    private final static float WATER_DEPTH = 0.25f;
    private KwdFile kwdFile;

    @Override
    public Spatial load(AssetManager assetManager, KwdFile object) {
        this.kwdFile = object;

        //Create a root
        BatchNode root = new BatchNode("Map");

        // Go through the map
        Map[][] tiles = object.getTiles();
        for (int x = 0; x < object.getWidth(); x++) {
            for (int y = 0; y < object.getHeight(); y++) {
                Map tile = tiles[x][y];

                // Get the terrain
                Terrain terrain = object.getTerrain(tile.getTerrainId());
                ArtResource ceilingResource = getCeilingResource(terrain);
                if (ceilingResource != null) {

                    // Ceiling
                    String modelName = ceilingResource.getName();

                    // Naming...
                    if (modelName.equals("CLAIMED TOP")) {
                        modelName = "Claimed Top";
                    }

                    // If ownable, playerId is first
                    if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                        modelName += tile.getPlayerId();
                    }

                    // If this resource is type complete, it needs to be parsed together from tiles
                    Spatial ceiling;
                    if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {

                        // 2x2
                        ceiling = new Node();
//                        modelName += "_" + 1;
                        for (int i = 0; i < 2; i++) {
                            for (int k = 0; k < 2; k++) {
                                Spatial ceilingPart = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + modelName + "_" + ((k % 2) + 1) + ".j3o", false);
                                ceilingPart.move((i - 1) * TILE_WIDTH, (k - 1) * TILE_WIDTH, 0);
                                ((Node) ceiling).attachChild(ceilingPart);
                            }
                        }
                        ceiling.setLocalScale(0.5f, 0.5f, 1);
                    } else {
                        ceiling = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + modelName + ".j3o", false);

                        if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
                            setRandomTexture(assetManager, ceiling);
                        }
                    }
                    ceiling.move(x * TILE_WIDTH, y * TILE_WIDTH, -TILE_HEIGHT);
                    ceiling.setShadowMode(RenderQueue.ShadowMode.Off); // Ceilings never cast or receive, only thing above them would be picked up creatures, and they are solid
                    root.attachChild(ceiling);

                    // See the wall status

                    // North
                    ArtResource wallNorth = getWallNorth(x, y, tiles, terrain);
                    if (wallNorth != null) {
                        Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallNorth.getName() + ".j3o", true);
                        wall.move(0, -TILE_WIDTH, 0);
                        addWall(wall, root, x, y);
                    }

                    // South
                    ArtResource wallSouth = getWallSouth(x, y, tiles, terrain);
                    if (wallSouth != null) {
                        Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallSouth.getName() + ".j3o", true);
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                        wall.rotate(quat);
                        wall.move(-TILE_WIDTH, 0, 0);
                        addWall(wall, root, x, y);
                    }

                    // East
                    ArtResource wallEast = getWallEast(x, y, tiles, terrain);
                    if (wallEast != null) {
                        Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallEast.getName() + ".j3o", true);
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                        wall.rotate(quat);
                        addWall(wall, root, x, y);
                    }

                    // West
                    ArtResource wallWest = getWallWest(x, y, tiles, terrain);
                    if (wallWest != null) {
                        Spatial wall = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + wallWest.getName() + ".j3o", true);
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                        wall.rotate(quat);
                        wall.move(-TILE_WIDTH, -TILE_WIDTH, 0);
                        addWall(wall, root, x, y);
                    }

                    //
                } else if (terrain.getCompleteResource() != null) {

                    // Floor, no ceiling, it has a floor
                    ArtResource floorResource = terrain.getCompleteResource();

                    // For water construction type (lava & water), there are 8 pieces (0-7 suffix) in complete resource
                    // And in the top resource there is the actual lava/water
                    if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {

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

                        Spatial floor = null;

                        //Sides
                        if (!waterE && waterS && waterSW && waterW && waterNW && waterN) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                            floor.rotate(quat);
                            floor.move(0, -TILE_WIDTH, 0);
                        } else if (!waterS && waterW && waterNW && waterN && waterNE && waterE) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
                        } else if (!waterW && waterN && waterNE && waterE && waterSE && waterS) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(-TILE_WIDTH, 0, 0);
//                        } else if (!waterSW && !waterS && !waterSE && waterE && waterW && waterN) {
                        } else if (!waterN && waterE && waterSE && waterS && waterSW && waterW) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(-TILE_WIDTH, -TILE_WIDTH, 0);
                        } //
                        // Just one corner
                        else if (!waterSW && waterS && waterSE && waterE && waterW && waterN && waterNE && waterNW) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(-TILE_WIDTH, -TILE_WIDTH, 0);
                        } else if (!waterNE && waterS && waterSE && waterE && waterW && waterN && waterSW && waterNW) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
                        } else if (!waterSE && waterS && waterSW && waterE && waterW && waterN && waterNE && waterNW) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(-TILE_WIDTH, 0, 0);
                        } else if (!waterNW && waterS && waterSW && waterE && waterW && waterN && waterNE && waterSE) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "2" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                            floor.rotate(quat);
                            floor.move(0, -TILE_WIDTH, 0);
                        } //
                        // Land corner
                        else if (!waterN && !waterNW && !waterW && waterS && waterSE && waterE) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(-TILE_WIDTH, 0, 0f);
                        } else if (!waterN && !waterNE && !waterE && waterSW && waterS && waterW) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(-TILE_WIDTH, -TILE_WIDTH, 0f);
                        } else if (!waterS && !waterSE && !waterE && waterN && waterW && waterNW) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                            floor.rotate(quat);
                            floor.move(0, -TILE_WIDTH, 0f);
                        } else if (!waterS && !waterSW && !waterW && waterN && waterNE && waterE) {
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "1" + ".j3o", false);
                        }//
                        // Just a seabed
                        else if (waterS && waterSW && waterW && waterSE && waterN && waterNE && waterE && waterNW) { // Just a seabed
                            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "3" + ".j3o", false);
                        } else {
                            loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "4" + ".j3o", false);
                        }

                        if (floor != null) {
                            floor.move(x * TILE_WIDTH, y * TILE_WIDTH, WATER_DEPTH);
                            floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Only receive
                            root.attachChild(floor);
                        }

                        ////
                    } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
                    } else {
                        Spatial floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + ".j3o", false);
                        floor.move(x * TILE_WIDTH, y * TILE_WIDTH, 0);
                        floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Only receive
                        root.attachChild(floor);
                    }
                } else {

                    // All is null, a room perhaps
                    Room room = object.getRoomByTerrain(terrain.getTerrainId());
                }
            }
        }

        // The models are in weird angle, rotate 90 degrees to get it "standard"
        Quaternion quat = new Quaternion();
        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0));
        root.rotate(quat);

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

        // The tile next to this needs to have it's own ceiling
        Terrain neigbourTerrain = kwdFile.getTerrain(tiles[x][y].getTerrainId());
        if (getCeilingResource(neigbourTerrain) == null) {
            if (neigbourTerrain.getSideResource() != null && terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS)) {

                // Use neighbour wall
                return neigbourTerrain.getSideResource();
            }

            // Use our wall
            return terrain.getSideResource();
        }

        return null;
    }

    private void addWall(Spatial wall, Node root, int x, int y) {

        // Move the ceiling to a correct tile
        if (wall != null) {
            wall.move(x * TILE_WIDTH, y * TILE_WIDTH, 0);
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
    private static Spatial loadAsset(final AssetManager assetManager, final String asset, final boolean wall) {
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

                    // Adjust the size
                    BoundingBox worldBound = (BoundingBox) n.getWorldBound();
                    float xExtent = worldBound.getXExtent() * 2;
                    float yExtent = worldBound.getYExtent() * 2;
                    float zExtent = worldBound.getZExtent() * 2;
                    n.scale((xExtent != 0 ? TILE_WIDTH / xExtent : 1f), (yExtent != 0 ? TILE_WIDTH / yExtent : 1f), (zExtent != 0 ? (wall ? TILE_HEIGHT : WATER_DEPTH) / zExtent : 1f));

                    // Set the translation so that everything moves similarly
                    worldBound = (BoundingBox) n.getWorldBound();
                    Vector3f boundCenter = worldBound.getCenter();
                    n.setLocalTranslation(0 - boundCenter.x - worldBound.getXExtent(), 0 - boundCenter.y - worldBound.getYExtent(), 0 - boundCenter.z - worldBound.getZExtent());
                }
            }
        });
        return spatial;
    }
}
