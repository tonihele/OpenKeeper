/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map.loader.room;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.Utils;
import toniarts.opendungeonkeeper.tools.convert.map.loader.MapLoader;

/**
 * Constructs 5 by 5 "rotated" buildings. As far as I know, only Dungeon Heart
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class FiveByFiveRotated {

    private FiveByFiveRotated() {
    }

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance) {
        Node n = new Node();

        // 5 by 5
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {

            // There are just 4 different pieces
            int x = p.x - start.x;
            int y = p.y - start.y;
            Node tile = null;

            // Corners
            if (x == 0 && y == 0) { // Top left corner
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                tile.rotate(quat);
            } else if (x == 4 && y == 0) { // Top right corner
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (x == 0 && y == 4) { // Lower left corner
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            } else if (x == 4 && y == 4) { // Lower right corner
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "3.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            } // Outer layer sides
            else if (x == 0) { // Left side
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            } else if (x == 4) { // Right side
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (y == 0) { // Top side
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                tile.rotate(quat);
            } else if (y == 4) { // Bottom side
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "2.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            } // The inner ring, corners
            else if (x == 1 && y == 1) { // Top left
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (x == 3 && y == 1) { // Top right
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            } else if (x == 1 && y == 3) { // Bottom left
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                tile.rotate(quat);
            } else if (x == 3 && y == 3) { // Bottom right
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "0.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            } // The inner ring, sides
            else if (x == 1) { // Left
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, -1));
                tile.rotate(quat);
            } else if (x == 3) { // Right
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            } else if (y == 1) { // Top
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
            } else if (y == 3) { // Bottom
                tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + "1.j3o"));
                resetAndMoveSpatial(tile, start, p);
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, 0, 1));
                tile.rotate(quat);
            }

            // Set the shadows
            n.setShadowMode(RenderQueue.ShadowMode.Receive);

            if (tile != null) // Debug
            {
                n.attachChild(tile);
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2, 0);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return n;
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, spatial
     * @param start start point
     * @param p the tile point
     */
    private static void resetAndMoveSpatial(Node tile, Point start, Point p) {

        // Reset, really, the size is 1 after this...
        for (Spatial subSpat : tile.getChildren()) {
            subSpat.setLocalScale(1);
            subSpat.setLocalTranslation(0, 0, 0);
        }
        tile.move(p.x - start.x, p.y - start.y, 1f);
    }
}
