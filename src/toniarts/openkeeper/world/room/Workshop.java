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
import java.util.EnumSet;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;

/**
 * The workshop
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Workshop extends Normal {

    private boolean[][] bigTiles;

    public Workshop(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);
    }

    @Override
    protected void setupCoordinates() {
        super.setupCoordinates();

        // Big tile map
        bigTiles = new boolean[map.length][map[0].length];
    }

    @Override
    protected boolean useBigFloorTile(int x, int y) {

        // In workshop a big tile can't be in touch with other big tiles (except diagonally)
        // There is also catch, not always the big tile (a grill), sometimes normal + saw table
        // The logic is unknown to me, something maybe to do with the coordinates, not totally random it seems
        boolean N = hasSameTile(bigTiles, x, y - 1);
        boolean E = hasSameTile(bigTiles, x + 1, y);
        boolean S = hasSameTile(bigTiles, x, y + 1);
        boolean W = hasSameTile(bigTiles, x - 1, y);
        if (!N && !E && !S && !W) {
            bigTiles[x][y] = true;
            return true;
        }
        return false;
    }

    @Override
    protected void contructPillars(Node node) {

        // We have very different logic than the normal
        // Go through all the points and see if they are fit for pillar placement
        for (Point p : roomInstance.getCoordinates()) {

            // See that we have 2 "free" neigbouring tiles
            EnumSet<WallSection.WallDirection> freeDirections = EnumSet.noneOf(WallSection.WallDirection.class);
            if (!hasSameTile(map, p.x - start.x, p.y - start.y - 1)) { // North
                freeDirections.add(WallSection.WallDirection.NORTH);
            }
            if (!hasSameTile(map, p.x - start.x, p.y - start.y + 1)) { // South
                freeDirections.add(WallSection.WallDirection.SOUTH);
            }
            if (!hasSameTile(map, p.x - start.x + 1, p.y - start.y)) { // East
                freeDirections.add(WallSection.WallDirection.EAST);
            }
            if (!hasSameTile(map, p.x - start.x - 1, p.y - start.y)) { // West
                freeDirections.add(WallSection.WallDirection.WEST);
            }

            // We may have up to 4 pillars in the same tile even, every corner gets one, no need to check anything else
            // Add a pillar
            // Face "in" diagonally
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                constructPillar(node, p, quat).move(-0.15f, MapLoader.TILE_HEIGHT, -0.85f);
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
                constructPillar(node, p, quat).move(-0.15f, MapLoader.TILE_HEIGHT, -0.15f);
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                constructPillar(node, p, quat).move(-0.85f, MapLoader.TILE_HEIGHT, -0.15f);
            }
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                constructPillar(node, p, null).move(-0.85f, MapLoader.TILE_HEIGHT, -0.85f);
            }
        }
    }

    private Spatial constructPillar(Node node, Point p, Quaternion quat) {
        Spatial part = assetManager.loadModel(getPillarResource());
        resetAndMoveSpatial(part, new Point(0, 0), p);

        if (quat != null) {
            part.rotate(quat);
        }

        node.attachChild(part);
        return part;
    }
}
