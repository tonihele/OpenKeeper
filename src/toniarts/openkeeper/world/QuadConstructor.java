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

public class QuadConstructor extends TileConstructor {

    public QuadConstructor(KwdFile kwdFile) {
        super(kwdFile);
    }
    /**
     * Constructs a quad tile type (2x2 pieces forms one tile), i.e. claimed top
     * and floor
     *
     * @param mapData the tiles
     * @param terrain the terrain
     * @param tile the tile
     * @param x x
     * @param y y
     * @param assetManager the asset manager instance
     * @param modelName the model name to load
     * @return the loaded model
     */
    @Override
    public Spatial construct(MapData mapData, int x, int y, final Terrain terrain, final AssetManager assetManager, String modelName) {

        switch (modelName) {
            case "CLAIMED TOP":
                modelName = "Claimed Top";
                break;
            case "CLAIMED FLOOR":
                modelName = "Claimed Floor";
                break;
        }

        // If ownable, playerId is first. With fixed Hero Lair
        if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && terrain.getTerrainId() != 35) {
            TileData tile = mapData.getTile(x, y);
            modelName += tile.getPlayerId() - 1 + "_";
        }

        // It needs to be parsed together from tiles
        boolean solid = isSolidTile(mapData, x, y);
        // Figure out which peace by seeing the neighbours
        // This is slightly different with the top
        boolean N = hasSameTile(mapData, x, y - 1, terrain) || (solid && isSolidTile(mapData, x, y - 1));
        boolean NE = hasSameTile(mapData, x + 1, y - 1, terrain) || (solid && isSolidTile(mapData, x + 1, y - 1));
        boolean E = hasSameTile(mapData, x + 1, y, terrain) || (solid && isSolidTile(mapData, x + 1, y));
        boolean SE = hasSameTile(mapData, x + 1, y + 1, terrain) || (solid && isSolidTile(mapData, x + 1, y + 1));
        boolean S = hasSameTile(mapData, x, y + 1, terrain) || (solid && isSolidTile(mapData, x, y + 1));
        boolean SW = hasSameTile(mapData, x - 1, y + 1, terrain) || (solid && isSolidTile(mapData, x - 1, y + 1));
        boolean W = hasSameTile(mapData, x - 1, y, terrain) || (solid && isSolidTile(mapData, x - 1, y));
        boolean NW = hasSameTile(mapData, x - 1, y - 1, terrain) || (solid && isSolidTile(mapData, x - 1, y - 1));

        // 2x2
        Spatial model = new Node();
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {

                int pieceNumber = 0;
                float yAngle = 0;
                Vector3f movement = null;

                // Determine the piece
                if (i == 0 && k == 0) { // North west corner
                    if (N && W && NW) {
                        pieceNumber = 3;
                    } else if (N && W && !NW) {
                        pieceNumber = 2;
                    } else if (!N && !W) {
                        pieceNumber = 1;
                    } else if (N && !W) {
                        pieceNumber = 4;
                    }

                    yAngle = FastMath.PI;
                    movement = new Vector3f(-TILE_WIDTH * 2, 0, 0);
                } else if (i == 1 && k == 0) { // North east corner
                    if (N && E && NE) {
                        pieceNumber = 3;
                    } else if (N && E && !NE) {
                        pieceNumber = 2;
                    } else if (!N && !E) {
                        pieceNumber = 1;
                    } else if (!N && E) {
                        pieceNumber = 4;
                    }

                    yAngle = FastMath.HALF_PI;
                } else if (i == 0 && k == 1) { // South west corner
                    if (S && W && SW) {
                        pieceNumber = 3;
                    } else if (S && W && !SW) {
                        pieceNumber = 2;
                    } else if (!S && !W) {
                        pieceNumber = 1;
                    } else if (!S && W) {
                        pieceNumber = 4;
                    }

                    yAngle = -FastMath.HALF_PI;
                    movement = new Vector3f(-TILE_WIDTH * 2, 0, 0);
                } else if (i == 1 && k == 1) { // South east corner
                    if (S && E && SE) {
                        pieceNumber = 3;
                    } else if (S && E && !SE) {
                        pieceNumber = 2;
                    } else if (!S && !E) {
                        pieceNumber = 1;
                    } else if (S && !E) {
                        pieceNumber = 4;
                    }
                }

                // Load the piece
                Spatial part = loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + modelName + pieceNumber + ".j3o", false);
                if (yAngle != 0) {
                    part.rotate(0, yAngle, 0);
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
}
