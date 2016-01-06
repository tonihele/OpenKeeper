/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.world.creature.steering;

import com.badlogic.gdx.ai.utils.Collision;
import com.badlogic.gdx.ai.utils.Ray;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import toniarts.openkeeper.world.WorldHandler;

/**
 * Collision detector for movement
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureRayCastCollisionDetector implements RaycastCollisionDetector<Vector2> {

    private final WorldHandler world;

    public CreatureRayCastCollisionDetector(WorldHandler world) {
        this.world = world;
    }

    @Override
    public boolean collides(Ray<Vector2> ray) {
        return findCollision(null, ray);
    }

    @Override
    public boolean findCollision(Collision<Vector2> outputCollision, Ray<Vector2> inputRay) {
        if (!inputRay.start.epsilonEquals(inputRay.end, MathUtils.FLOAT_ROUNDING_ERROR)) {
            Vector3f start = new Vector3f(inputRay.start.x, 0.25f, inputRay.start.y);
            com.jme3.math.Ray ray = new com.jme3.math.Ray(start, start.subtract(inputRay.end.x, 0.25f, inputRay.end.y).normalizeLocal());
            ray.setLimit(0.05f);
            CollisionResults results = new CollisionResults();
            ((Node) world.getWorld().getChild("Map")).getChild("Terrain").collideWith(ray, results);
            if (results.size() > 0) {
                CollisionResult collission = results.getClosestCollision();
                if (collission.getDistance() > ray.getLimit()) {
                    return false;
                }
                if (outputCollision != null) {
                    outputCollision.normal = new Vector2(collission.getContactNormal().x, collission.getContactNormal().z);
                    outputCollision.point = new Vector2(collission.getContactPoint().x, collission.getContactPoint().z);
                }
            }
            return (results.size() != 0);
        }
        return false;
    }

}
