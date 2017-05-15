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

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.RoomUtils;
import toniarts.openkeeper.world.EntityInstance;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.terrain.Water;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;

/**
 *
 * @author ArchDemon
 */
public class Temple extends DoubleQuad {

    public enum PieceLocation {NORTH_WEST, NORTH_EAST, SOUTH_WEST, SOUTH_EAST }
    private static final short OBJECT_TEMPLE_HAND_ID = 66;
    private static final short OBJECT_TEMPLE_CANDLESTICK_ID = 111;
    private static final Logger logger = Logger.getLogger(Temple.class.getName());

    public Temple(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        Point start = roomInstance.getMatrixStartPoint();

        // Water
        boolean[][] waterArea = RoomUtils.calculateWaterArea(roomInstance.getCoordinatesAsMatrix());
        boolean hasWater = hasWater(waterArea);
        boolean[][] borderArea;
        if(hasWater) {


            borderArea = RoomUtils.calculateBorderArea(roomInstance.getCoordinatesAsMatrix(), waterArea);

            // Hand
            boolean drawHand = RoomUtils.matrixContainsSquare(roomInstance.getCoordinatesAsMatrix(), 5);
            if (drawHand) {

                final Point topLeft = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
                final Point bottomRight = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

                for (int i = 0; i < waterArea.length; ++i) {
                    for (int j = 0; j < waterArea[0].length; ++j) {
                        if (waterArea[i][j]) {
                            topLeft.x = Math.min(i, topLeft.x);
                            topLeft.y = Math.min(j, topLeft.y);
                            bottomRight.x = Math.max(i, bottomRight.x);
                            bottomRight.y = Math.max(j, bottomRight.y);
                        }
                    }
                }

                Point centre = new Point(start.x + (topLeft.x + bottomRight.x) / 2, start.y + (topLeft.y + bottomRight.y) / 2);
                Spatial part = objectLoader.load(assetManager, centre.x, centre.y, OBJECT_TEMPLE_HAND_ID, roomInstance.getOwnerId());
                part.move(0, -3 * MapLoader.FLOOR_HEIGHT / 2, TILE_WIDTH / 4);
                root.attachChild(part);
            }
        } else {
            borderArea = new boolean[roomInstance.getCoordinatesAsMatrix().length][roomInstance.getCoordinatesAsMatrix()[0].length];
        }

        final List<EntityInstance<Terrain>> instances = new ArrayList<>(1);
        final EntityInstance<Terrain> ent = new EntityInstance<>(getWorldState().getGameState().getLevelData().getMap().getWater());
        instances.add(ent);
        for (Point p : roomInstance.getCoordinates()) {
            // Figure out which piece by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean NE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean SE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean SW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            boolean NW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));

            Point localPoint = roomInstance.worldCoordinateToLocalCoordinate(p.x, p.y);
            Node model = Temple.constructQuad(assetManager, modelName, N, NE, E, SE, S, SW, W, NW, borderArea, localPoint, hasWater);
            moveSpatial(model, p);
            root.attachChild(model);


            if(waterArea[localPoint.x][localPoint.y] || NE || SE || SW || NW) {
                ent.addCoordinate(p);
            }
        }

        if(!instances.isEmpty()) {
            Spatial waterTiles = Water.construct(assetManager, instances);
            root.attachChild(waterTiles);
        }


        constructCandles(root);

        return root;
    }

