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
