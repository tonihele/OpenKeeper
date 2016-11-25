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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.steer.Proximity;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.CollisionAvoidance;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.proximities.ProximityBase;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * A steering factory, static methods for constructing steerings for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureSteeringCreator {

    private CreatureSteeringCreator() {
    }

    public static SteeringBehavior<Vector2> navigateToPoint(final WorldState worldState, final CreatureControl creature, final Point p) {
        GraphPath<TileData> outPath = worldState.findPath(WorldState.getTileCoordinates(creature.getSpatial().getWorldTranslation()), p, creature);

        if (outPath != null && outPath.getCount() > 1) {

            // Debug
            // worldHandler.drawPath(new LinePath<>(pathToArray(outPath)));
            // Navigate
            PrioritySteering<Vector2> prioritySteering = new PrioritySteering(creature, 0.0001f);
            FollowPath<Vector2, LinePath.LinePathParam> followPath = new FollowPath(creature, new LinePath<>(pathToArray(outPath), true), 1);
            followPath.setDecelerationRadius(1f);
            followPath.setArrivalTolerance(0.2f);
            prioritySteering.add(followPath);

            // Add regular avoidance
            CollisionAvoidance<Vector2> ca = new CollisionAvoidance<>(creature, new ProximityBase<Vector2>(creature, null) {

                @Override
                public int findNeighbors(Proximity.ProximityCallback<Vector2> callback) {
                    List<CreatureControl> creatures = new ArrayList<>(creature.getVisibleCreatures());
                    int neighborCount = 0;
                    int agentCount = creatures.size();
                    for (int i = 0; i < agentCount; i++) {
                        Steerable<Vector2> currentAgent = creatures.get(i);
                        if (callback.reportNeighbor(currentAgent)) {
                            neighborCount++;
                        }
                    }

                    return neighborCount;
                }
            });
            prioritySteering.add(ca);
            return prioritySteering;
        }

        return null;
    }

    private static Array<Vector2> pathToArray(GraphPath<TileData> outPath) {
        Array<Vector2> path = new Array<>(outPath.getCount());
        for (TileData tile : outPath) {
            path.add(new Vector2(tile.getX() - 0.5f, tile.getY() - 0.5f));
        }
        return path;
    }

}
