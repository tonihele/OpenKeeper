/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.navigation.steering;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Some common methods used by steering
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SteeringUtils {

    private SteeringUtils() {
        // Nope...
    }

    public static float calculateVectorToAngle(Vector2 vector) {
        return (float) Math.atan2(-vector.x, vector.y);
    }

    public static Vector2 calculateAngleToVector(Vector2 outVector, float angle) {
        outVector.x = -(float) Math.sin(angle);
        outVector.y = (float) Math.cos(angle);
        return outVector;
    }

    public static Array<Vector2> pathToArray(List<Vector2> inPath) {
        Array<Vector2> path = new Array<>(inPath.size());
        for (Vector2 vector : inPath) {
            path.add(vector);
        }
        return path;
    }

    public static List<Vector2> pathToList(GraphPath<MapTile> inPath) {
        List<Vector2> path = new ArrayList<>(inPath.getCount());
        for (MapTile tile : inPath) {
            path.add(WorldUtils.pointToVector2(tile.getLocation()));
        }
        return path;
    }

}
