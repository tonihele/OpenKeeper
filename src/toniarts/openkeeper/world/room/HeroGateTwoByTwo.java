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
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 *
 * @author ArchDemon
 */
public class HeroGateTwoByTwo extends GenericRoom {

    public HeroGateTwoByTwo(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState) {
        super(assetManager, roomInstance, objectLoader, worldState);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();

        // Contruct the tiles
        int i = 0;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(modelName + i++ + ".j3o"));

            // Reset
            resetAndMoveSpatial(tile, start, p);

            root.attachChild(tile);
        }

        // Set the transform and scale to our scale and 0 the transform
        root.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        root.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return root;
    }

}
