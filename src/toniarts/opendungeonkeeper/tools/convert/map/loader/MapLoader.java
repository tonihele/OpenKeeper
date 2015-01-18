/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map.loader;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource;
import toniarts.opendungeonkeeper.tools.convert.map.KwdFile;
import toniarts.opendungeonkeeper.tools.convert.map.Map;
import toniarts.opendungeonkeeper.tools.convert.map.Terrain;

/**
 * Loads whole maps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapLoader implements ILoader<KwdFile> {

    private final static float TILE_SIZE_X = 0.7f;
    private final static float TILE_SIZE_Y = 0.7f;
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
                                Spatial ceilingPart = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + modelName + "_" + ((k % 2) + 1) + ".j3o");
                                ceilingPart.move(i * 0.175f - 0.0875f, k * 0.175f - 0.0875f, 0);
                                ((Node) ceiling).attachChild(ceilingPart);
                            }
                        }
                        ceiling.setLocalScale(2, 2, 1);
                    } else {
                        ceiling = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + modelName + ".j3o");
                    }
                    ceiling.move(x * TILE_SIZE_X, y * TILE_SIZE_Y, (ceilingResource.equals(terrain.getCompleteResource()) ? -0.7f : 0));
                    root.attachChild(ceiling);

                    // See the wall status

                    // North
                    ArtResource wallNorth = getWallNorth(x, y, tiles, terrain);
                    if (wallNorth != null) {
                        Spatial wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallNorth.getName() + ".j3o");
                        wall.move(0, 0.5f, -0.5f);
                        addWall(wall, root, x, y);
                    }

                    // South
                    ArtResource wallSouth = getWallSouth(x, y, tiles, terrain);
                    if (wallSouth != null) {
                        Spatial wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallSouth.getName() + ".j3o");
                        wall.move(0, -0.5f, -0.5f);
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                        wall.rotate(quat);
                        addWall(wall, root, x, y);
                    }

                    // East
                    ArtResource wallEast = getWallEast(x, y, tiles, terrain);
                    if (wallEast != null) {
                        Spatial wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallEast.getName() + ".j3o");
                        wall.move(-0.5f, 0, -0.5f);
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                        wall.rotate(quat);
                        addWall(wall, root, x, y);
                    }

                    // West
                    ArtResource wallWest = getWallWest(x, y, tiles, terrain);
                    if (wallWest != null) {
                        Spatial wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallWest.getName() + ".j3o");
                        wall.move(0.5f, 0, -0.5f);
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                        wall.rotate(quat);
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
                        if (!waterNE && !waterE && !waterSE && waterN && waterS && waterW) {
                            floor = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o");
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                            floor.rotate(quat);
                            floor.move(0, 0, -1.15f);
                        } else if (!waterNW && !waterW && !waterSW && waterN && waterS && waterE) {
                            floor = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o");
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(0, 0, -1.15f);
                        } else if (!waterNW && !waterN && !waterNW && waterE && waterW && waterS) {
                            floor = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o");
                            Quaternion quat = new Quaternion();
                            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                            floor.rotate(quat);
                            floor.move(0, 0, -1.15f);
                        } else if (!waterSW && !waterS && !waterSW && waterE && waterW && waterN) {
                            floor = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "0" + ".j3o");
                            floor.move(0, 0, -1.15f);
                        } else { // Just a seabed
                            floor = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + "3" + ".j3o");
                            floor.move(0, 0, -1.33f);
                        }

                        floor.move(x * TILE_SIZE_X, y * TILE_SIZE_Y, 0);
                        root.attachChild(floor);

                        ////
                    } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
                    } else {
                        Spatial floor = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + floorResource.getName() + ".j3o");
                        floor.move(x * TILE_SIZE_X, y * TILE_SIZE_Y, -1f);
                        root.attachChild(floor);
                    }
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
            wall.move(x * TILE_SIZE_X, y * TILE_SIZE_Y, 0);
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
}