    private boolean hasWater(boolean[][] waterArea) {
        for(int i = 0; i < waterArea.length; ++i) {
            for(int j = 0; j < waterArea[0].length; ++j) {
                if(waterArea[i][j]) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void constructCandles(Node node) {

        // We have very different logic than the normal
        // Go through all the points and see if they are fit for pillar placement
        for (Point p : roomInstance.getCoordinates()) {

            // See that we have 2 "free" neigbouring tiles
            EnumSet<WallSection.WallDirection> freeDirections = EnumSet.noneOf(WallSection.WallDirection.class);
            if (!hasSameTile(map, p.x - start.x, p.y - start.y - 1)) { // North
                freeDirections.add(WallSection.WallDirection.NORTH);
            }
            if (!hasSameTile(map, p.x - start.x, p.y - start.y + 1)) { // South
                freeDirections.add(WallSection.WallDirection.SOUTH);
            }
            if (!hasSameTile(map, p.x - start.x + 1, p.y - start.y)) { // East
                freeDirections.add(WallSection.WallDirection.EAST);
            }
            if (!hasSameTile(map, p.x - start.x - 1, p.y - start.y)) { // West
                freeDirections.add(WallSection.WallDirection.WEST);
            }

//             We may have up to 4 pillars in the same tile even, every corner gets one, no need to check anything else
//             Add a pillar
//             Face "in" diagonally
            float offset = MapLoader.TILE_WIDTH / 4;
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                float yAngle = -FastMath.HALF_PI;
                Spatial candle = constructCandle(node, p, yAngle);//.move(0, MapLoader.TILE_HEIGHT, 0);
                candle.move(offset, 0, -offset);
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.EAST)) {
                float yAngle = FastMath.PI;
                Spatial candle = constructCandle(node, p, yAngle);//.move(-0.15f, MapLoader.TILE_HEIGHT, -0.15f);
                candle.move(offset, 0, offset);
            }
            if (freeDirections.contains(WallSection.WallDirection.SOUTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                float yAngle = FastMath.HALF_PI;
                Spatial candle = constructCandle(node, p, yAngle);//.move(-0.85f, MapLoader.TILE_HEIGHT, -0.15f);
                candle.move(-offset, 0, offset);
            }
            if (freeDirections.contains(WallSection.WallDirection.NORTH) && freeDirections.contains(WallSection.WallDirection.WEST)) {
                Spatial candle = constructCandle(node, p, 0);//.move(-0.85f, MapLoader.TILE_HEIGHT, -0.85f);
                candle.move(-offset, 0, -offset);
            }


        }
    }

    private Spatial constructCandle(Node node, Point p, float yAngle) {
        Spatial part = objectLoader.load(assetManager, p.x, p.y, OBJECT_TEMPLE_CANDLESTICK_ID, roomInstance.getOwnerId());

        if (yAngle != 0) {
            part.rotate(0, yAngle, 0);
        }

        node.attachChild(part);
        return part;
    }

    public static Node constructQuad(AssetManager assetManager, String modelName,
                                     boolean N, boolean NE, boolean E, boolean SE, boolean S, boolean SW, boolean W, boolean NW, boolean[][] borderArea, Point localPoint, boolean hasWater) {


        /*
          1 : Quad-piece corner piece with 2 borders |_ rotate it to get the right orientation
          0 : Quad-piece where one side has a border | rotate it to get the right orientation
          2 : Quad-piece where no side has a border, useful to fill up inner corners where no borders are
         */

        Node quad = new Node();

        for (int i = 0; i < 4; ++i) {
            // 4 - 8 - walls
            int piece = 0;
            float yAngle = 0;
            Vector3f movement;
            // Determine the piece
            PieceLocation pieceLocation = PieceLocation.values()[i];
            switch (pieceLocation) {
                case NORTH_WEST:  // North west corner
                    if (N && NE && NW && E && SE && S && SW && W) {
                        piece = 13;
                    } else if (N && E && S && W && NW && !SE) {
                        piece =  !hasWater ? 1 : isBorderTile(localPoint, borderArea) ? 12 : 1;
                        yAngle = FastMath.HALF_PI;
                    } else if (N && E && SE && S && W && !NW) {
                        piece = 2;
                        yAngle = FastMath.HALF_PI;
                    } else if (!N && !W) {
                        piece = 1;
                        yAngle = FastMath.HALF_PI;
                    } else if (!S && !E && NW) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 11 : 2;
                        yAngle = -FastMath.HALF_PI;
                    } else if (N && !W) {
                        piece = 0;
                        yAngle = FastMath.HALF_PI;
                    } else if (N && W && (!S || !SW)) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 10 : 2;
                        yAngle = FastMath.PI;
                    } else if (N && W && (!E || !NE)) {
                        piece = !hasWater ? 1 : isBorderTile(localPoint, borderArea) ? 10 : 1;
                        yAngle = -FastMath.HALF_PI;
                    }
                    movement = new Vector3f(-MapLoader.TILE_WIDTH / 4, 0, -MapLoader.TILE_WIDTH / 4);
                    break;
                case NORTH_EAST:  // North east corner
                    if (N && NE && NW && E && SE && S && SW && W) {
                        piece = 13;
                    } else if (N && NE && E && S && W && !SW) {
                        piece = !hasWater ? 1 : isBorderTile(localPoint, borderArea) ? 12 : 1;
                    } else if (N && E && S && SW && W && !NE) {
                        piece = 2;
                    } else if (!N && !E) {
                        piece = 1;
                    } else if (!S && !W && NE) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 11 : 2;
                        yAngle = FastMath.PI;
                    } else if (N && !E) {
                        piece = 0;
                        yAngle = -FastMath.HALF_PI;
                    } else if (N && E && (!S || !SE)) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 10 : 2;
                        yAngle = FastMath.PI;
                    } else if (N && E && (!W || !NW)) {
                        piece = !hasWater ? 1 : isBorderTile(localPoint, borderArea) ? 10 : 1;
                        yAngle = FastMath.HALF_PI;
                    }
                    movement = new Vector3f(MapLoader.TILE_WIDTH / 4, 0, -MapLoader.TILE_WIDTH / 4);
                    break;
                case SOUTH_WEST:  // South west corner
                    if (N && NE && NW && E && SE && S && SW && W) {
                        piece = 13;
                    } else if (N && E && S && SW && W && !NE) {
                        piece = !hasWater ? 1 : isBorderTile(localPoint, borderArea) ? 12 : 1;
                        yAngle = FastMath.PI;
                    } else if (N && NE && E && S && W && !SW) {
                        piece = 2;
                        yAngle = FastMath.PI;
                    } else if (!S && !W) {
                        piece = 1;
                        yAngle = FastMath.PI;
                    } else if (!N && !E && SW) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 11 : 2;
                        yAngle = 0;
                    } else if (!N && !W && S) {
                        piece = 0;
                        yAngle = FastMath.HALF_PI;
                    } else if (W && !S) {
                        piece = 0;
                        yAngle = FastMath.PI;
                    } else if (S && W && (!E || !SE)) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 10 : 2;
                        yAngle = -FastMath.HALF_PI;
                    } else if (S && W && (!N || !NW)) {
                        piece = !hasWater ? 1 : isBorderTile(localPoint, borderArea) ? 10 : 1;
                    }
                    movement = new Vector3f(-MapLoader.TILE_WIDTH / 4, 0, MapLoader.TILE_WIDTH / 4);
                    break;
                case SOUTH_EAST: // South east corner
                    if (N && NE && NW && E && SE && S && SW && W) {
                        piece = 13;
                    } else if (N && E && SE && S && W && !NW) {
                        piece = !hasWater ? 1 : isBorderTile(localPoint, borderArea) ? 12 : 1;
                        yAngle = -FastMath.HALF_PI;
                    } else if (N && E && S && W && NW && !SE) {
                        piece = 2;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!S && !E) {
                        piece = 1;
                        yAngle = -FastMath.HALF_PI;
                    } else if (!N && !W && SE) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 11 : 2;
                        yAngle = FastMath.HALF_PI;
                    } else if (!N && !E && S) {
                        piece = 0;
                        yAngle = -FastMath.HALF_PI;
                    } else if (E && !S) {
                        piece = 0;
                        yAngle = FastMath.PI;
                    } else if (E && S && (!W || !SW)) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 10 : 2; //done
                        yAngle = FastMath.HALF_PI;
                    } else if (E && S && (!N || !NE)) {
                        piece = !hasWater ? 2 : isBorderTile(localPoint, borderArea) ? 10 : 2;
                    }
                    movement = new Vector3f(MapLoader.TILE_WIDTH / 4, 0, MapLoader.TILE_WIDTH / 4);
                    break;
                default:
                    movement = new Vector3f(Vector3f.ZERO);
                }

            logger.info("Selected piece: " + piece);
            // Load the piece
            Spatial part = AssetUtils.loadModel(assetManager, modelName + piece);
            part.rotate(0, yAngle, 0);
            part.move(movement);


            quad.attachChild(part);

        }

        return quad;
    }

    private static boolean isBorderTile(Point localPoint, boolean[][] borderArea) {
        return borderArea[localPoint.x][localPoint.y];
    }

}
