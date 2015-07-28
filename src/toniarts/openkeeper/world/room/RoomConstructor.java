
package toniarts.openkeeper.world.room;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.world.MapLoader;

/**
 *
 * @author ArchDemon
 */


public abstract class RoomConstructor {
    protected static void resetAndMoveSpatial(Node tile, Point start, Point p) {

        // Reset, really, the size is 1 after this...
        for (Spatial subSpat : tile.getChildren()) {
            subSpat.setLocalScale(1);
            subSpat.setLocalTranslation(0, 0, 0);
        }
        tile.move(p.x - start.x, -MapLoader.TILE_HEIGHT, p.y - start.y);
    }
}
