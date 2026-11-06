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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Stands guard in the Guard Room. As in DK II, the guard goes to the guard
 * post nearest to it, a corner of the room, and after a while walks on to
 * another post.
 */
public final class Guard extends AbstractCapacityCriticalRoomTask {

    /**
     * The least time a guard stands at a guard post, in logic ticks
     */
    public static final int GUARD_POST_TICKS = 75;
    /**
     * The least time a guard stands on a plain room tile, in logic ticks. Used
     * when the room has no other guard post to go to.
     */
    public static final int ROOM_TILE_TICKS = 50;

    private final int ticksPerSecond;
    private final int maxExtraTicks;
    private final RandomGenerator random;
    private Point guardPost;
    private float arrivalTime = -1;
    private float standTime;

    /**
     * A guard duty in the Guard Room
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the tile reserved for the guard in the room
     * @param playerId the owner of the room
     * @param room the Guard Room
     * @param taskManager the task manager
     * @param ticksPerSecond the logic ticks per second of the level
     * @param maxExtraTicks at most this many random ticks are added to the
     * time a guard stands at a post
     * @param random the random number generator
     */
    public Guard(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            IRoomController room, TaskManager taskManager, int ticksPerSecond, int maxExtraTicks, RandomGenerator random) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.ticksPerSecond = ticksPerSecond;
        this.maxExtraTicks = maxExtraTicks;
        this.random = random;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        if (guardPost == null) {
            guardPost = getNearest(getGuardPosts(getRoomTiles()), creature.getCreatureCoordinates());
        }
        return WorldUtils.pointToVector2f(guardPost);
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.GUARD;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // The guard has just come to its post
        if (arrivalTime < 0) {
            arrivalTime = executionDuration;
            standTime = getStandTicks(getGuardPosts(getRoomTiles()).contains(guardPost)) / (float) ticksPerSecond;
            return;
        }

        if (executionDuration - arrivalTime >= standTime) {
            guardPost = getNextGuardPost();
            arrivalTime = -1;
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GUARD;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }

    private List<Point> getRoomTiles() {
        return getRoom().getRoomInstance().getCoordinates();
    }

    private int getStandTicks(boolean atGuardPost) {
        int ticks = (atGuardPost ? GUARD_POST_TICKS : ROOM_TILE_TICKS);
        if (maxExtraTicks > 0) {
            ticks += random.nextInt(maxExtraTicks);
        }
        return ticks;
    }

    private Point getNextGuardPost() {
        List<Point> roomTiles = getRoomTiles();
        List<Point> candidates = getGuardPosts(roomTiles);
        candidates.remove(guardPost);
        if (candidates.isEmpty()) {

            // No other guard post, just walk around the room
            candidates = new ArrayList<>(roomTiles);
            candidates.remove(guardPost);
        }
        if (candidates.isEmpty()) {
            return guardPost;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Gets the guard posts of a Guard Room. A tile is a guard post when the
     * room doesn't go on at both of its opposite sides, so the posts are the
     * corners of the room and the ends of its one tile wide parts.
     *
     * @param roomTiles the tiles of the room
     * @return the guard post tiles
     */
    public static List<Point> getGuardPosts(Collection<Point> roomTiles) {
        Set<Point> tiles = new HashSet<>(roomTiles);
        List<Point> guardPosts = new ArrayList<>();
        for (Point p : roomTiles) {
            boolean horizontal = tiles.contains(new Point(p.x - 1, p.y)) && tiles.contains(new Point(p.x + 1, p.y));
            boolean vertical = tiles.contains(new Point(p.x, p.y - 1)) && tiles.contains(new Point(p.x, p.y + 1));
            if (!horizontal && !vertical) {
                guardPosts.add(p);
            }
        }
        return guardPosts;
    }

    private Point getNearest(List<Point> points, Point from) {
        Point nearest = getTaskLocation();
        int nearestDistance = Integer.MAX_VALUE;
        for (Point p : points) {
            int distance = WorldUtils.calculateDistance(from, p);
            if (distance < nearestDistance) {
                nearest = p;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

}
