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

import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.behaviors.ReachOrientation;
import com.badlogic.gdx.math.Vector2;

/**
 * The purpose of this class is to make our steering class concrete. Ideal of
 * course is to use interfaces. But using SafeArrayList for example, requires a
 * concrete class. So we use this to mask it so that we can easily keep it still
 * contained.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class EntitySteeringBehavior extends PrioritySteering<Vector2> implements Comparable<EntitySteeringBehavior> {

    private final ISteerableEntity steerableEntity;

    public EntitySteeringBehavior(ISteerableEntity owner) {
        super(owner);

        this.steerableEntity = owner;
    }

    /**
     * Sets the length of the update the steering is worked out for. The path
     * following works out DK II's change of the velocity of a tick once an
     * update, and the facing is not asked to turn faster than one update.
     *
     * @param tpf the length of the update, in seconds
     */
    public void setUpdateInterval(float tpf) {
        for (var behavior : behaviors) {
            if (behavior instanceof FollowWaypoints path) {
                path.setUpdateInterval(tpf);
            } else if (behavior instanceof ReachOrientation<?> orientation) {
                orientation.setTimeToTarget(Math.max(.001f, tpf));
            }
        }
    }

    /**
     * Whether the path following heads for the last waypoint
     *
     * @return true on the last leg of the walk
     */
    public boolean isOnFinalPathSegment() {
        FollowWaypoints pathFollowing = getPathFollowing();
        return pathFollowing != null && pathFollowing.isOnLastLeg();
    }

    /**
     * The path following of the steering
     *
     * @return the path following, null if the entity only turns
     */
    public FollowWaypoints getPathFollowing() {
        for (SteeringBehavior<Vector2> behavior : behaviors) {
            if (behavior instanceof FollowWaypoints followWaypoints) {
                return followWaypoints;
            }
        }
        return null;
    }

    @Override
    public int compareTo(EntitySteeringBehavior o) {
        return steerableEntity.compareTo(o.steerableEntity);
    }

}
