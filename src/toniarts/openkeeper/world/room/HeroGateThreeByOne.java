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
import com.jme3.math.FastMath;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Thing;
import static toniarts.openkeeper.tools.convert.map.Thing.Room.Direction.EAST;
import static toniarts.openkeeper.tools.convert.map.Thing.Room.Direction.NORTH;
import static toniarts.openkeeper.tools.convert.map.Thing.Room.Direction.SOUTH;
import toniarts.openkeeper.world.MapLoader;

/**
 *
 * @author ArchDemon
 */
public class HeroGateThreeByOne extends GenericRoom {

    public HeroGateThreeByOne(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);
    }

    @Override
    protected Spatial contructFloor() {
        Node n = new Node(roomInstance.getRoom().getName());
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();
        Point center = roomInstance.getCenter();
        // Contruct the tiles
        for (int j = 0; j < 2; j++) {
            for (int i = -2; i < 3; i++) {
                Spatial tile;
                if (i == -2 || i == 2) {
                    if (j != 0) {
                        continue;
                    }
                    tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(modelName + "6" + ".j3o"));
                    tile.rotate(0, -FastMath.PI / i, 0);
                    tile.move(0, 0, -i / 2);
                } else {
                    tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(modelName + (3 * j + i + 1) + ".j3o"));
                }

                // Reset
                resetAndMoveSpatial((Node) tile, new Point(0, i));

                n.attachChild(tile);
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        switch (direction) {
            case NORTH:
                n.rotate(0, -FastMath.HALF_PI, 0);
                break;
            case EAST:
                n.rotate(0, FastMath.PI, 0);
                break;
            case SOUTH:
                n.rotate(0, FastMath.HALF_PI, 0);
                break;
            default:
                break;
        }
        n.move(center.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, center.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return n;
    }
}
