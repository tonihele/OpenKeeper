/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map.loader.room;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
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
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = MapLoader.loadAsset(assetManager, AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + i + ".j3o", false);
            tile.move(p.x * MapLoader.TILE_WIDTH, p.y * MapLoader.TILE_WIDTH, 0);
            n.attachChild(tile);
            i++;
        }

        return n;
    }
}
