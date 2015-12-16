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
import com.jme3.math.Quaternion;
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
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
            floor.rotate(quat);
            floor.move(0, 0, -TILE_WIDTH);
        } else if (!waterS && waterW && waterNW && waterN && waterNE && waterE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
        } else if (!waterW && waterN && waterNE && waterE && waterSE && waterS) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!waterN && waterE && waterSE && waterS && waterSW && waterW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "0" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } //
        // Just one corner
        else if (!waterSW && waterS && waterSE && waterE && waterW && waterN && waterNE && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } else if (!waterNE && waterS && waterSE && waterE && waterW && waterN && waterSW && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
        } else if (!waterSE && waterS && waterSW && waterE && waterW && waterN && waterNE && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!waterNW && waterS && waterSW && waterE && waterW && waterN && waterNE && waterSE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "2" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
            floor.rotate(quat);
            floor.move(0, 0, -TILE_WIDTH);
        } //
        // Land corner
        else if (!waterN && !waterNW && !waterW && waterS && waterSE && waterE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, 0);
        } else if (!waterN && !waterNE && !waterE && waterSW && waterS && waterW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
            floor.rotate(quat);
            floor.move(-TILE_WIDTH, 0, -TILE_WIDTH);
        } else if (!waterS && !waterSE && !waterE && waterN && waterW && waterNW) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
            floor.rotate(quat);
            floor.move(0, 0, -TILE_WIDTH);
        } else if (!waterS && !waterSW && !waterW && waterN && waterNE && waterE) {
            floor = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + "1" + ".j3o", false);
        }//
        // Just a seabed
        else if (waterS && waterSW && waterW && waterSE && waterN && waterNE && waterE && waterNW) { // Just a seabed
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
                    Spatial part = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + model + pieceNumber + ".j3o", false);
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
}
