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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
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

    public GenericRoom(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        this.assetManager = assetManager;
        this.roomInstance = roomInstance;
        this.direction = direction;
    }

    public Spatial construct() {
        Node node = new Node(roomInstance.getRoom().getName());

        // Add the floor
        BatchNode floorNode = new BatchNode("Floor");
        floorNode.attachChild(contructFloor());
        floorNode.setShadowMode(getFloorShadowMode());
        floorNode.batch();
        node.attachChild(floorNode);

        // Add the wall
        Spatial wall = contructWall();
        if (wall != null) {
            BatchNode wallNode = new BatchNode("Wall");
            wallNode.attachChild(wall);
            wallNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            wallNode.batch();
            node.attachChild(wallNode);
        }
        return node;
    }

    protected abstract Spatial contructFloor();

    protected RenderQueue.ShadowMode getFloorShadowMode() {
        return RenderQueue.ShadowMode.Receive;
    }

    protected Spatial contructWall() {
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
}
