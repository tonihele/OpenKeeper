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
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.MapLoader;
import static toniarts.openkeeper.world.MapLoader.TILE_WIDTH;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;
import static toniarts.openkeeper.world.room.GenericRoom.hasSameTile;
import toniarts.openkeeper.world.room.control.RoomPrisonerControl;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
public class Prison extends DoubleQuad {

    private Point door;

    public Prison(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader, WorldState worldState, EffectManagerState effectManager) {
        super(assetManager, roomInstance, objectLoader, worldState, effectManager);

        addObjectControl(new RoomPrisonerControl(this) {
            @Override
            protected Collection<Point> getCoordinates() {

                // Only the innards of prison can hold objects
                return Prison.this.getInsideCoordinates();
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }

        });
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
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
                door = p;

                Spatial part = AssetUtils.loadAsset(assetManager, modelName + "14");

                moveSpatial(part, start, p);
                hasDoor = true;
                part.move(-TILE_WIDTH / 4, 0, -TILE_WIDTH / 4);

                root.attachChild(part);

                continue;
            }

            Node model = new Node();

            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {
                    // 9 - stone floor
                    // 8 stone wall with skeleton
                    // 7 stone wall
                    // 6 stone wall half
                    // 5 stone wall half
                    // 4 stone wall half
                    // 3 -  dirt
                    // 2 -  dirt corner
                    // 19 - dirt with zabor
                    // 17 - dirt
                    // 15 dirt with zabor
                    // 14 - dirt with gate
                    // 13 - dirt
                    // 12 - dirt corner with zabor
                    // 11 dirt corner with palka
                    // 10 - dirt with zabor
                    // 1 - dirt corner with brics ?
                    // 0 dirt with brics
                    // Prison_Pillar
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
                            pieceNumber = 17;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !W) {
                            pieceNumber = 17;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 19;
                            yAngle = FastMath.PI;
                        } else if (E && W && !S) {
                            pieceNumber = 10;
                            //yAngle = FastMath.PI;
                        }
                    } else if (i == 1 && k == 0) { // North east corner
                        if (!N && !E) {
                            pieceNumber = 1;
                            //yAngle = -FastMath.HALF_PI;
                        } else if (!S && !W) {
                            pieceNumber = 11;
                        } else if ((W || E) && !N) {
                            pieceNumber = 17;
                            yAngle = FastMath.PI;
                        } else if ((N || S) && !E) {
                            pieceNumber = 17;
                            yAngle = FastMath.HALF_PI;
                        } else if (N && S && !W) {
                            pieceNumber = 19;
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
                            pieceNumber = 17;
                        } else if ((N || S) && !W) {
                            pieceNumber = 17;
                            yAngle = -FastMath.HALF_PI;
                        } else if (N && S && !E) {
                            pieceNumber = 19;
                            yAngle = FastMath.PI;
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
                            pieceNumber = 17;
                        } else if ((N || S) && !E) {
                            pieceNumber = 17;
                            yAngle = FastMath.HALF_PI;
                        } else if (S && E && !W) {
                            pieceNumber = 19;
                            //yAngle = FastMath.PI;
                        } else if (E && W && !N) {
                            pieceNumber = 10;
                            yAngle = FastMath.PI;
                        }
                    }
                    // Load the piece
                    Spatial part = AssetUtils.loadAsset(assetManager, modelName + pieceNumber);
                    
                    moveSpatial(part, start, p);
                    if (yAngle != 0) {
                        part.rotate(0, yAngle, 0);
                    }
                    part.move(TILE_WIDTH / 4 - i * TILE_WIDTH / 2, 0, TILE_WIDTH / 4 - k * TILE_WIDTH / 2);

                    model.attachChild(part);

                }
            }

            root.attachChild(model);
        }

        // Set the transform and scale to our scale and 0 the transform
        AssetUtils.scale(root);
        AssetUtils.moveToTile(root, start);

        return root;
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // Prison is all accessible, but you do have to use the door!
        if (door != null && fromX != null && fromY != null) {

            // Path finding
            Set<Point> insides = new HashSet<>(getInsideCoordinates());
            Point from = new Point(fromX, fromY);
            Point to = new Point(toX, toY);
            if (insides.contains(to) && insides.contains(from)) {
                return true; // Inside the prison == free movement
            } else if (insides.contains(from) && to.equals(door)) {
                return true; // From inside also through the door
            } else if (insides.contains(to) && from.equals(door)) {
                return true; // Path finding, from outside
            } else if (!insides.contains(to) && !insides.contains(from)) {
                return true; // Outside the prison == free movement
            }
            return false;
        }
        if (fromX == null && fromY == null && door != null) {
            return true; // We have a door, the tile is accessible
        }
        return super.isTileAccessible(fromX, fromY, toX, toY);
    }

    private Collection<Point> getInsideCoordinates() {
        boolean[][] matrix = roomInstance.getCoordinatesAsMatrix();
        List<Point> coordinates = new ArrayList<>();
        for (int x = 1; x < matrix.length - 1; x++) {
            for (int y = 1; y < matrix[x].length - 1; y++) {
                boolean N = hasSameTile(map, x, y + 1);
                boolean NE = hasSameTile(map, x - 1, y + 1);
                boolean E = hasSameTile(map, x - 1, y);
                boolean SE = hasSameTile(map, x - 1, y - 1);
                boolean S = hasSameTile(map, x, y - 1);
                boolean SW = hasSameTile(map, x + 1, y - 1);
                boolean W = hasSameTile(map, x + 1, y);
                boolean NW = hasSameTile(map, x + 1, y + 1);
                if (N && NE && E && SE && S && SW && W && NW) {
                    coordinates.add(new Point(roomInstance.getMatrixStartPoint().x + x, roomInstance.getMatrixStartPoint().y + y));
                }
            }
        }
        return coordinates;
    }

}
