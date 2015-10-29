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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.world.MapLoader;

/**
 * Constructs "normal" rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Normal extends RoomConstructor {

    private Normal() {
    }

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance) {
        Node n = new Node(roomInstance.getRoom().getName());

        // Normal rooms
        Point start = roomInstance.getCoordinates().get(0);
        boolean[][] map = roomInstance.getCoordinatesAsMatrix();
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[x].length; y++) {

                // There are 4 different floor pieces and pillars
                // NOTE: I don't understand how the pillars are referenced, they are neither in Terrain nor Room, but they are listed in the Objects. Even Lair has pillars, but I've never seem the in-game
                // Pillars go into all at least 3x3 corners, there can be more than 4 pillars per room
                Node tile = new Node();

                // Figure out which piece by seeing the neighbours
                boolean N = hasSameTile(map, x, y - 1);
                boolean NE = hasSameTile(map, x + 1, y - 1);
                boolean E = hasSameTile(map, x + 1, y);
                boolean SE = hasSameTile(map, x + 1, y + 1);
                boolean S = hasSameTile(map, x, y + 1);
                boolean SW = hasSameTile(map, x - 1, y + 1);
                boolean W = hasSameTile(map, x - 1, y);
                boolean NW = hasSameTile(map, x - 1, y - 1);

                for (int i = 0; i < 2; i++) {
                    for (int k = 0; k < 2; k++) {

                        int pieceNumber = 0;
                        Quaternion quat = null;
                        Vector3f movement = null;

                        // Determine the piece
                        if (i == 0 && k == 0) { // North west corner
                            if (N && W && NW) {
                                pieceNumber = 3;
                            } else if (!N && W && NW) {
                                pieceNumber = 0;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                            } else if (!NW && N && W) {
                                pieceNumber = 2;
                            } else if (!N && !W) {
                                pieceNumber = 1;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                            } else if (W && !NW && !N) {
                                pieceNumber = 0;
                            } else if (!W && !NW && N) {
                                pieceNumber = 0;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                            } else {
                                pieceNumber = 0;
                            }
//                            movement = new Vector3f(-0.5f, -0.5f, 0);
                        } else if (i == 1 && k == 0) { // North east corner
                            if (N && E && NE) {
                                pieceNumber = 3;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                            } else if (!N && E && NE) {
                                pieceNumber = 0;
                            } else if (!NE && N && E) {
                                pieceNumber = 2;
                            } else if (!N && !E) {
                                pieceNumber = 1;
                            } else if (!E && NE && N) {
                                pieceNumber = 0;
                            } else if (!E && !NE && N) {
                                pieceNumber = 0;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                            } else {
                                pieceNumber = 0;
                            }
//                            movement = new Vector3f(0, -0.5f, 0);
                        } else if (i == 0 && k == 1) { // South west corner
                            if (S && W && SW) {
                                pieceNumber = 3;
                            } else if (!S && W && SW) {
                                pieceNumber = 4;
                            } else if (!SW && S && W) {
                                pieceNumber = 2;
                            } else if (!S && !W) {
                                pieceNumber = 1;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                            } else if (!W && SW && S) {
                                pieceNumber = 0;
                            } else if (!W && !SW && S) {
                                pieceNumber = 0;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                            } else {
                                pieceNumber = 0;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                            }

//                            movement = new Vector3f(-0.5f, 0, 0);
                        } else if (i == 1 && k == 1) { // South east corner
                            if (S && E && SE) {
                                pieceNumber = 3;
                            } else if (!S && E && SE) {
                                pieceNumber = 0;
                            } else if (!SE && S && E) {
                                pieceNumber = 2;
                            } else if (!S && !E) {
                                pieceNumber = 1;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                            } else if (!E && SE && S) {
                                pieceNumber = 4;
                            } else if (!E && !SE && S) {
                                pieceNumber = 0;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                            } else {
                                pieceNumber = 0;
                                quat = new Quaternion();
                                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                            }
                        }

                        // Load the piece
                        Spatial part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + pieceNumber + ".j3o");
                        resetAndMoveSpatial(part, start, new Point(start.x + x, start.y + y));
                        if (quat != null) {
                            part.rotate(quat);
                        }
                        if (movement != null) {
                            part.move(movement);
                        }
                        part.move(i * 0.5f - 0.25f, 0, k * 0.5f - 0.25f);
                        tile.attachChild(part);
                    }
                }

                n.attachChild(tile);
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return n;
    }

    private static boolean hasSameTile(boolean[][] map, int x, int y) {

        // Check for out of bounds
        if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
            return false;
        }
        return map[x][y];
    }
}
