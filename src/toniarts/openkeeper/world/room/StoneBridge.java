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
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.world.MapLoader;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import static toniarts.openkeeper.world.MapLoader.loadAsset;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 *
 * @author ArchDemon
 */
public class StoneBridge extends Quad {

    public StoneBridge(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader) {
        super(assetManager, roomInstance, objectLoader);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();
        Point start = roomInstance.getCoordinates().get(0);

        // Contruct the tiles Wooden Bridge
        for (Point p : roomInstance.getCoordinates()) {
            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            //boolean NE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            //boolean SE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            //boolean SW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            //boolean NW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));
            // 2x2
            Node model = new Node();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {

                    int pieceNumber = 0;
                    float yAngle = 0;
                    // Determine the piece
                    // 2 = 3
                    if (i == 1 && j == 1) { // North west corner
                        if (!N && !W) {
                            pieceNumber = 1;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && W) {
                            pieceNumber = 2;
                        } else if (!W) {
                            yAngle = -FastMath.HALF_PI;
                        }
                    } else if (i == 0 && j == 1) { // North east corner
                        if (!N && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.PI;
                        } else if (N && E) {
                            pieceNumber = 2;
                        } else if (!E) {
                            yAngle = -FastMath.HALF_PI;
                        } else {
                            yAngle = FastMath.PI;
                        }
                    } else if (i == 1 && j == 0) { // South west corner
                        if (!S && !W) {
                            pieceNumber = 1;
                        } else if (S && W) {
                            pieceNumber = 2;
                        } else if (!W) {
                            yAngle = FastMath.HALF_PI;
                        }
                    } else if (i == 0 && j == 0) { // South east corner
                        if (!S && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.HALF_PI;
                        } else if (S && E) {
                            pieceNumber = 2;
                        } else if (!E) {
                            yAngle = FastMath.HALF_PI;
                        } else {
                            yAngle = FastMath.PI;
                        }
                    }
                    // Load the piece
                    Spatial part = loadAsset(assetManager, modelName + pieceNumber + ".j3o", false);
                    resetAndMoveSpatial(part, start, p);
                    if (yAngle != 0) {
                        part.rotate(0, yAngle, 0);
                    }
                    part.move(i * TILE_WIDTH / 2 - TILE_WIDTH / 4, 0, j * TILE_WIDTH / 2 - TILE_WIDTH / 4);

                    model.attachChild(part);
                }
            }
            root.attachChild(model);
        }
        // Set the transform and scale to our scale and 0 the transform
        root.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, -0.1f, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        root.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return root;
    }
}
