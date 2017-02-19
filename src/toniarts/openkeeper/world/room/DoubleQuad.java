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
package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 *
 * @author ArchDemon
 */
public class DoubleQuad extends GenericRoom {

    public DoubleQuad(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        Point start = roomInstance.getCoordinates().get(0);

        // Contruct the tiles
        int i = 0;
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = AssetUtils.loadTerrainWithoutCache(assetManager, modelName + i);
            // Reset
            moveSpatial(tile, start, p);
            // FIXME: this not correct
            switch (i) {
                case 0:
                    tile.move(-MapLoader.TILE_WIDTH / 4, 0, -MapLoader.TILE_WIDTH / 4);
                    break;
                case 1:
                    tile.move(-MapLoader.TILE_WIDTH / 4, 0, MapLoader.TILE_WIDTH / 4);
                    break;
                case 2:
                    tile.move(MapLoader.TILE_WIDTH / 4, 0, -MapLoader.TILE_WIDTH / 4);
                    break;
                default:
                    tile.move(MapLoader.TILE_WIDTH / 4, 0, MapLoader.TILE_WIDTH / 4);
                    break;
            }

            root.attachChild(tile);

            i++;

            if (i == 16) {
                i = 17;
            }
            if (i == 18) {
                i = 19;
            }
            if (i > 19) {
                i = 0;
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.moveToTile(root, start);
        root.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return root;
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // Only the surroundings are accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(toX, toY);
        boolean N = hasSameTile(map, roomPoint.x, roomPoint.y + 1);
        boolean NE = hasSameTile(map, roomPoint.x - 1, roomPoint.y + 1);
        boolean E = hasSameTile(map, roomPoint.x - 1, roomPoint.y);
        boolean SE = hasSameTile(map, roomPoint.x - 1, roomPoint.y - 1);
        boolean S = hasSameTile(map, roomPoint.x, roomPoint.y - 1);
        boolean SW = hasSameTile(map, roomPoint.x + 1, roomPoint.y - 1);
        boolean W = hasSameTile(map, roomPoint.x + 1, roomPoint.y);
        boolean NW = hasSameTile(map, roomPoint.x + 1, roomPoint.y + 1);
        return !(N && NE && E && SE && S && SW && W && NW);
    }

}
