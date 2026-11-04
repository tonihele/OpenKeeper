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
package toniarts.openkeeper.game.navigation.steering;

import com.badlogic.gdx.ai.steer.behaviors.ReachOrientation;
import com.badlogic.gdx.math.Vector2;
import toniarts.openkeeper.utils.Point;
import java.util.List;
import java.util.function.ToDoubleFunction;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A steering factory, static methods for constructing steerings for entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class EntitySteeringFactory {

    private EntitySteeringFactory() {
    }

    /**
     * Steers an entity along its waypoints and turns it to face a target at
     * the end
     *
     * @param path the waypoints, the first one is where the walk started
     * @param faceTarget the tile to face at the end, if any
     * @param steerable the entity
     * @param p the tile the walk ends on
     * @param crowd the sum of the radii of the creatures round a waypoint
     * @param worker whether the entity walks like a worker
     * @param chase whether the entity chases an attack target
     * @param ticksPerSecond the game ticks a second
     * @return the steering, null if there is nothing to do
     */
    public static EntitySteeringBehavior navigateToPoint(List<Vector2> path, final Point faceTarget, final ISteerableEntity steerable, final Point p,
            ToDoubleFunction<Vector2> crowd, boolean worker, boolean chase, int ticksPerSecond) {
        if (path.size() > 1 || faceTarget != null) {

            EntitySteeringBehavior prioritySteering = new EntitySteeringBehavior(steerable);

            if (path.size() > 1) {
                // Add regular avoidance
//                CollisionAvoidance<Vector2> ca = new CollisionAvoidance<>(creature, new ProximityBase<Vector2>(creature, null) {
//
//                    @Override
//                    public int findNeighbors(Proximity.ProximityCallback<Vector2> callback) {
//                        List<CreatureControl> creatures = new ArrayList<>(creature.getVisibleCreatures());
//                        int neighborCount = 0;
//                        int agentCount = creatures.size();
//                        for (int i = 0; i < agentCount; i++) {
//                            Steerable<Vector2> currentAgent = creatures.get(i);
//
//                            // Skip if this is us, when sharing collission avoidance i.e. this can contain us
//                            if (!currentAgent.equals(owner)) {
//                                if (callback.reportNeighbor(currentAgent)) {
//                                    neighborCount++;
//                                }
//                            }
//                        }
//
//                        return neighborCount;
//                    }
//                });
//                prioritySteering.add(ca);

                // Navigate
                prioritySteering.add(new FollowWaypoints(steerable, path, crowd, worker, chase, ticksPerSecond));
            }

            if (faceTarget != null) {

                // Add reach orientation
                ReachOrientation orient = new ReachOrientation(steerable,
                        new TargetLocation(WorldUtils.pointToVector2(faceTarget),
                                WorldUtils.pointToVector2(p)));
                orient.setDecelerationRadius(0.2f);
                orient.setTimeToTarget(0.001f);
                orient.setAlignTolerance(0.1f);
                prioritySteering.add(orient);
            }

            return prioritySteering;
        }

        return null;
    }

}
