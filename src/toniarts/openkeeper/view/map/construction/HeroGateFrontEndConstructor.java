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
package toniarts.openkeeper.view.map.construction;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.data.Level;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.FullMoon;
import toniarts.openkeeper.view.map.WallSection;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.room.control.FrontEndLevelControl;

/**
 * Loads up a hero gate, front end edition. Main menu. Most of the objects are
 * listed in the objects, but I don't see how they help<br>
 * TODO: Effect on the gem holder & lightning, controls for the level selection
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HeroGateFrontEndConstructor extends RoomConstructor {

    public HeroGateFrontEndConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        super(assetManager, roomInstance);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // The front end hero gate

        // Contruct the tiles
        int i = 1;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            ArtResource artResource = roomInstance.getRoom().getCompleteResource();
            Spatial tile = RoomConstructor.loadModel(assetManager, artResource.getName() + i, artResource, false, true);

            // Reset
            moveSpatial(tile, start, p);

            switch (i) {

                case 2:

                    // The light beams, I dunno, there are maybe several of these here
                    // FIXME beams is a visual effect of gem holder ???
                    float yAngle = -FastMath.PI;
                    Spatial obj = loadObject("3dfe_beams", assetManager, start, p);
                    obj.rotate(0, yAngle, 0).move(0, 0.4f, 0.1f);
                    root.attachChild(obj);
                    animate(obj, false);

                    break;
                case 11:

                    // Map
                    Node map = new Node("Map");
                    for (int x = 1; x < 21; x++) {
                        switch (x) {
                            case 6:
                                attachAndCreateLevel(map, Level.LevelType.Level, x, "a", assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p));
                                attachAndCreateLevel(map, Level.LevelType.Level, x, "b", assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p));
                                break;
                            case 11:
                                attachAndCreateLevel(map, Level.LevelType.Level, x, "a", assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p));
                                attachAndCreateLevel(map, Level.LevelType.Level, x, "b", assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p));
                                attachAndCreateLevel(map, Level.LevelType.Level, x, "c", assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "c" + "_arrows", assetManager, start, p));
                                break;
                            case 15:
                                attachAndCreateLevel(map, Level.LevelType.Level, x, "a", assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "a" + "_arrows", assetManager, start, p));
                                attachAndCreateLevel(map, Level.LevelType.Level, x, "b", assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "b" + "_arrows", assetManager, start, p));
                                break;
                            default:
                                attachAndCreateLevel(map, Level.LevelType.Level, x, null, assetManager, start, p);
                                map.attachChild(loadObject("3dmaplevel" + x + "_arrows", assetManager, start, p));
                        }
                    }

                    // Secret levels
                    for (int x = 1; x < 6; x++) {
                        if (x == 5 && !FullMoon.isFullMoon()) {
                            // don't show full moon level
                            continue;
                        }
                        attachAndCreateLevel(map, Level.LevelType.Secret, x, null, assetManager, start, p);
                    }

                    // The map base
                    map.attachChild(loadObject("3dmap_level21", assetManager, start, p));

                    // Add the map node
                    root.attachChild(map);

                    break;
            }

            root.attachChild(tile);
            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.translateToTile(root, start);

        return root;
    }

    @Override
    public Spatial getWallSpatial(Point start, WallSection.WallDirection direction) {
        return null;
    }

    /**
     * Creates and attach the level node, creates a control for it
     *
     * @param map node to attach to
     * @param type level type
     * @param level level number
     * @param variation variation, like level "a" etc.
     * @param assetManager the asset manager instance
     * @param start starting point for the room
     * @param p this tile coordinate
     */
    private void attachAndCreateLevel(Node map, Level.LevelType type, int levelnumber, String variation,
            AssetManager assetManager, Point start, Point p) {

        String objName = "3dmap_level";
        if (type.equals(Level.LevelType.Secret)) {
            objName = "Secret_Level";
        }

        Spatial lvl = loadObject(objName + levelnumber + (variation == null ? "" : variation),
                assetManager, start, p);
        Node node = new Node();
        node.attachChild(lvl);
        node.addControl(new FrontEndLevelControl(new Level(type, levelnumber, variation), assetManager));
        node.setBatchHint(Spatial.BatchHint.Never);
        map.attachChild(node);
    }

    /**
     * Load an object asset
     *
     * @param model the model string
     * @param assetManager the asset manager instance
     * @param start starting point
     * @param p the current point
     * @return reset and ready to go model (also animated if there is a such
     * option)
     */
    private Spatial loadObject(String model, AssetManager assetManager, Point start, Point p) {
        Node object = (Node) RoomConstructor.loadModel(assetManager, model, null, false, true);

        // Reset
        moveSpatial(object, start, p);
        object.move(0, MapLoader.FLOOR_HEIGHT, 0);

        return object;
    }

    private void animate(Spatial object, final boolean randomizeAnimation) {

        // Animate
        object.breadthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                AnimControl animControl = spatial.getControl(AnimControl.class);
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
            }
        });
    }

}
