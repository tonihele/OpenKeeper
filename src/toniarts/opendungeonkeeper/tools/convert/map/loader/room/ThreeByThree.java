/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map.loader.room;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.Utils;
import toniarts.opendungeonkeeper.tools.convert.map.loader.MapLoader;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ThreeByThree {

    private ThreeByThree() {
    }

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance) {
        Node n = new Node();

        // 3 by 3, a simple case
        int i = 0;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Node tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + i + ".j3o"));

            // Reset, really, the size is 1 after this...
            for (Spatial subSpat : tile.getChildren()) {
                subSpat.setLocalScale(1);
                subSpat.setLocalTranslation(0, 0, 0);
            }
            tile.move(p.x - start.x, -1.0f, p.y - start.y);

            // Set the shadows
            // TODO: optimize, set to individual pieces and see zExtend whether it casts or not
            n.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            n.attachChild(tile);
            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return n;
    }
}
