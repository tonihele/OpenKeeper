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
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.effect.EffectManager;
import toniarts.openkeeper.world.room.control.PlugControl;
import toniarts.openkeeper.world.room.control.RoomGoldControl;

/**
 * Constructs 5 by 5 "rotated" buildings. As far as I know, only Dungeon Heart
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class FiveByFiveRotated extends GenericRoom {

    private int centreDecay = -1;
    private boolean destroyed = false;
    private boolean created = false;

    public FiveByFiveRotated(AssetManager assetManager, EffectManager effectManager,
            RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, effectManager, roomInstance, direction);
        addObjectControl(new RoomGoldControl(this) {

            @Override
            protected int getObjectsPerTile() {
                return FiveByFiveRotated.this.getGoldPerTile();
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return 16;
            }
        });
    }

    protected abstract int getGoldPerTile();

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // 5 by 5
        // DHeart Piece[1-20]Exp.j3o
        Point start = roomInstance.getCoordinates().get(0);
        String resource = (destroyed) ? "Dungeon_Destroyed" : roomInstance.getRoom().getCompleteResource().getName();
        for (Point p : roomInstance.getCoordinates()) {

            // There are just 4 different pieces
            int x = p.x - start.x;
            int y = p.y - start.y;
            Spatial tile;
            float yAngle = 0;
            int piece = -1;
            // Corners
            if (x == 0 && y == 0) { // Top left corner
                piece = 3;
                yAngle = FastMath.HALF_PI;
            } else if (x == 4 && y == 0) { // Top right corner
                piece = 3;
            } else if (x == 0 && y == 4) { // Lower left corner
                piece = 3;
                yAngle = -FastMath.PI;
            } else if (x == 4 && y == 4) { // Lower right corner
                piece = 3;
                yAngle = -FastMath.HALF_PI;
            } // Outer layer sides
            else if (x == 0) { // Left side
                piece = 2;
                yAngle = -FastMath.PI;
            } else if (x == 4) { // Right side
                piece = 2;
            } else if (y == 0) { // Top side
                piece = 2;
                yAngle = FastMath.HALF_PI;
            } else if (y == 4) { // Bottom side
                piece = 2;
                yAngle = -FastMath.HALF_PI;
            } // The inner ring, corners
            else if (x == 1 && y == 1) { // Top left
                piece = 0;
            } else if (x == 3 && y == 1) { // Top right
                piece = 0;
                yAngle = -FastMath.HALF_PI;
            } else if (x == 1 && y == 3) { // Bottom left
                piece = 0;
                yAngle = FastMath.HALF_PI;
            } else if (x == 3 && y == 3) { // Bottom right
                piece = 0;
                yAngle = -FastMath.PI;
            } // The inner ring, sides
            else if (x == 1) { // Left
                piece = 1;
                yAngle = FastMath.HALF_PI;
            } else if (x == 3) { // Right
                piece = 1;
                yAngle = -FastMath.HALF_PI;
            } else if (y == 1) { // Top
                piece = 1;
            } else if (y == 3) { // Bottom
                piece = 1;
                yAngle = -FastMath.PI;
            }

            if (piece != -1) {
                tile = loadModel(resource + piece);
                resetAndMoveSpatial(tile, start, p);
                if (yAngle != 0) {
                    tile.rotate(0, yAngle, 0);
                }
                root.attachChild(tile);
            }

            // Only observed 5 by 5 is the Dungeon Heart, its object list is empty, so I just hard code these here
            // The center pieces
            if (x == 2 && y == 2) {

                if (destroyed) {
                    tile = loadModel(resource + 4);
                    resetAndMoveSpatial(tile, start, p);
                    root.attachChild(tile);

                    // The steps between the arches
                    tile = loadModel(resource + 5);
                    resetAndMoveSpatial(tile, start, p);
                    root.attachChild(tile.move(0, -MapLoader.TILE_HEIGHT * 0.42f, 0));

                } else {
                    // The arches
                    tile = loadModel("DHeart Arches");
                    resetAndMoveSpatial(tile, start, p);
                    root.attachChild(tile);

                    // The steps between the arches
                    tile = loadModel("DHeart BigSteps");
                    resetAndMoveSpatial(tile, start, p);
                    root.attachChild(tile);

                    tile = loadModel("DHeart BigSteps");
                    resetAndMoveSpatial(tile, start, p);
                    tile.rotate(0, -FastMath.TWO_PI / 3, 0);
                    root.attachChild(tile);

                    tile = loadModel("DHeart BigSteps");
                    resetAndMoveSpatial(tile, start, p);
                    tile.rotate(0, FastMath.TWO_PI / 3, 0);
                    root.attachChild(tile);

                    // The alfa & omega! The heart, TODO: use object loader once it is in decent condition, this after all is a real object
                    if (centreDecay == -1) {
                        tile = loadModel("Dungeon centre");
                    } else {
                        tile = loadModel("DungeonCentre_DECAY" + centreDecay);
                    }
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

                    for (Integer id : roomInstance.getRoom().getEffects()) {
                        Node effect = effectManager.load(id);
                        resetAndMoveSpatial(effect, start, p);
                        root.attachChild(effect);
                    }

                    root.attachChild(tile.move(0, MapLoader.TILE_HEIGHT / 4, 0));

                    if (!created) {
                        created = true;

                        tile = loadModel("DHeartPlug");
                        tile.setName("plug");
                        tile.setBatchHint(Spatial.BatchHint.Never);
                        tile.rotate(0, FastMath.QUARTER_PI, 0);
                        resetAndMoveSpatial(tile, start, p);
                        root.attachChild(tile.move(0, MapLoader.TILE_HEIGHT, 0));

                        Node plug = getPlug();
                        plug.setName("plug_decay");
                        plug.setCullHint(Spatial.CullHint.Always);
                        plug.setBatchHint(Spatial.BatchHint.Never);
                        root.attachChild(plug.move(x, MapLoader.TILE_HEIGHT, y));

                        root.addControl(new PlugControl());
                    }
                }
            }
        }

        // Set the transform and scale to our scale and 0 the transform
        root.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2);
        root.scale(MapLoader.TILE_WIDTH, MapLoader.TILE_HEIGHT, MapLoader.TILE_WIDTH);

        return root;
    }

    private Node getPlug() {
        Node plug = new Node();
        float step, rp, r = 0.69f;

        for (int i = 1; i < 10; i++) {

            Spatial piece = loadModel("DHeartPlug" + i);
            /*
             * with no reset spatial
             if (i == 1) {
             rp = 0.04f;
             step = 0.40f;
             //piece.move(0.65f, 0, 0.32f);
             } else if (i == 2) {
             step = 0.73f;
             //piece.move(0.5f, 0, 0.5f);
             } else if (i == 3) {
             rp = 0.17f;
             step = 2.05f;
             //piece.move(-0.38f, 0, 0.78f);
             } else if (i == 4) {
             rp = -0.017f;
             step = 2.525f;
             //piece.move(-0.52f, 0, 0.44f);
             } else if (i == 5) {
             rp = 0.082f;
             step = 3.465f;
             //piece.move(-0.7f, 0, -0.2f);
             } else if (i == 6) {
             rp = 0.15f;
             step = 3.81f;
             //piece.move(-0.65f, 0, -0.48f);
             } else if (i == 7) {
             rp = 0.19f;
             step = 5.08f;
             //piece.move(0.34f, 0, -0.76f);
             } else if (i == 8) {
             rp = 0.1f;
             step = 5.54f;
             //piece.move(0.63f, 0, -0.48f);
             } else if (i == 9) {
             rp = 0.065f;
             step = 6.44f;
             //piece.move(0.7f, 0, 0.2f);
             } else {
             step = 0;
             }
             */

            resetAndMoveSpatial(piece, new Point(), new Point());

            if (i == 1) {
                rp = 0.031f;
                step = 0.40f;
            } else if (i == 2) {
                rp = 0.054f;
                step = 0.81f;
            } else if (i == 3) {
                rp = 0.08f;
                step = 2.00f;
            } else if (i == 4) {
                rp = 0.06f;
                step = 2.512f;
            } else if (i == 5) {
                rp = 0.036f;
                step = 3.47f;
            } else if (i == 6) {
                rp = 0.17f;
                step = 3.92f;
            } else if (i == 7) {
                rp = 0.117f;
                step = 5.07f;
            } else if (i == 8) {
                rp = 0.126f;
                step = 5.59f;
            } else {
                rp = 0.026f;
                step = 6.345f;
            }

            piece.move((r + rp) * FastMath.cos(step), -0.1f, (r + rp) * FastMath.sin(step));
            piece.setUserData("yAngle", step);
            plug.attachChild(piece);
        }
        return plug;
    }

    @Override
    public boolean isTileAccessible(int x, int y) {

        // The center 3x3 is not accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(x, y);
        return ((roomPoint.x == 0 || roomPoint.x == 4) || (roomPoint.y == 0 || roomPoint.y == 4));
    }

    @Override
    protected BatchNode constructWall() {
        return null;
    }
}
