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
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import toniarts.openkeeper.world.room.Quad;

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
     * @param mapData the tiles
     * @param x tile X coordinate
     * @param y tile Y coordinate
     * @param terrain the terrain tile
     * @param assetManager the asset manager instance
     * @param model the floor resource
     * @return a water / lava tile
     */
    @Override
    public Spatial construct(MapData mapData, int x, int y, final Terrain terrain, AssetManager assetManager, String model) {

        // The bed
        // Figure out which peace by seeing the neighbours
        boolean N = hasSameTile(mapData, x, y - 1, terrain);
        boolean NE = hasSameTile(mapData, x + 1, y - 1, terrain);
        boolean E = hasSameTile(mapData, x + 1, y, terrain);
        boolean SE = hasSameTile(mapData, x + 1, y + 1, terrain);
        boolean S = hasSameTile(mapData, x, y + 1, terrain);
        boolean SW = hasSameTile(mapData, x - 1, y + 1, terrain);
        boolean W = hasSameTile(mapData, x - 1, y, terrain);
        boolean NW = hasSameTile(mapData, x - 1, y - 1, terrain);

        Spatial floor;
        int piece = -1;
        float yAngle = 0;
        Vector3f movement = null;

        //Sides
        if (!E && S && SW && W && NW && N) {
            piece = 0;
            yAngle = FastMath.HALF_PI;
        } else if (!S && W && NW && N && NE && E) {
            piece = 0;
        } else if (!W && N && NE && E && SE && S) {
            piece = 0;
            yAngle = -FastMath.HALF_PI;
        } else if (!N && E && SE && S && SW && W) {
            piece = 0;
            yAngle = -FastMath.PI;
        } //
        // Just one corner
        else if (!SW && S && SE && E && W && N && NE && NW) {
            piece = 2;
            yAngle = -FastMath.PI;
        } else if (!NE && S && SE && E && W && N && SW && NW) {
            piece = 2;
        } else if (!SE && S && SW && E && W && N && NE && NW) {
            piece = 2;
            yAngle = -FastMath.HALF_PI;
        } else if (!NW && S && SW && E && W && N && NE && SE) {
            piece = 2;
            yAngle = FastMath.HALF_PI;
        } // Land corner
        else if (!N && !NW && !W && S && SE && E) {
            piece = 1;
            yAngle = -FastMath.HALF_PI;
        } else if (!N && !NE && !E && SW && S && W) {
            piece = 1;
            yAngle = -FastMath.PI;
        } else if (!S && !SE && !E && N && W && NW) {
            piece = 1;
            yAngle = FastMath.HALF_PI;
        } else if (!S && !SW && !W && N && NE && E) {
            piece = 1;
        }// Just a seabed
        else if (S && SW && W && SE && N && NE && E && NW) { // Just a seabed
            piece = 3;
        }
        //
        if (piece != -1) {
            floor = loadAsset(assetManager, model + piece, true);
            if (yAngle != 0) {
                floor.rotate(0, yAngle, 0);
            }
            if (movement != null) {
                floor.move(movement);
            }
            return floor;
        }
        // We have only the one tilers left, they need to be constructed similar to quads, but unfortunately not just the same
        // 2x2
        floor = new Node();
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {

                piece = 7;
                yAngle = 0;

                // Determine the piece
                if (i == 0 && k == 0) { // North west corner
                    if (!N && W) { // Side
                        piece = 4;
                        yAngle = FastMath.PI;
                    } else if (!W && N) { // Side
                        piece = 4;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!NW && N && W) { // Corner surrounded by water
                        piece = 6;
                        yAngle = FastMath.HALF_PI;
                    } else if (!N && !W) { // Corner surrounded by land
                        piece = 5;
                        yAngle = -FastMath.HALF_PI;
                    } 
                    movement = new Vector3f(-TILE_WIDTH / 4, 0, -TILE_WIDTH / 4);
                    
                } else if (i == 1 && k == 0) { // North east corner
                    if (!N && E) { // Side
                        piece = 4;
                        yAngle = FastMath.PI;
                    } else if (!E && N) { // Side
                        piece = 4;
                        yAngle = FastMath.HALF_PI;
                    } else if (!NE && N && E) { // Corner surrounded by water
                        piece = 6;
                    } else if (!N && !E) { // Corner surrounded by land
                        piece = 5;
                        yAngle = -FastMath.PI;
                    } 
                    movement = new Vector3f(TILE_WIDTH / 4, 0, -TILE_WIDTH / 4);
                    
                } else if (i == 0 && k == 1) { // South west corner
                    if (!S && W) { // Side
                        piece = 4;
                    } else if (!W && S) { // Side
                        piece = 4;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!SW && S && W) { // Corner surrounded by water
                        piece = 6;
                        yAngle = -FastMath.PI;
                    } else if (!S && !W) { // Corner surrounded by land
                        piece = 5;
                    } 
                    movement = new Vector3f(-TILE_WIDTH / 4, 0, TILE_WIDTH / 4);
                    
                } else { // South east corner if (i == 1 && k == 1)
                    if (!S && E) { // Side
                        piece = 4;
                    } else if (!E && S) { // Side
                        piece = 4;
                        yAngle = FastMath.HALF_PI;
                    } else if (!SE && S && E) { // Corner surrounded by water
                        piece = 6;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!S && !E) { // Corner surrounded by land
                        piece = 5;
                        yAngle = FastMath.HALF_PI;
                    }
                    movement = new Vector3f(TILE_WIDTH / 4, 0, TILE_WIDTH / 4);
                }

                // Load the piece
                Spatial part = MapLoader.loadTerrain(assetManager, model + piece, true);
                if (yAngle != 0) {
                    part.rotate(0, yAngle, 0);
                }

                part.move(movement);
                ((Node) floor).attachChild(part);
            }
        }
        //
        return floor;
    }
}
