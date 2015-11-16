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
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import static toniarts.openkeeper.world.MapLoader.loadAsset;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
public class Prison extends GenericRoom {

    public Prison(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);
    }

    @Override
    protected Spatial contructFloor() {
        Node n = new Node(roomInstance.getRoom().getName());
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();
        String modelName2 = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName().toLowerCase();
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
                Spatial part = loadAsset(assetManager, modelName + "14" + ".j3o", false);

                resetAndMoveSpatial(part, start, p);
                hasDoor = true;
                part.move(-TILE_WIDTH / 4, 0, -TILE_WIDTH / 4);

                n.attachChild(part);

                continue;
            }

            Node model = new Node();

            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {
                    // 9 - stone floor
                    // 8 stone wall with skeleton
                    // 7 stone wall
                    // 6 stone wall half
                    // 5 stone wall half
                    // 4 stone wall half
                    // 3 -  dirt
                    // 2 -  dirt corner
                    // 19 - dirt with zabor
                    // 17 - dirt
                    // 15 dirt with zabor
                    // 14 - dirt with gate
                    // 13 - dirt
                    // 12 - dirt corner with zabor
                    // 11 dirt corner with palka
                    // 10 - dirt with zabor
                    // 1 - dirt corner with brics ?
                    // 0 dirt with brics
                    // Prison_Pillar
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
                            pieceNumber = 17;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 17;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 19;
                            yAngle = FastMath.PI;
                        } else if (E && W && !S) {
                            pieceNumber = 10;
                            //yAngle = FastMath.PI;
                        }
                    } else if (i == 1 && k == 0) { // North east corner
                        if (!N && !E) {
                            pieceNumber = 1;
                            //yAngle = -FastMath.HALF_PI;
                        } else if (!S && !W) {
                            pieceNumber = 11;
                        } else if ((W || E) && !N) {
                            pieceNumber = 17;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !E) {
                            pieceNumber = 17;
                            yAngle = FastMath.HALF_PI;
                        } else if (N && S && !W) {
                            pieceNumber = 19;
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
                            pieceNumber = 17;
                        } else if ((N || S) && !W) {
                            pieceNumber = 17;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 19;
                            yAngle = FastMath.PI;
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
                            pieceNumber = 17;
                        } else if ((N || S) && !E) {
                            pieceNumber = 17;
                            yAngle = FastMath.HALF_PI;
                        } else if (S && E && !W) {
                            pieceNumber = 19;
                            //yAngle = FastMath.PI;
                        } else if (E && W && !N) {
                            pieceNumber = 10;
                            yAngle = FastMath.PI;
                        }
                    }
                    // Load the piece
                    Spatial part;
                    try {
                        part = loadAsset(assetManager, modelName + pieceNumber + ".j3o", false);
                    } catch (Exception ex) {
                        part = loadAsset(assetManager, modelName2 + pieceNumber + ".j3o", false);
                    }
                    resetAndMoveSpatial(part, start, p);
                    if (yAngle != 0) {
                        part.rotate(0, yAngle, 0);
                    }
                    part.move(TILE_WIDTH / 4 - i * TILE_WIDTH / 2, 0, TILE_WIDTH / 4 - k * TILE_WIDTH / 2);

                    model.attachChild(part);

                }
            }

            n.attachChild(model);
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return n;
    }
}
