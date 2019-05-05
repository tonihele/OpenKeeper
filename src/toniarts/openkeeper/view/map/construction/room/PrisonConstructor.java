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
package toniarts.openkeeper.view.map.construction.room;

import com.jme3.asset.AssetManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.map.construction.DoubleQuadConstructor;
import toniarts.openkeeper.world.MapLoader;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
public class PrisonConstructor extends DoubleQuadConstructor {

    public PrisonConstructor(AssetManager assetManager, toniarts.openkeeper.common.RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();

        boolean door = false;
        for (Point p : roomInstance.getCoordinates()) {

            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean NE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean SE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean SW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            boolean NW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));

            if (!door && !N && !NE && E && SE && S && SW && W && !NW) {
                Spatial part = AssetUtils.loadModel(assetManager, modelName + "14");
                part.move(-MapLoader.TILE_WIDTH / 4, 0, -MapLoader.TILE_WIDTH / 4);
                moveSpatial(part, p);

                root.attachChild(part);

                door = true;
                continue;
            }

            Node model = constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW);
            moveSpatial(model, p);
            root.attachChild(model);
        }

        return root;
    }
}
