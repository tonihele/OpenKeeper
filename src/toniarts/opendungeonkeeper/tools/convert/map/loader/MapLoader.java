/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map.loader;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
        Node root = new Node("Map");

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
                    if (ceilingResource.equals(terrain.getCompleteResource())) {
                        modelName += "_" + 0;
                    }

                    Spatial ceiling = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + modelName + ".j3o");
                    ceiling.move(x * TILE_SIZE_X, y * TILE_SIZE_Y, 0);
                    root.attachChild(ceiling);
                }

                // See the wall status
                Spatial wall = null;

                // North
                ArtResource wallNorth = getWallNorth(x, y, tiles, terrain);
                if (wallNorth != null) {
                    wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallNorth.getName() + ".j3o");
                    wall.move(0, 0.5f, -0.5f);
                    addWall(wall, root, x, y);
                }

                // South
                ArtResource wallSouth = getWallSouth(x, y, tiles, terrain);
                if (wallSouth != null) {
                    wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallSouth.getName() + ".j3o");
                    wall.move(0, -0.5f, -0.5f);
                    Quaternion quat = new Quaternion();
                    quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                    wall.rotate(quat);
                    addWall(wall, root, x, y);
                }

                // East
                ArtResource wallEast = getWallEast(x, y, tiles, terrain);
                if (wallEast != null) {
                    wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallEast.getName() + ".j3o");
                    wall.move(-0.5f, 0, -0.5f);
                    Quaternion quat = new Quaternion();
                    quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                    wall.rotate(quat);
                    addWall(wall, root, x, y);
                }

                // West
                ArtResource wallWest = getWallWest(x, y, tiles, terrain);
                if (wallWest != null) {
                    wall = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + wallWest.getName() + ".j3o");
                    wall.move(0.5f, 0, -0.5f);
                    Quaternion quat = new Quaternion();
                    quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                    wall.rotate(quat);
                    addWall(wall, root, x, y);
                }

                //

                if (terrain.getSideResource() != null) {
                    // See wall need to next tile (since we have the
                }
            }
        }

        // The models are in weird angle, rotate 90 degrees to get it "standard"
        Quaternion quat = new Quaternion();
        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0));
        root.rotate(quat);
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
            if (neigbourTerrain.getSideResource() != null) {

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
        if (terrain.getTopResource() != null && terrain.getTopResource().getSettings().getType() == ArtResource.Type.TERRAIN_MESH) {
            return terrain.getTopResource(); // Normal
        }
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && terrain.getCompleteResource() != null && terrain.getCompleteResource().getSettings().getType() == ArtResource.Type.TERRAIN_MESH) {
            return terrain.getCompleteResource(); // Reinforced wall
        }
        return null;
    }
}
