package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.Utils;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;

/**
 *
 * @author ArchDemon
 */
public class HeroGateThreeByOne {

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        BatchNode n = new BatchNode();
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();
        Point center = roomInstance.getCenter();
        // Contruct the tiles
        for (int j = 0; j < 2; j++) {
            for (int i = -2; i < 3; i++) {
                Spatial tile;
                if (i == -2 || i == 2) {
                    if (j != 0) {
                        continue;
                    }
                    tile = assetManager.loadModel(Utils.getCanonicalAssetKey(modelName + "6" + ".j3o"));
                    tile.rotate(0, -FastMath.PI / i, 0);
                    tile.move(0, 0, -i / 2);
                } else {
                    tile = assetManager.loadModel(Utils.getCanonicalAssetKey(modelName + (3 * j + i + 1) + ".j3o"));
                }
                // Reset
                resetAndMoveSpatial((Node) tile, new Point(0, i));
                // Set the shadows
                // TODO: optimize, set to individual pieces and see zExtend whether it casts or not
                tile.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

                n.attachChild(tile);
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        switch (direction) {
            case NORTH:
                n.rotate(0, -FastMath.HALF_PI, 0);
                break;
            case EAST:
                n.rotate(0, FastMath.PI, 0);
                break;
            case SOUTH:
                n.rotate(0, FastMath.HALF_PI, 0);
                break;
            default:
                break;
        }
        n.move(center.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, center.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
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
     */
    private static void resetAndMoveSpatial(Node tile, Point start) {

        // Reset, really, the size is 1 after this...
        for (Spatial subSpat : tile.getChildren()) {
            subSpat.setLocalScale(MapLoader.TILE_WIDTH);
            subSpat.setLocalTranslation(0, 0, 0);
        }
        tile.move(start.x, -MapLoader.TILE_HEIGHT, start.y);
    }
}
