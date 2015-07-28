package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.world.MapLoader;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import static toniarts.openkeeper.world.MapLoader.loadAsset;
import static toniarts.openkeeper.world.room.RoomConstructor.resetAndMoveSpatial;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
public class CombatPit extends RoomConstructor {

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance) {
        Node n = new Node(roomInstance.getRoom().getName());
        String modelName = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();
        Point start = roomInstance.getCoordinates().get(0);
        // Contruct the tiles
        boolean hasDoor = false;

        for (Point p : roomInstance.getCoordinates()) {
            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean NE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            boolean SE = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean SW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean NW = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));

            if (!hasDoor && !S && N && NE && NW && E && W && !SW && !SE) {
                Spatial part = loadAsset(assetManager, modelName + "14" + ".j3o", false);

                resetAndMoveSpatial((Node) part, start, p);
                hasDoor = true;

                part.move(-TILE_WIDTH / 4, 0, -TILE_WIDTH / 4);
                // Set the shadows
                // TODO: optimize, set to individual pieces and see zExtend whether it casts or not
                part.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                n.attachChild(part);

                continue;
            }

            BatchNode model = new BatchNode();

            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {
                    // 4 - 8 - walls

                    int pieceNumber = 13;
                    float yAngle = 0;
                    // Determine the piece
                    if (i == 0 && k == 0) { // North west corner
                        if (!N && !W) {
                            pieceNumber = 1;
                            yAngle = -FastMath.HALF_PI;
                        } else if (!S && !E) {
                            pieceNumber = 11;
                            yAngle = FastMath.HALF_PI;
                        } else if ((W || E) && !N) {
                            pieceNumber = 0;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 0;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 10;
                            yAngle = FastMath.HALF_PI;
                        } else if (E && W && !S) {
                            pieceNumber = 10;
                            //yAngle = FastMath.PI;
                        }
                    } else if (i == 1 && k == 0) { // North east corner
                        if (!N && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.PI;
                        } else if (!S && !W) {
                            pieceNumber = 11;
                        } else if ((W || E) && !N) {
                            pieceNumber = 0;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !E) {
                            pieceNumber = 0;
                            yAngle = FastMath.HALF_PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 10;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !W) {
                            pieceNumber = 12;
                            //yAngle = FastMath.PI;
                        } else if (E && W && !S) {
                            pieceNumber = 10;
                            //yAngle = FastMath.PI;
                        }
                    } else if (i == 0 && k == 1) { // South west corner
                        if (!S && !W) {
                            pieceNumber = 1;
                        } else if (!N && !E) {
                            pieceNumber = 11;
                            yAngle = FastMath.PI;
                        } else if ((W || E) && !S) {
                            pieceNumber = 0;
                        } else if ((N || S) && !W) {
                            pieceNumber = 0;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 10;
                            yAngle = FastMath.HALF_PI;
                        } else if (E && W && !N) {
                            pieceNumber = 10;
                            yAngle = FastMath.PI;
                        }
                    } else if (i == 1 && k == 1) { // South east corner
                        if (!S && !E) {
                            pieceNumber = 1;
                            yAngle = FastMath.HALF_PI;
                        } else if (!N && !W) {
                            pieceNumber = 11;
                            yAngle = -FastMath.HALF_PI;
                        } else if ((W || E) && !S) {
                            pieceNumber = 0;
                        } else if ((N || S) && !E) {
                            pieceNumber = 0;
                            yAngle = FastMath.HALF_PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 10;
                            yAngle = -FastMath.HALF_PI;
                        } else if (S && E && !W) {
                            pieceNumber = 12;
                            //yAngle = FastMath.PI;
                        } else if (E && W && !N) {
                            pieceNumber = 10;
                            yAngle = FastMath.PI;
                        }
                    }
                    // Load the piece
                    Spatial part = loadAsset(assetManager, modelName + pieceNumber + ".j3o", false);

                    resetAndMoveSpatial((Node) part, start, p);
                    if (yAngle != 0) {
                        part.rotate(0, yAngle, 0);
                    }
                    part.move(TILE_WIDTH / 4 - i * TILE_WIDTH / 2, 0, TILE_WIDTH / 4 - k * TILE_WIDTH / 2);

                    // Set the shadows
                    // TODO: optimize, set to individual pieces and see zExtend whether it casts or not
                    part.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                    model.attachChild(part);

                }
            }

            model.batch();
            n.attachChild(model);

        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return n;
    }
}
