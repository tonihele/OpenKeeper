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
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.MapLoader;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
public class CombatPit extends DoubleQuad {

    public CombatPit(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        Point start = roomInstance.getCoordinates().get(0);
        // Contruct the tiles
        boolean hasDoor = false;

        for (Point p : roomInstance.getCoordinates()) {
            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean NE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            boolean SE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean SW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean NW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));

            if (!hasDoor && !S && N && NE && NW && E && W && !SW && !SE) {
                Spatial part = AssetUtils.loadAsset(assetManager, modelName + "14");

                moveSpatial(part, start, p);
                hasDoor = true;

                part.move(-TILE_WIDTH / 4, 0, -TILE_WIDTH / 4);
                root.attachChild(part);

                continue;
            }

            Node model = new Node();
            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {
                    // 4 - 8 - walls

                    int pieceNumber = 13;
                    float yAngle = 0;
                    // Determine the piece
                    if (i == 0 && k == 0) { // North west corner
                        if (!N && !W) {
                            pieceNumber = 1;
                            yAngle = -FastMath.HALF_PI;
                        } else if (!S && !E) {
                            pieceNumber = 11;
                            yAngle = FastMath.HALF_PI;
                        } else if ((W || E) && !N) {
                            pieceNumber = 0;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 0;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 10;
                            yAngle = FastMath.HALF_PI;
                        } else if (E && W && !S) {
                            pieceNumber = 10;
                            //yAngle = FastMath.PI;
                        }
                    } else if (i == 1 && k == 0) { // North east corner
                        if (!N && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.PI;
                        } else if (!S && !W) {
                            pieceNumber = 11;
                        } else if ((W || E) && !N) {
                            pieceNumber = 0;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !E) {
                            pieceNumber = 0;
                            yAngle = FastMath.HALF_PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 10;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !W) {
                            pieceNumber = 12;
                            //yAngle = FastMath.PI;
                        } else if (E && W && !S) {
                            pieceNumber = 10;
                            //yAngle = FastMath.PI;
                        }
                    } else if (i == 0 && k == 1) { // South west corner
                        if (!S && !W) {
                            pieceNumber = 1;
                        } else if (!N && !E) {
                            pieceNumber = 11;
                            yAngle = FastMath.PI;
                        } else if ((W || E) && !S) {
                            pieceNumber = 0;
                        } else if ((N || S) && !W) {
                            pieceNumber = 0;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 10;
                            yAngle = FastMath.HALF_PI;
                        } else if (E && W && !N) {
                            pieceNumber = 10;
                            yAngle = FastMath.PI;
                        }
                    } else if (i == 1 && k == 1) { // South east corner
                        if (!S && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.HALF_PI;
                        } else if (!N && !W) {
                            pieceNumber = 11;
                            yAngle = -FastMath.HALF_PI;
                        } else if ((W || E) && !S) {
                            pieceNumber = 0;
                        } else if ((N || S) && !E) {
                            pieceNumber = 0;
                            yAngle = FastMath.HALF_PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 10;
                            yAngle = -FastMath.HALF_PI;
                        } else if (S && E && !W) {
                            pieceNumber = 12;
                            //yAngle = FastMath.PI;
                        } else if (E && W && !N) {
                            pieceNumber = 10;
                            yAngle = FastMath.PI;
                        }
                    }
                    // Load the piece
                    Spatial part = AssetUtils.loadAsset(assetManager, modelName + pieceNumber);

                    moveSpatial(part, start, p);
                    if (yAngle != 0) {
                        part.rotate(0, yAngle, 0);
                    }
                    part.move(TILE_WIDTH / 4 - i * TILE_WIDTH / 2, 0, TILE_WIDTH / 4 - k * TILE_WIDTH / 2);
                    model.attachChild(part);

                }
            }

            root.attachChild(model);
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.moveToTile(root, start);
        //root.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return root;
    }
}
