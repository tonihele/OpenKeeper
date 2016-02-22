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
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.AssetsConverter;
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
    private final static int[] wallIndexes = new int[]{8, 7};
    private Node root;
    private final String tooltip;

    public GenericRoom(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        this.assetManager = assetManager;
        this.roomInstance = roomInstance;
        this.direction = direction;

        // Strings
        ResourceBundle bundle = Main.getResourceBundle("Interface/Texts/Text");
        tooltip = bundle.getString(Integer.toString(roomInstance.getRoom().getTooltipStringId()));
    }

    public Spatial construct() {

        // Add the floor
        BatchNode floorNode = new BatchNode("Floor");
        contructFloor(floorNode);
        floorNode.setShadowMode(getFloorShadowMode());
        floorNode.batch();
        getRootNode().attachChild(floorNode);

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
        
    }

    public Spatial getWallSpatial(Point start, WallSection.WallDirection direction) {
       
        String resource = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();

        for (WallSection section : roomInstance.getWallPoints()) {

            if (section.getDirection() != direction) {
                continue;
            }
            
            float yAngle = FastMath.PI;
            Vector3f moveFirst = new Vector3f(-0.75f, 0, -0.25f);
            Vector3f moveSecond = new Vector3f(-0.25f, 0, -0.25f);
            if (section.getDirection() == WallSection.WallDirection.WEST) {
                //yAngle = FastMath.PI / 2;
                moveFirst = new Vector3f(-0.25f, 0, -0.25f);
                moveSecond = new Vector3f(-0.75f, 0, -0.25f);
            } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                //yAngle = FastMath.PI;
                moveFirst = new Vector3f(-0.25f, 0, -0.25f);
                moveSecond = new Vector3f(-0.75f, 0, -0.25f);
            } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                //yAngle = -FastMath.PI / 2;
                moveFirst = new Vector3f(-0.75f, 0, -0.25f);
                moveSecond = new Vector3f(-0.25f, 0, -0.25f);
            }

            // Reset wall index for each wall section
            resetWallIndex();
            
            int i = 0;
            for (Point p : section.getCoordinates()) {
                if (start.equals(p)) {
                    Spatial spatial;
                    if (i == 0 || i == (section.getCoordinates().size() - 1)) {
                        spatial = new BatchNode();
                        int firstPiece = (i == 0 ? 4 : 6);
                        if (firstPiece == 4 && (section.getDirection() == WallSection.WallDirection.EAST
                                || section.getDirection() == WallSection.WallDirection.NORTH)) {
                            firstPiece = 5; // The sorting direction forces us to do this
                        }

                        // Load the piece
                        Spatial part = assetManager.loadModel(resource + firstPiece + ".j3o");
                        resetSpatial(part);
                        part.move(moveFirst);
                        if (yAngle != 0) {
                            part.rotate(0, yAngle, 0);
                        }
                        ((BatchNode) spatial).attachChild(part);

                        // Second
                        int secondPiece = (i == (section.getCoordinates().size() - 1) ? 5 : 6);
                        if (secondPiece == 5 && (section.getDirection() == WallSection.WallDirection.EAST
                                || section.getDirection() == WallSection.WallDirection.NORTH)) {
                            secondPiece = 4; // The sorting direction forces us to do this
                        }

                        part = assetManager.loadModel(resource + secondPiece + ".j3o");
                        resetSpatial(part);
                        part.move(moveSecond);
                        if (yAngle != 0) {
                            part.rotate(0, yAngle, 0);
                        }
                        ((BatchNode) spatial).attachChild(part);

                        ((BatchNode) spatial).batch();

                        //spatial.move(-0.5f, 0, -0.5f);
                    } else {

                        // Complete walls, 8, 7, 8, 7 and so forth
                        spatial = assetManager.loadModel(resource + getWallIndexNext() + ".j3o");
                        resetSpatial(spatial);
                        if (yAngle != 0) {
                            spatial.rotate(0, yAngle, 0);
                        }
                        if (section.getDirection() == WallSection.WallDirection.WEST) {
                            spatial.move(-0.5f, 0, 0.5f);
                        } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                            spatial.move(0.5f, 0, 0.5f);
                        } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                            spatial.move(0.5f, 0, -0.5f);
                        } else {
                            spatial.move(-0.5f, 0, -0.5f);
                        }
                    }

                    return spatial;
                } else if (i != 0 && i != (section.getCoordinates().size() - 1)) {
                    getWallIndexNext();                    
                }

                i++;
            }
        }
        return null;
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

    protected void resetSpatial(Spatial tile) {
        if (tile instanceof Node) {
            for (Spatial subSpat : ((Node) tile).getChildren()) {
                subSpat.setLocalScale(MapLoader.TILE_WIDTH);
                subSpat.setLocalTranslation(0, 0, 0);
            }
        } else {
            tile.setLocalScale(MapLoader.TILE_WIDTH);
            tile.setLocalTranslation(0, 0, 0);
        }
        tile.move(0, -MapLoader.TILE_HEIGHT, 0);
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

    /**
     * Override this to report any room obtacles
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return true if accessible
     */
    public boolean isTileAccessible(int x, int y) {
        return true;
    }

    /**
     * Get room tooltip
     *
     * @return room tooltip
     */
    public String getTooltip() {
        return tooltip;
    }

}
