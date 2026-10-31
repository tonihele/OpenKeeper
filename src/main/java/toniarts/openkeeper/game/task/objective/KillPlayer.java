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
package toniarts.openkeeper.game.task.objective;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

/**
 * Kill player objective for those goodly heroes. Can create a complex set of
 * tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class KillPlayer extends AbstractObjectiveTask {

    /**
     * North, east, south and west, in the order DK II turns through them
     */
    private static final Point[] DIRECTIONS = {new Point(0, -1), new Point(1, 0), new Point(0, 1), new Point(-1, 0)};

    /**
     * How far past an obstacle DK II looks for the tile where the tunnel goes
     * on along its line
     */
    private static final int MAX_OBSTACLE_LENGTH = 81;

    protected final short targetPlayerId;
    protected final ICreatureController creature;
    protected final ILevelInfo levelInfo;
    private final Point target;

    public KillPlayer(final INavigationService navigationService, final IMapController mapController, final ILevelInfo levelInfo, short targetPlayerId, ICreatureController creature) {
        super(navigationService, mapController, levelInfo.getPlayer(targetPlayerId).getDungeonHeartLocation(), creature.getOwnerId());

        this.targetPlayerId = targetPlayerId;
        this.creature = creature;
        this.levelInfo = levelInfo;
        this.target = pickTarget();
        createSubTasks();
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (!isPlayerDestroyed() && creature != null && !getTaskQueue().isEmpty()) {

            // Check that the objectives are still the same
            return Thing.HeroParty.Objective.KILL_PLAYER.equals(creature.getObjective()) && Short.valueOf(targetPlayerId).equals(creature.getObjectiveTargetPlayerId());
        }
        return !isPlayerDestroyed() && !getTaskQueue().isEmpty();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return new Vector2f(target.x, target.y);
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

    }

    /**
     * Tells whether the target keeper takes part in the level and has a
     * Dungeon Heart to go for. Maps can give heroes a keeper to kill whose slot
     * is left empty, or a side, such as the heroes' own, that has no heart. A
     * defeated keeper has lost his heart, so there is nothing left to go for
     *
     * @param levelInfo the level
     * @param targetPlayerId the keeper to kill
     * @return true if there is such a keeper to go after
     */
    public static boolean isTargetPresent(ILevelInfo levelInfo, short targetPlayerId) {
        Keeper keeper = levelInfo.getPlayer(targetPlayerId);
        return keeper != null && !keeper.isDestroyed() && keeper.getDungeonHeartLocation() != null;
    }

    private boolean isPlayerDestroyed() {
        Keeper keeper = levelInfo.getPlayer(targetPlayerId);
        return keeper == null || keeper.isDestroyed();
    }

    /**
     * The heart tile to go for. Like DK II (0x462490 with CRoom_4E4D90), a
     * random tile of the heart room, or the next one of the room after it
     * that can be stood on
     */
    private Point pickTarget() {
        IRoomController heart = mapController.getRoomControllerByCoordinates(getTaskLocation());
        List<Point> points = heart.getRoomInstance().getCoordinates();
        int first = FastMath.rand.nextInt(points.size());
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get((first + i) % points.size());
            if (heart.isTileAccessible(null, point)) {
                return point;
            }
        }
        return points.get(first);
    }

    /**
     * Walks to the heart if it can. If not, a hero that can dig tunnels there
     * the way DK II does (0x485C10 and 0x485EF0): along a straight line, a
     * step at a time on the axis with more of the way left (the y axis when
     * they are even), digging what is solid. Where the line meets rock that
     * can't be dug, it follows the wall round until it is back on the line,
     * digging into the wall where it can. After every tile it digs it walks
     * if it now can.
     */
    private void createSubTasks() {
        Set<Point> walkable = getWalkableToTarget();
        if (walkable.contains(creature.getCreatureCoordinates())
                || !(creature.isWorker() || (creature.getParty() != null && creature.getParty().isWorkersAvailable()))) {
            return;
        }

        for (Point tile : planTunnel(walkable)) {
            addSubTask(new ObjectiveTaskDecorator(getId(), new ObjectiveDigTileTask(navigationService, mapController, tile, playerId)) {

                @Override
                public boolean isWorkerPartyTask() {
                    return true;
                }

            });
            addSubTask(new ObjectiveTaskDecorator(getId(), new GoToTask(navigationService, mapController, tile, playerId)));
        }
    }

    /**
     * The tiles to dig, in order, until the hero can walk to the target or
     * the tunnel can't go on
     */
    private List<Point> planTunnel(Set<Point> walkable) {
        List<Point> digs = new ArrayList<>();
        Set<Point> dug = new HashSet<>();
        int maxDigs = mapController.getMapData().getWidth() * mapController.getMapData().getHeight();
        Point current = creature.getCreatureCoordinates();
        boolean followingWall = false;
        int direction = 0;
        int wallSide = 0;
        Point obstacle = null;
        Point rejoin = null;
        while (digs.size() < maxDigs) {
            Point dig = null;
            if (!followingWall) {

                // Along the line, over open ground, until something solid
                Point next = current;
                do {
                    if (next.equals(target)) {
                        return digs;
                    }
                    current = next;
                    next = stepTowardsTarget(current);
                } while (!isSolid(next, dug) && !isLava(next));
                if (canDig(next, dug)) {
                    dig = next;
                } else {

                    // Round the obstacle, back to the line where it can be dug or walked again
                    obstacle = current;
                    rejoin = getRejoinTile(current, dug);
                    if (rejoin == null) {
                        return digs;
                    }
                    direction = getDirection(current, next);
                    wallSide = getWallSide(current, rejoin, direction, dug);
                    direction = turn(direction, -wallSide);
                    followingWall = true;
                }
            }
            if (followingWall) {
                Point position = current;
                int passes = 4;
                while (dig == null) {
                    if (position.equals(rejoin)) {
                        followingWall = false;
                        current = rejoin;
                        if (isSolid(rejoin, dug)) {
                            dig = rejoin;
                        }
                        break;
                    }

                    // Into the wall if it can be dug or opens up, else turn away from it until there is a way on
                    boolean moved = false;
                    boolean movedIntoWall = false;
                    int turn = turn(direction, wallSide);
                    for (int i = 0; i < 5 && dig == null && !moved; i++) {
                        Point neighbour = getNeighbour(position, turn);
                        if (canDig(neighbour, dug)) {
                            dig = neighbour;
                            direction = turn;
                        } else if (!isSolid(neighbour, dug) && !isLava(neighbour)) {
                            position = neighbour;
                            direction = turn;
                            moved = true;
                            movedIntoWall = (i == 0);
                        } else {
                            turn = (i == 0 ? direction : turn(turn, -wallSide));
                        }
                    }
                    if (dig == null && (!moved || (position.equals(obstacle) && (movedIntoWall || --passes == 0)))) {
                        return digs;
                    }
                }
            }
            if (dig != null) {
                digs.add(dig);
                dug.add(dig);
                current = dig;
                if (isNextToAny(dig, walkable)) {
                    return digs;
                }
            }
        }
        return digs;
    }

    /**
     * The first tile on the line past an obstacle that isn't impenetrable,
     * the hero's own wall or lava
     */
    private Point getRejoinTile(Point from, Set<Point> dug) {
        Point tile = from;
        for (int i = 0; i < MAX_OBSTACLE_LENGTH; i++) {
            tile = stepTowardsTarget(tile);
            if (isPassable(tile, dug)) {
                return tile;
            }
        }
        return null;
    }

    /**
     * Which side to keep the wall on. DK II (0x4868D0) sends a walker round
     * each way, through anything that isn't impenetrable, the hero's own wall
     * or lava, and takes the side of the one that is back on the line first
     *
     * @return -1 to keep the wall on the left, 1 on the right
     */
    private int getWallSide(Point start, Point rejoin, int direction, Set<Point> dug) {
        Point left = start;
        Point right = start;
        int leftDirection = turn(direction, 1);
        int rightDirection = turn(direction, -1);
        int starts = 0;
        int maxSteps = 4 * mapController.getMapData().getWidth() * mapController.getMapData().getHeight();
        for (int i = 0; i < maxSteps; i++) {
            if (left.equals(start) && ++starts == 4) {
                break;
            }
            if (left.equals(rejoin)) {
                return -1;
            }
            leftDirection = walkAlongWall(left, leftDirection, -1, dug);
            left = getNeighbour(left, leftDirection);
            if (right.equals(rejoin)) {
                return 1;
            }
            rightDirection = walkAlongWall(right, rightDirection, 1, dug);
            right = getNeighbour(right, rightDirection);
        }
        return 1;
    }

    /**
     * The way a walker keeping the wall on the given side goes on
     *
     * @return the direction of the step, the walker stays put if there is no
     * way on
     */
    private int walkAlongWall(Point position, int direction, int wallSide, Set<Point> dug) {
        int turn = turn(direction, wallSide);
        if (isPassable(getNeighbour(position, turn), dug)) {
            return turn;
        }
        turn = direction;
        for (int i = 0; i < 4; i++) {
            if (isPassable(getNeighbour(position, turn), dug)) {
                return turn;
            }
            turn = turn(turn, -wallSide);
        }
        return -1;
    }

    /**
     * The tiles the hero can walk to the target from, on the map as it is
     */
    private Set<Point> getWalkableToTarget() {
        Set<Point> walkable = new HashSet<>();
        Deque<IMapTileInformation> open = new ArrayDeque<>();
        IMapTileInformation start = mapController.getMapData().getTile(target);
        if (start != null) {
            walkable.add(target);
            open.add(start);
        }
        while (!open.isEmpty()) {
            IMapTileInformation tile = open.poll();
            for (Point direction : DIRECTIONS) {
                IMapTileInformation neighbour = mapController.getMapData().getTile(tile.getX() + direction.x, tile.getY() + direction.y);
                if (neighbour != null && !walkable.contains(neighbour.getLocation()) && navigationService.isAccessible(neighbour, tile, creature)) {
                    walkable.add(neighbour.getLocation());
                    open.add(neighbour);
                }
            }
        }
        return walkable;
    }

    private Point stepTowardsTarget(Point from) {
        int dx = target.x - from.x;
        int dy = target.y - from.y;
        if (Math.abs(dy) < Math.abs(dx)) {
            return new Point(from.x + (dx < 0 ? -1 : 1), from.y);
        }
        return new Point(from.x, from.y + (dy < 0 ? -1 : 1));
    }

    private static int getDirection(Point from, Point to) {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (from.x + DIRECTIONS[i].x == to.x && from.y + DIRECTIONS[i].y == to.y) {
                return i;
            }
        }
        throw new IllegalArgumentException(to + " is not next to " + from);
    }

    private static int turn(int direction, int quarters) {
        return Math.floorMod(direction + quarters, DIRECTIONS.length);
    }

    private static Point getNeighbour(Point point, int direction) {
        if (direction < 0) {
            return point;
        }
        return new Point(point.x + DIRECTIONS[direction].x, point.y + DIRECTIONS[direction].y);
    }

    private static boolean isNextToAny(Point point, Set<Point> tiles) {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (tiles.contains(getNeighbour(point, i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rock, gold and the walls of others can be dug, the hero's own walls and
     * impenetrable rock can't
     */
    private boolean canDig(Point point, Set<Point> dug) {
        return isSolid(point, dug) && isPassable(point, dug);
    }

    private boolean isPassable(Point point, Set<Point> dug) {
        Terrain terrain = getTerrain(point);
        if (terrain == null || terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            return false;
        }
        return !isSolid(point, dug) || (!terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)
                && mapController.getMapData().getTile(point).getOwnerId() != creature.getOwnerId());
    }

    private boolean isSolid(Point point, Set<Point> dug) {
        Terrain terrain = getTerrain(point);
        return terrain == null || (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && !dug.contains(point));
    }

    private boolean isLava(Point point) {
        Terrain terrain = getTerrain(point);
        return terrain != null && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA);
    }

    private Terrain getTerrain(Point point) {
        if (point.x < 0 || point.y < 0 || point.x >= mapController.getMapData().getWidth() || point.y >= mapController.getMapData().getHeight()) {
            return null;
        }
        return mapController.getTerrain(mapController.getMapData().getTile(point));
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.KILL_PLAYER;
    }

}
