/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.utils;

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.awt.Point;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.world.MapLoader;

/**
 * Contains transforms from tile indexes and world coordinates
 * @author archdemon
 */
public class WorldUtils {
    /**
     * Get a standard camera position vector on given map point
     *
     * @param x tile x coordinate
     * @param y tile y coordinate
     * @return position on 3D world with y = 0
     */
    public static Vector3f pointToVector3f(final int x, final int y) {
        return new Vector3f(x * MapLoader.TILE_WIDTH, 0, y * MapLoader.TILE_WIDTH);
    }
    
    /**
     * calculates position from center ActionPoint
     * @param ap
     * @return position on 3D world with y = 0
     */
    public static Vector3f ActionPointToVector3f(final ActionPoint ap) {
                    
        return new Vector3f(
                (ap.getStart().x + ap.getEnd().x) / 2.0f * MapLoader.TILE_WIDTH, 
                0, 
                (ap.getStart().y + ap.getEnd().y) / 2.0f * MapLoader.TILE_WIDTH);
    }

    /**
     * 
     * @param p
     * @return position on 3D world with y = 0
     */
    public static Vector3f pointToVector3f(final Point p) {
        return pointToVector3f(p.x, p.y);
    }

    public static Vector2f pointToVector2f(final int x, final int y) {
        return new Vector2f(x * MapLoader.TILE_WIDTH, y * MapLoader.TILE_WIDTH);
    }

    public static Vector2f pointToVector2f(final Point p) {
        return pointToVector2f(p.x, p.y);
    }

    public static Vector2 pointToVector2(final int x, final int y) {
        return new Vector2(x * MapLoader.TILE_WIDTH, y * MapLoader.TILE_WIDTH);
    }
    
    public static Vector2 pointToVector2(final Point p) {
        return pointToVector2(p.x, p.y);
    }

    public static Point vector2fToPoint(final Vector2f v) {
        return vector2fToPoint(v.x, v.y);
    }

    public static Point vector3fToPoint(final Vector3f v) {
        return vector2fToPoint(v.x, v.z);
    }
    
    public static Point vector2fToPoint(final float x, final float y) {
        return new Point(Math.round(x / MapLoader.TILE_WIDTH), Math.round(y / MapLoader.TILE_WIDTH));
    }
}
