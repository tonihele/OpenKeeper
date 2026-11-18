/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.math.Vector2;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Walks a creature from waypoint to waypoint the way DK II's follow_path
 * steering does (0x4D3C40). The creature heads for its next waypoint from
 * wherever it is, so one pushed aside is not pulled back onto a line.
 * <br>
 * A waypoint is passed within 1.2 of the creature's radii, the walk ends
 * within half a radius of the last one. Workers pass a waypoint within half a
 * tile and stop within a tenth of a tile.
 * <br>
 * DK II works the steering out once a game tick, as a change of the velocity
 * of a tick, and adds it with the weight 0.35 (0x4D56E0, 0x4D4760). The
 * acceleration here spreads that change over the tick.
 * <br>
 * A creature slows down for a crowd round its next waypoint: the share of the
 * way it wants to cover in a tick drops by the sum of the radii of the
 * creatures there, up to 1 (0x4D15E0). Most creatures nearly stop for a crowd
 * of 1, workers don't. A chase does it at the last waypoint too, where a walk
 * otherwise arrives (DK II's path flag 2, 0x471D10). DK II also turns a
 * creature to face its waypoint before walking, that is not done here.
 */
public final class FollowWaypoints extends SteeringBehavior<Vector2> {

    /**
     * DK II's pilot caps every steering force to 1.5 tiles a tick (0x4D5B30)
     */
    public static final float MAX_FORCE = 1.5f;

    /**
     * The weight of the path following (0x4D56E0)
     */
    public static final float WEIGHT = 0x5999 / 65536f;
    private static final float PASS_RADII = 0x13333 / 65536f;
    private static final float WORKER_PASS_DISTANCE = 0.5f;
    private static final float FINISH_RADII = 0.5f;
    private static final float WORKER_FINISH_DISTANCE = 0x1999 / 65536f;

    /**
     * The velocity of a tick a creature wants, as a share of the way to its
     * waypoint
     */
    private static final float SEEK_SHARE = 0x1051E / 65536f;
    private static final float WORKER_SEEK_SHARE = 0x1D332 / 65536f;

    /**
     * At the last waypoint a creature slows down to reach it in two ticks
     */
    private static final float ARRIVAL_TICKS = 2;

    private final List<Vector2> waypoints;
    private final ToDoubleFunction<Vector2> crowd;
    private final boolean worker;
    private final boolean chase;
    private final float ticksPerSecond;
    private int waypoint;
    private boolean finished;

    /**
     * Follows the waypoints from the first one the creature hasn't walked past
     * yet
     *
     * @param owner the walking creature, its bounding radius is its radius
     * @param waypoints the waypoints, the first one is where the walk started
     * @param crowd the sum of the radii of the creatures round a waypoint
     * @param worker whether the creature is a worker
     * @param chase whether the creature chases an attack target
     * @param ticksPerSecond the game ticks a second
     */
    public FollowWaypoints(Steerable<Vector2> owner, List<Vector2> waypoints, ToDoubleFunction<Vector2> crowd, boolean worker,
            boolean chase, int ticksPerSecond) {
        super(owner);

        this.waypoints = waypoints;
        this.crowd = crowd;
        this.worker = worker;
        this.chase = chase;
        this.ticksPerSecond = ticksPerSecond;

        // Rebuilt half way, the creature is already past the waypoints it is nearer to the next one than they are
        Vector2 position = owner.getPosition();
        while (waypoint < waypoints.size() - 1
                && position.dst2(waypoints.get(waypoint + 1)) < waypoints.get(waypoint).dst2(waypoints.get(waypoint + 1))) {
            waypoint++;
        }
    }

    @Override
    protected SteeringAcceleration<Vector2> calculateRealSteering(SteeringAcceleration<Vector2> steering) {
        steering.setZero();
        if (finished) {
            return steering;
        }

        Vector2 position = owner.getPosition();
        int last = waypoints.size() - 1;
        float passDistance = worker ? WORKER_PASS_DISTANCE : PASS_RADII * owner.getBoundingRadius();
        if (waypoint < last && position.dst(waypoints.get(waypoint)) < passDistance) {
            waypoint++;
        }
        Vector2 target = waypoints.get(waypoint);
        float distance = position.dst(target);
        if (waypoint == last && distance < (worker ? WORKER_FINISH_DISTANCE : FINISH_RADII * owner.getBoundingRadius())) {
            finished = true;
            return steering;
        }

        // The velocity of a tick wanted, less with a crowd round the waypoint (0x4D15E0)
        Vector2 desired = steering.linear.set(target).sub(position);
        if (worker) {
            desired.scl(WORKER_SEEK_SHARE - (float) Math.min(crowd.applyAsDouble(target), 1));
        } else if (waypoint < last || chase) {
            desired.scl(SEEK_SHARE - (float) Math.min(crowd.applyAsDouble(target), 1));
        } else {
            desired.scl(Math.min(owner.getMaxLinearSpeed() / ticksPerSecond, distance / ARRIVAL_TICKS) / distance);
        }

        // Its change of the velocity of a tick, spread over the tick
        desired.mulAdd(owner.getLinearVelocity(), -1 / ticksPerSecond).limit(MAX_FORCE).scl(WEIGHT * ticksPerSecond * ticksPerSecond);
        return steering;
    }

}
