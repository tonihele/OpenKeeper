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
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;

/**
 * Base class for all rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class GenericRoom {

    protected final AssetManager assetManager;
    protected final RoomInstance roomInstance;
    protected final Thing.Room.Direction direction;
    private int wallPointer = -1;
    private static int[] wallIndexes = new int[]{8, 7};
    private Node root;

    public GenericRoom(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        this.assetManager = assetManager;
        this.roomInstance = roomInstance;
        this.direction = direction;
    }

    public Spatial construct() {

        // Add the floor
        BatchNode floorNode = new BatchNode("Floor");
        contructFloor(floorNode);
        floorNode.setShadowMode(getFloorShadowMode());
        floorNode.batch();
        getRootNode().attachChild(floorNode);

        // Add the wall
        if (MapLoader.hasRoomWalls(roomInstance.getRoom())) {
            BatchNode wallNode = new BatchNode("Wall");
            contructWall(wallNode);
            if (!wallNode.getChildren().isEmpty()) {
                wallNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                wallNode.batch();
            }
            getRootNode().attachChild(wallNode);
        }
        return getRootNode();
    }

    /**
     * Get the room's root node
     *
     * @return the root node
     */
    public Node getRootNode() {
        if (root == null) {
            root = new Node(roomInstance.getRoom().getName());
        }
        return root;
    }

    protected abstract void contructFloor(Node root);

    protected RenderQueue.ShadowMode getFloorShadowMode() {
        return RenderQueue.ShadowMode.Receive;
    }

    protected void contructWall(Node root) {

        // Get the wall points
        Point start = roomInstance.getCoordinates().get(0);
        for (WallSection section : roomInstance.getWallPoints()) {
            int i = 0;

            // Reset wall index for each wall section
            resetWallIndex();

            for (Point p : section.getCoordinates()) {

                // 4,5,6 are half walls
                // First
                if (i == 0 || i == (section.getCoordinates().size() - 1)) {

                    int firstPiece = (i == 0 ? 4 : 6);
                    if (firstPiece == 4 && (section.getDirection() == WallSection.WallDirection.WEST || section.getDirection() == WallSection.WallDirection.SOUTH)) {
                        firstPiece = 5; // The sorting direction forces us to do this
                    }

                    Quaternion quat = null;
                    if (section.getDirection() == WallSection.WallDirection.WEST) {
                        quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                    } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                        quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                    } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                        quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                    }

                    // Load the piece
                    Spatial part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + firstPiece + ".j3o");
                    resetAndMoveSpatial(part, start, new Point(start.x + p.x, start.y + p.y));
                    if (quat != null) {
                        part.rotate(quat);
                    }
                    part.move(-0.75f, 0, -0.75f);
                    if (section.getDirection() == WallSection.WallDirection.WEST) {
                        part.move(0, 0, 0);
                    } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                        part.move(0, 0, 0.5f);
                    } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                        part.move(0.5f, 0, 0);
                    }
                    root.attachChild(part);

                    int secondPiece = (i == (section.getCoordinates().size() - 1) ? 5 : 6);
                    if (secondPiece == 5 && (section.getDirection() == WallSection.WallDirection.WEST || section.getDirection() == WallSection.WallDirection.SOUTH)) {
                        secondPiece = 4; // The sorting direction forces us to do this
                    }

                    part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + secondPiece + ".j3o");
                    resetAndMoveSpatial(part, start, new Point(start.x + p.x, start.y + p.y));
                    if (quat != null) {
                        part.rotate(quat);
                    }
                    part.move(-0.25f, 0, -0.75f);
                    if (section.getDirection() == WallSection.WallDirection.WEST) {
                        part.move(-0.5f, 0, 0.5f);
                    } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                        part.move(0, 0, 0.5f);
                    } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                        part.move(0, 0, 0.5f);
                    }
                    root.attachChild(part);
                } else {

                    // Complete walls, 8, 7, 8, 7 and so forth
                    Spatial part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + getWallIndexNext() + ".j3o");
                    resetAndMoveSpatial(part, start, new Point(start.x + p.x, start.y + p.y));
                    if (section.getDirection() == WallSection.WallDirection.WEST) {
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                        part.rotate(quat);
                    } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                        part.rotate(quat);
                    } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                        Quaternion quat = new Quaternion();
                        quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                        part.rotate(quat);
                    }
                    part.move(-0.5f, 0, -0.5f);
                    root.attachChild(part);
                }

                i++;
            }
        }
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, spatial
     * @param start start point
     * @param p the tile point
     */
    protected void resetAndMoveSpatial(Spatial tile, Point start, Point p) {

        // Reset, really, the size is 1 after this...
        if (tile instanceof Node) {
            for (Spatial subSpat : ((Node) tile).getChildren()) {
                subSpat.setLocalScale(1);
                subSpat.setLocalTranslation(0, 0, 0);
            }
        } else {
            tile.setLocalScale(1);
            tile.setLocalTranslation(0, 0, 0);
        }
        tile.move(p.x - start.x, -MapLoader.TILE_HEIGHT, p.y - start.y);
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, spatial
     * @param start start point
     */
    protected void resetAndMoveSpatial(Node tile, Point start) {

        // Reset, really, the size is 1 after this...
        for (Spatial subSpat : tile.getChildren()) {
            subSpat.setLocalScale(MapLoader.TILE_WIDTH);
            subSpat.setLocalTranslation(0, 0, 0);
        }
        tile.move(start.x, -MapLoader.TILE_HEIGHT, start.y);
    }

    /**
     * Redraw the walls, and the walls only
     *
     * @param node the room
     */
    public void updateWalls(Spatial node) {
        BatchNode wallNode = (BatchNode) ((Node) node).getChild("Wall");
        wallNode.detachAllChildren();
        contructWall(wallNode);
        wallNode.batch();
    }

    /**
     * Get the wall indexes, whole wall sections. The index means the index
     * suffix on a wall model
     *
     * @return list of wall indexes
     */
    protected int[] getWallIndexes() {
        return wallIndexes;
    }

    /**
     * Get next wall index
     *
     * @return the next wall index
     */
    public int getWallIndexNext() {
        wallPointer++;
        if (wallPointer >= getWallIndexes().length) {
            wallPointer = 0;
        }
        return getWallIndexes()[wallPointer];
    }

    /**
     * Restart the wall counter
     */
    public void resetWallIndex() {
        wallPointer = -1;
    }
}
