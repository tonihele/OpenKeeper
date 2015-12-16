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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;

/**
 *
 * @author ArchDemon
 */

public class HeroGate extends GenericRoom {

    public HeroGate(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);
    }
    
    @Override
    protected void contructFloor(Node n) {
        // Contruct the tiles        
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            int piece = 2;
            float yAngle = 0;
            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x,     p.y - 1));           
            boolean E = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean S = roomInstance.hasCoordinate(new Point(p.x,     p.y + 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            
            if (!N && E && W) {
                piece = 1;
            } else if (!S && !E && !W) {
                // FIXME
                piece = 9;
            } else if (!W) {
                piece = 3;                
            } else if (!E) {
                piece = 3;
                yAngle = - 2 * FastMath.HALF_PI;
            }

            Node tile = (Node) assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + piece + ".j3o");

            // Reset
            resetAndMoveSpatial(tile, start, p);
            if (yAngle != 0) {
                tile.rotate(0, yAngle, 0);
            }
            // Set the shadows
            //tile.setShadowMode(RenderQueue.ShadowMode.Receive);

            n.attachChild(tile);            
        }

        // Set the transform and scale to our scale and 0 the transform
        n.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2);
        n.scale(MapLoader.TILE_WIDTH); // Squares anyway...
    }
    
    @Override
    protected void contructWall(Node root) {

        // Get the wall points
        Point start = roomInstance.getCoordinates().get(0);
        for (WallSection section : roomInstance.getWallPoints()) {
            int i = 0;

            // Reset wall index for each wall section
            resetWallIndex();

            for (Point p : section.getCoordinates()) {
                int piece;
                
                if (section.getDirection() == WallSection.WallDirection.NORTH) {
                    piece = (i == 1) ? 5 : 7;
                    
                    Spatial part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + piece + ".j3o");
                    resetAndMoveSpatial(part, start, new Point(start.x + p.x, start.y + p.y));
                    part.move(-0.5f, 0, -0.5f);
                    root.attachChild(part);
                    
                    i++;
                    
                } else if (section.getDirection() == WallSection.WallDirection.WEST) {
                    piece = 7;
                    Spatial part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + piece + ".j3o");
                    resetAndMoveSpatial(part, start, new Point(start.x + p.x, start.y + p.y));
                    part.rotate(0, FastMath.HALF_PI, 0);
                    part.move(-0.5f, 0, -0.5f);
                    root.attachChild(part);
                    
                } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                    piece = 7;
                    
                    Spatial part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + piece + ".j3o");
                    resetAndMoveSpatial(part, start, new Point(start.x + p.x, start.y + p.y));
                    part.rotate(0, -FastMath.HALF_PI, 0);
                    part.move(-0.5f, 0, -0.5f);
                    root.attachChild(part);
                    
                } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                    //FIXME
                    if (i == 0) {
                        piece = 4;
                    } else if (i == 1) {
                        piece = 5;
                    } else {
                        piece = 8;
                    }
                    
                    Spatial part = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + piece + ".j3o");
                    resetAndMoveSpatial(part, start, new Point(start.x + p.x, start.y + p.y));
                    part.rotate(0, FastMath.PI, 0);
                    part.move(-0.5f, 0, -0.5f);
                    root.attachChild(part);
                    
                    i++;
                }
                
                
            }
        }
    }
}