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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.Arrays;
import java.util.EnumSet;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.room.WallSection.WallDirection;

/**
 * Constructs "normal" rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Normal extends GenericRoom {

    protected final boolean[][] map;
    protected final Point start;

    public Normal(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);

        map = roomInstance.getCoordinatesAsMatrix();
        start = roomInstance.getMatrixStartPoint();
    }

    @Override
    public Spatial construct() {
        super.construct();

        // Pillars
        if (hasPillars()) {
            BatchNode pillarsNode = new BatchNode("Pillars");
            contructPillars(pillarsNode);
            if (!pillarsNode.getChildren().isEmpty()) {
                pillarsNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                pillarsNode.batch();
                getRootNode().attachChild(pillarsNode);
            }
        }

        return getRootNode();
    }

    @Override
    protected void contructFloor(Node n) {

        // Normal rooms
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[x].length; y++) {

                // Skip non-room tiles
                if (!map[x][y]) {
                    continue;
                }

                // There are 4 different floor pieces and pillars
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
    }

    private boolean hasSameTile(boolean[][] map, int x, int y) {

        // Check for out of bounds
        if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
            return false;
        }
        return map[x][y];
    }

    /**
     * Does the room have pillars
     *
     * @return has pillars
     */
    protected boolean hasPillars() {
        return true; // I have no idea where to get this data
    }

    /**
     * Construct room pillars. Info in:
     * https://github.com/tonihele/OpenKeeper/issues/116
     *
     * @param node the pillar node
     */
    protected void contructPillars(Node node) {

        // NOTE: I don't understand how the pillars are referenced, they are neither in Terrain nor Room, but they are listed in the Objects. Even Lair has pillars, but I've never seem the in-game
        // Pillars go into all at least 3x3 corners, there can be more than 4 pillars per room

        // Go through all the points and see if they are fit for pillar placement
        for (Point p : roomInstance.getCoordinates()) {

            // See that we have 2 "free" neigbouring tiles
            EnumSet<WallDirection> freeDirections = EnumSet.noneOf(WallDirection.class);
            if (!hasSameTile(map, p.x - start.x, p.y - start.y - 1)) { // North
                freeDirections.add(WallDirection.NORTH);
            }
            if (!hasSameTile(map, p.x - start.x, p.y - start.y + 1)) { // South
                freeDirections.add(WallDirection.SOUTH);
            }
            if (!hasSameTile(map, p.x - start.x + 1, p.y - start.y)) { // East
                freeDirections.add(WallDirection.EAST);
            }
            if (!hasSameTile(map, p.x - start.x - 1, p.y - start.y)) { // West
                freeDirections.add(WallDirection.WEST);
            }

            // If we have 2, see the other directions that they have 3x3 the same tile
            if (freeDirections.size() == 2 && !freeDirections.containsAll(Arrays.asList(WallDirection.NORTH, WallDirection.SOUTH)) && !freeDirections.containsAll(Arrays.asList(WallDirection.WEST, WallDirection.EAST))) {
                boolean found = true;
                loop:
                for (int y = 0; y < 3; y++) {
                    for (int x = 0; x < 3; x++) {

                        // Get the tile from wanted direction
                        int xPoint = x;
                        int yPoint = y;
                        if (freeDirections.contains(WallDirection.EAST)) { // Go west
                            xPoint = -x;
                        }
                        if (freeDirections.contains(WallDirection.SOUTH)) { // Go north
                            yPoint = -y;
                        }
                        if (!hasSameTile(map, p.x - start.x + xPoint, p.y - start.y + yPoint)) {
                            found = false;
                            break loop;
                        }
                    }
                }

                // Add
                if (found) {

                    // Contruct a pillar
                    Spatial part = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "_Pillar.j3o"));
                    resetAndMoveSpatial(part, new Point(0, 0), p);

                    // Face "in" diagonally
                    if (freeDirections.contains(WallDirection.NORTH) && freeDirections.contains(WallDirection.EAST)) {
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                        part.rotate(quat);
                    } else if (freeDirections.contains(WallDirection.SOUTH) && freeDirections.contains(WallDirection.EAST)) {
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                        part.rotate(quat);
                    } else if (freeDirections.contains(WallDirection.SOUTH) && freeDirections.contains(WallDirection.WEST)) {
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                        part.rotate(quat);
                    }

                    part.move(-0.5f, MapLoader.TILE_HEIGHT, -0.5f);
                    node.attachChild(part);
                }
            }
        }
    }
}
