/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.world.room;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
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
import toniarts.opendungeonkeeper.world.MapLoader;
import toniarts.opendungeonkeeper.world.room.control.FrontEndLevelControl;

/**
 * Loads up a hero gate, front end edition. Main menu. Most of the objects are
 * listed in the objects, but I don't see how they help<br>
 * TODO: Effect on the gem holder & lightning, controls for the level selection
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HeroGateFrontEnd {

    private HeroGateFrontEnd() {
    }

    public static Spatial construct(AssetManager assetManager, RoomInstance roomInstance) {
        Node n = new Node();

        // The front end hero gate

        // Contruct the tiles
        int i = 1;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Node tile = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + i + ".j3o"));

            // Reset
            resetAndMoveSpatial(tile, start, p);

            // Set the shadows
            // TODO: optimize, set to individual pieces and see zExtend whether it casts or not
            tile.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            n.attachChild(tile);

            // Add some objects according to the tile number
            if (i == 2) {
                n.attachChild(loadObject("3DFE_GemHolder", assetManager, start, p, false));

                // The light beams, I dunno, there are maybe several of these here
                Quaternion quat = new Quaternion();
                quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                n.attachChild(loadObject("3dfe_beams", assetManager, start, p, true).rotate(quat).move(0, 0.4f, 0.1f));

                // TODO: Add a point/spot light here

                // Banners
                n.attachChild(loadObject("banner1_swing", assetManager, start, p, true));
                n.attachChild(loadObject("banner2_swing", assetManager, start, p, true));

                // The "candles"
                addCandles(n, assetManager, start, p);
            } else if (i == 5) {

                // Banners
                n.attachChild(loadObject("banner3_swing", assetManager, start, p, true));
                n.attachChild(loadObject("banner4_swing", assetManager, start, p, true));

                // The "candles"
                addCandles(n, assetManager, start, p);
            } else if (i == 8) {

                // Banners
                n.attachChild(loadObject("banner1_swing", assetManager, start, p, true));
                n.attachChild(loadObject("banner2_swing", assetManager, start, p, true));

                // The "candles"
                addCandles(n, assetManager, start, p);
            } else if (i == 11) {

                // Banners
                n.attachChild(loadObject("banner1_swing", assetManager, start, p, true));
                n.attachChild(loadObject("banner2_swing", assetManager, start, p, true));

                // The "candles"
                addCandles(n, assetManager, start, p);

                // Map
                Node map = new Node("Map");
                for (int x = 1; x < 21; x++) {
                    attachAndCreateLevel(map, x, null, assetManager, start, p, false);
                    if (x == 15) {
                        attachAndCreateLevel(map, x, "a", assetManager, start, p, false);
                        map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p, false));
                        attachAndCreateLevel(map, x, "b", assetManager, start, p, false);
                        map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p, false));
                    } else if (x == 6) {
                        attachAndCreateLevel(map, x, "a", assetManager, start, p, false);
                        map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p, false));
                        map.attachChild(loadObject("3dmap_level" + x + "b", assetManager, start, p, false));
                        map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p, false));
                    } else if (x == 11) {
                        attachAndCreateLevel(map, x, "a", assetManager, start, p, false);
                        map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p, false));
                        attachAndCreateLevel(map, x, "b", assetManager, start, p, false);
                        map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p, false));
                        attachAndCreateLevel(map, x, "c", assetManager, start, p, false);
                        map.attachChild(loadObject("3dmaplevel" + x + "c" + "_arrows", assetManager, start, p, false));
                    } else {
                        map.attachChild(loadObject("3dmaplevel" + x + "_arrows", assetManager, start, p, false));
                    }
                }

                // Secret levels
                for (int x = 1; x < 6; x++) {
                    map.attachChild(loadObject("Secret_Level" + x, assetManager, start, p, false));
                }

                // The map base
                map.attachChild(loadObject("3dmap_level21", assetManager, start, p, false));

                // Add the map node
                n.attachChild(map);
            }

            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
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
        tile.move(p.x - start.x, -1.0f, p.y - start.y);
    }

    /**
     * Load an object asset
     *
     * @param model the model string
     * @param assetManager the asset manager instance
     * @param start starting point
     * @param p the current point
     * @param randomizeAnimation randomize object animation (speed and start
     * time)
     * @return reseted and ready to go model (also animated if there is a such
     * option)
     */
    private static Spatial loadObject(String model, AssetManager assetManager, Point start, Point p, boolean randomizeAnimation) {
        Node object = (Node) assetManager.loadModel(Utils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + model + ".j3o"));

        // Reset
        resetAndMoveSpatial(object, start, p);
        object.move(0, 1f, 0);

        // Animate
        AnimControl animControl = (AnimControl) object.getChild(0).getControl(AnimControl.class);
        if (animControl != null) {
            AnimChannel channel = animControl.createChannel();
            channel.setAnim("anim");
            channel.setLoopMode(LoopMode.Loop);
            if (randomizeAnimation) {
                channel.setSpeed(FastMath.nextRandomInt(6, 10) / 10f);
                channel.setTime(FastMath.nextRandomFloat() * channel.getAnimMaxTime());
            }

            // Don't batch animated objects, seems not to work
            object.setBatchHint(Spatial.BatchHint.Never);
        }

        return object;
    }

    /**
     * Adds two candles to the tile, one to each side
     *
     * @param n node to attach to
     * @param assetManager the asset manager instance
     * @param start starting point for the room
     * @param p this tile coordinate
     */
    private static void addCandles(Node n, AssetManager assetManager, Point start, Point p) {

        // The "candles"
        n.attachChild(loadObject("chain_swing", assetManager, start, p, true).move(-1f, 0, 0));
        Quaternion quat = new Quaternion();
        quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
        n.attachChild(loadObject("chain_swing", assetManager, start, p, true).rotate(quat).move(1f, 0, 1f));
    }

    /**
     * Creates and attach the level node, creates a control for it
     *
     * @param map node to attach to
     * @param level level number
     * @param variation variation, like level "a" etc.
     * @param assetManager the asset manager instance
     * @param start starting point for the room
     * @param p this tile coordinate
     * @param randomizeAnimation randomize object animation (speed and start
     * time)
     */
    private static void attachAndCreateLevel(Node map, int level, String variation, AssetManager assetManager, Point start, Point p, boolean randomizeAnimation) {
        Spatial lvl = loadObject("3dmap_level" + level + (variation == null ? "" : variation), assetManager, start, p, randomizeAnimation);
        lvl.addControl(new FrontEndLevelControl(level, variation, assetManager));
        lvl.setBatchHint(Spatial.BatchHint.Never);
        map.attachChild(lvl);
    }
}
