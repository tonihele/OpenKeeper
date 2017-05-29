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
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 *
 * @author ArchDemon
 */
public class WaterConstructor extends SingleTileConstructor {

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
        // Figure out which piece by seeing the neighbours
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

        if (S && SW && W && SE && N && NE && E && NW) {
            piece = 3;
        } else if (!E && S && SW && W && NW && N) {
            piece = 0;
            yAngle = FastMath.HALF_PI;
        } else if (!S && W && NW && N && NE && E) {
            piece = 0;
        } else if (!W && N && NE && E && SE && S) {
            piece = 0;
            yAngle = -FastMath.HALF_PI;
        } else if (!N && E && SE && S && SW && W) {
            piece = 0;
            yAngle = FastMath.PI;
        } else if (!SW && S && SE && E && W && N && NE && NW) {
            piece = 2;
            yAngle = FastMath.PI;
        } else if (!NE && S && SE && E && W && N && SW && NW) {
            piece = 2;
        } else if (!SE && S && SW && E && W && N && NE && NW) {
            piece = 2;
            yAngle = -FastMath.HALF_PI;
        } else if (!NW && S && SW && E && W && N && NE && SE) {
            piece = 2;
            yAngle = FastMath.HALF_PI;
        } else if (!N && !NW && !W && S && SE && E) {
            piece = 1;
            yAngle = -FastMath.HALF_PI;
        } else if (!N && !NE && !E && SW && S && W) {
            piece = 1;
            yAngle = FastMath.PI;
        } else if (!S && !SE && !E && N && W && NW) {
            piece = 1;
            yAngle = FastMath.HALF_PI;
        } else if (!S && !SW && !W && N && NE && E) {
            piece = 1;
        }
        //
        if (piece != -1) {
            floor = loadAsset(assetManager, model + piece, true);
            if (yAngle != 0) {
                floor.rotate(0, yAngle, 0);
            }
            return floor;
        }

        // 2x2
        floor = Quad.constructQuad(assetManager, model, 4, FastMath.PI, N, NE, E, SE, S, SW, W, NW);

        return floor;
    }
}
