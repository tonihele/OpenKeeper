package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.Utils;
import toniarts.openkeeper.world.MapLoader;

/**
 *
 * @author ArchDemon
 */
public class HeroGateTwoByTwo {

    private HeroGateTwoByTwo() {
    }

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance) {
        BatchNode n = new BatchNode();
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();


        // Contruct the tiles
        int i = 0;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = assetManager.loadModel(Utils.getCanonicalAssetKey(modelName + i++ + ".j3o"));
            // Reset
            resetAndMoveSpatial((Node) tile, start, p);
            // Set the shadows
            // TODO: optimize, set to individual pieces and see zExtend whether it casts or not
            tile.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            n.attachChild(tile);
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...
        n.batch();

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
        tile.move(p.x - start.x, -MapLoader.TILE_HEIGHT, p.y - start.y);
    }
}
