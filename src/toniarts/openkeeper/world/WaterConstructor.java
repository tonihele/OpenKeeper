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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import static toniarts.openkeeper.world.MapLoader.loadAsset;

/**
 *
 * @author ArchDemon
 */

public class WaterConstructor extends TileConstructor {

    private final static float WATER_DEPTH = 0.3525f;
    public final static float WATER_LEVEL = 0.075f;

    public WaterConstructor(KwdFile kwdFile) {
        super(kwdFile);
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
    @Override
    public Spatial construct(TileData[][] tiles, int x, int y, final Terrain terrain, AssetManager assetManager, String model) {

        // The bed
        // Figure out which peace by seeing the neighbours
        boolean N = hasSameTile(tiles, x, y - 1, terrain);
        boolean NE = hasSameTile(tiles, x + 1, y - 1, terrain);
        boolean E = hasSameTile(tiles, x + 1, y, terrain);
        boolean SE = hasSameTile(tiles, x + 1, y + 1, terrain);
        boolean S = hasSameTile(tiles, x, y + 1, terrain);
        boolean SW = hasSameTile(tiles, x - 1, y + 1, terrain);
        boolean W = hasSameTile(tiles, x - 1, y, terrain);
        boolean NW = hasSameTile(tiles, x - 1, y - 1, terrain);
        Spatial floor;
        //Sides
        if (!E && S && SW && W && NW && N) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
            floor.rotate(0, FastMath.HALF_PI, 0);
            floor.move(0, 0, -TILE_WIDTH);
        } else if (!S && W && NW && N && NE && E) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
        } else if (!W && N && NE && E && SE && S) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
            floor.rotate(0, -FastMath.HALF_PI, 0);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!N && E && SE && S && SW && W) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
            floor.rotate(0, -FastMath.PI, 0);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } //
        // Just one corner
        else if (!SW && S && SE && E && W && N && NE && NW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
            floor.rotate(0, -FastMath.PI, 0);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } else if (!NE && S && SE && E && W && N && SW && NW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
        } else if (!SE && S && SW && E && W && N && NE && NW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
            floor.rotate(0, -FastMath.PI, 0);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!NW && S && SW && E && W && N && NE && SE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
            floor.rotate(0, FastMath.HALF_PI, 0);
            floor.move(0, 0, -TILE_WIDTH);
        }
        // Land corner
        else if (!N && !NW && !W && S && SE && E) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);

            floor.rotate(0, -FastMath.HALF_PI, 0);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!N && !NE && !E && SW && S && W) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);
            floor.rotate(0, -FastMath.PI, 0);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } else if (!S && !SE && !E && N && W && NW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);

            floor.rotate(0, FastMath.HALF_PI, 0);
            floor.move(0, 0, -TILE_WIDTH);
        } else if (!S && !SW && !W && N && NE && E) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);
        }//
        // Just a seabed
        else if (S && SW && W && SE && N && NE && E && NW) { // Just a seabed
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "3" + ".j3o", false);
            floor.move(0, -WATER_DEPTH, 0); // Water bed is flat
        }//
        // We have only the one tilers left, they need to be constructed similar to quads, but unfortunately not just the same
        else {

            // 2x2
            floor = new Node();
            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {

                    int pieceNumber = 7;
                    float yAngle = 0;
                    Vector3f movement = null;

                    // Determine the piece
                    if (i == 0 && k == 0) { // North west corner
                        if (!N && W) { // Side
                            pieceNumber = 4;
                            yAngle = FastMath.PI;
                        } else if (!W && N) { // Side
                            pieceNumber = 4;
                            yAngle = -FastMath.HALF_PI;
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2); //
                        } else if (!NW && N && W) { // Corner surrounded by water
                            pieceNumber = 6;
                            yAngle = FastMath.HALF_PI;
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0); //
                        } else if (!N && !W) { // Corner surrounded by land
                            pieceNumber = 5;
                            yAngle = -FastMath.HALF_PI;
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2); //
                        } else { // Seabed
                            movement = new Vector3f(TILE_WIDTH / 2, 0, TILE_WIDTH / 2);
                        }
                    } else if (i == 1 && k == 0) { // North east corner
                        if (!N && E) { // Side
                            pieceNumber = 4;
                            yAngle = FastMath.PI;
                            movement = new Vector3f(-TILE_WIDTH / 2, 0, 0); //
                        } else if (!E && N) { // Side
                            pieceNumber = 4;
                            yAngle = FastMath.HALF_PI;
                        } else if (!NE && N && E) { // Corner surrounded by water
                            pieceNumber = 6;
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2); //
                        } else if (!N && !E) { // Corner surrounded by land
                            pieceNumber = 5;
                            yAngle = -FastMath.PI;
                            movement = new Vector3f(-TILE_WIDTH / 2, 0, 0); //
                        } else { // Seabed
                            movement = new Vector3f(0, 0, TILE_WIDTH / 2);
                        }
                    } else if (i == 0 && k == 1) { // South west corner
                        if (!S && W) { // Side
                            pieceNumber = 4;
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0); //
                        } else if (!W && S) { // Side
                            pieceNumber = 4;
                            yAngle = -FastMath.HALF_PI;
                        } else if (!SW && S && W) { // Corner surrounded by water
                            pieceNumber = 6;
                            yAngle = -FastMath.PI;
                            movement = new Vector3f(0, 0, -TILE_WIDTH / 2); //
                        } else if (!S && !W) { // Corner surrounded by land
                            pieceNumber = 5;
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0); //
                        } else { // Seabed
                            movement = new Vector3f(TILE_WIDTH / 2, 0, 0);
                        }
                    } else if (i == 1 && k == 1) { // South east corner
                        if (!S && E) { // Side
                            pieceNumber = 4;
                        } else if (!E && S) { // Side
                            pieceNumber = 4;
                            yAngle = FastMath.HALF_PI;
                            movement = new Vector3f(0, 0, -TILE_WIDTH / 2); //
                        } else if (!SE && S && E) { // Corner surrounded by water
                            pieceNumber = 6;
                            yAngle = -FastMath.HALF_PI;
                            movement = new Vector3f(-TILE_WIDTH / 2, 0, 0); //
                        } else if (!S && !E) { // Corner surrounded by land
                            pieceNumber = 5;
                            yAngle = FastMath.HALF_PI;
                            movement = new Vector3f(0, 0, -TILE_WIDTH / 2); //
                        }
                    }

                    // Load the piece
                    Spatial part = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + pieceNumber + ".j3o", false);
                    if (yAngle != 0) {
                        part.rotate(0, yAngle, 0);
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
}
