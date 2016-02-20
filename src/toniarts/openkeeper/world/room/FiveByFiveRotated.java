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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;

/**
 * Constructs 5 by 5 "rotated" buildings. As far as I know, only Dungeon Heart
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class FiveByFiveRotated extends GenericRoom {

    public FiveByFiveRotated(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);
    }

    @Override
    protected void contructFloor(Node n) {

        // 5 by 5
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {

            // There are just 4 different pieces
            int x = p.x - start.x;
            int y = p.y - start.y;
            Spatial tile = null;

            // Corners
            if (x == 0 && y == 0) { // Top left corner
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                tile.rotate(quat);
            } else if (x == 4 && y == 0) { // Top right corner
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (x == 0 && y == 4) { // Lower left corner
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            } else if (x == 4 && y == 4) { // Lower right corner
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            } // Outer layer sides
            else if (x == 0) { // Left side
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            } else if (x == 4) { // Right side
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (y == 0) { // Top side
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                tile.rotate(quat);
            } else if (y == 4) { // Bottom side
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            } // The inner ring, corners
            else if (x == 1 && y == 1) { // Top left
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (x == 3 && y == 1) { // Top right
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            } else if (x == 1 && y == 3) { // Bottom left
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                tile.rotate(quat);
            } else if (x == 3 && y == 3) { // Bottom right
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            } // The inner ring, sides
            else if (x == 1) { // Left
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));
                tile.rotate(quat);
            } else if (x == 3) { // Right
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            } else if (y == 1) { // Top
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (y == 3) { // Bottom
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                tile.rotate(quat);
            }

            if (tile != null) // Debug
            {
                n.attachChild(tile);
            }

            // Only observed 5 by 5 is the Dungeon Heart, its object list is empty, so I just hard code these here
            // The center pieces
            if (x == 2 && y == 2) {

                // The arches
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/DHeart Arches.j3o"));
                resetAndMoveSpatial(tile, start, p);
                n.attachChild(tile);

                // The steps between the arches
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/DHeart BigSteps.j3o"));
                resetAndMoveSpatial(tile, start, p);
                n.attachChild(tile);

                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/DHeart BigSteps.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 1.5f, new Vector3f(0, -1, 0));
                tile.rotate(quat);
                n.attachChild(tile);

                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/DHeart BigSteps.j3o"));
                resetAndMoveSpatial(tile, start, p);
                quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 1.5f, new Vector3f(0, 1, 0));
                tile.rotate(quat);
                n.attachChild(tile);

                // The alfa & omega! The heart, TODO: use object loader once it is in decent condition, this after all is a real object
                tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/Dungeon centre.j3o"));
                resetAndMoveSpatial(tile, start, p);

                // Animate
                AnimControl animControl = (AnimControl) ((Node) tile).getChild(0).getControl(AnimControl.class);
                if (animControl != null) {
                    AnimChannel channel = animControl.createChannel();
                    channel.setAnim("anim");
                    channel.setLoopMode(LoopMode.Loop);

                    // Don't batch animated objects, seems not to work
                    tile.setBatchHint(Spatial.BatchHint.Never);
                }
                n.attachChild(tile.move(0, 0.25f, 0));
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...
    }

    @Override
    public boolean isTileAccessible(int x, int y) {

        // The center 3x3 is not accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(x, y);
        return ((roomPoint.x == 0 || roomPoint.x == 4) || (roomPoint.y == 0 || roomPoint.y == 4));
    }

}
