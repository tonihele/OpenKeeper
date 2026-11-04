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
package toniarts.openkeeper.game.navigation;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Vector2;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.navigation.pathfinding.MapDistance;
import toniarts.openkeeper.game.navigation.pathfinding.MapIndexedGraph;
import toniarts.openkeeper.game.navigation.pathfinding.MapPathFinder;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Offers navigation related services
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class NavigationService implements INavigationService {
    
    private static final Logger logger = System.getLogger(NavigationService.class.getName());

    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final MapIndexedGraph pathFindingMap;
    private final MapPathFinder pathFinder;
    private final MapDistance heuristic;

    public NavigationService(IMapController mapController, IEntityPositionLookup entityPositionLookup) {
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;

        pathFindingMap = new MapIndexedGraph(mapController, entityPositionLookup);
        pathFinder = new MapPathFinder(pathFindingMap, false);
        heuristic = new MapDistance();
    }


    @Override
    public Point findRandomAccessibleTile(Point start, int radius, INavigable navigable) {
        return findRandomAccessibleTile(start, radius, navigable, null);
    }

    @Override
    public Point findRandomTileInRoom(Point start, int radius, INavigable navigable) {
        IRoomController roomInstance = mapController.getRoomControllerByCoordinates(start);
        if (roomInstance == null) {
            logger.log(Level.WARNING, () -> "Starting point " + start + " is not in a room!");
            return null;
        }
        Set<Point> allowedTiles = new HashSet<>(roomInstance.getRoomInstance().getCoordinates());

        return findRandomAccessibleTile(start, radius, navigable, allowedTiles);
    }

    private Point findRandomAccessibleTile(Point start, int radius, INavigable navigable, Set<Point> allowedTiles) {
        Set<Point> tiles = HashSet.newHashSet(radius * radius - 1);

        // Start growing the circle, always testing the tile
        getAccessibleNeighbours(mapController.getMapData().getTile(start.x, start.y), radius, navigable, tiles, allowedTiles);
        tiles.remove(start);

        // Take a random point
        if (!tiles.isEmpty()) {
            return Utils.getRandomItem(new ArrayList<>(tiles));
        }
        return null;
    }

    private void getAccessibleNeighbours(IMapTileInformation startTile, int radius, INavigable navigable, Set<Point> tiles, Set<Point> allowedTiles) {
        if (radius > 0) {
            for (int y = startTile.getY() - 1; y <= startTile.getY() + 1; y++) {
                for (int x = startTile.getX() - 1; x <= startTile.getX() + 1; x++) {

                    // If this is good, add and get neighbours
                    IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                    if (tile != null
                            && !tiles.contains(tile.getLocation())
                            && isAccessible(startTile, tile, navigable)
                            && (allowedTiles == null || allowedTiles.contains(tile.getLocation()))) {
                        tiles.add(tile.getLocation());
                        getAccessibleNeighbours(tile, radius - 1, navigable, tiles, allowedTiles);
                    }
                }
            }
        }
    }

    /**
     * Note that this is not thread safe!!
     *
     * @param start
     * @param end
     * @param navigable
     * @return
     */
    @Override
    public GraphPath<IMapTileInformation> findPath(Point start, Point end, INavigable navigable) {
        pathFindingMap.setPathFindable(navigable);
        GraphPath<IMapTileInformation> outPath = new DefaultGraphPath<>();
        IMapTileInformation startTile = mapController.getMapData().getTile(start.x, start.y);
        IMapTileInformation endTile = mapController.getMapData().getTile(end.x, end.y);
        if (startTile != null && endTile != null && pathFinder.searchNodePath(startTile, endTile, heuristic, outPath)) {
            return outPath;
        }
        return null;
    }

    @Override
    public boolean isAccessible(IMapTileInformation from, IMapTileInformation to, INavigable navigable) {
        Float cost = navigable.getCost(from, to, mapController, entityPositionLookup);
        return cost != null;
    }

    @Override
    public List<Vector2> smoothPath(List<Vector2> path, float radius, INavigable navigable) {
        Set<Point> pathTiles = HashSet.newHashSet(path.size());
        path.forEach(point -> pathTiles.add(WorldUtils.vectorToPoint(point)));
        Map<Long, Boolean> steps = new HashMap<>();

        // Keep a point only when the line from the last one kept to the next one is blocked
        List<Vector2> waypoints = new ArrayList<>();
        Vector2 from = path.getFirst();
        waypoints.add(from);
        for (int i = 1; i < path.size() - 1; i++) {
            if (!isWalkable(from, path.get(i + 1), radius, navigable, pathTiles, steps)) {
                from = path.get(i);
                waypoints.add(from);
            }
        }
        if (path.size() > 1) {
            waypoints.add(path.getLast());
        }
        return waypoints;
    }

    /**
     * Can the entity walk straight from one point to another: the line and the
     * two lines its radius to the sides cross only tiles it can walk on
     */
    private boolean isWalkable(Vector2 from, Vector2 to, float radius, INavigable navigable, Set<Point> pathTiles, Map<Long, Boolean> steps) {
        IMapTileInformation start = mapController.getMapData().getTile(WorldUtils.vectorToPoint(from));
        Vector2 side = new Vector2(to).sub(from).nor().rotate90(1).scl(radius);
        return start != null
                && isWalkable(start, from.x, from.y, to.x, to.y, navigable, pathTiles, steps)
                && isWalkable(start, from.x + side.x, from.y + side.y, to.x + side.x, to.y + side.y, navigable, pathTiles, steps)
                && isWalkable(start, from.x - side.x, from.y - side.y, to.x - side.x, to.y - side.y, navigable, pathTiles, steps);
    }

    /**
     * Steps through the tiles a line crosses, in order (Amanatides and Woo).
     * Tile n spans n - 0.5 to n + 0.5.
     */
    private boolean isWalkable(IMapTileInformation start, float fromX, float fromY, float toX, float toY, INavigable navigable,
            Set<Point> pathTiles, Map<Long, Boolean> steps) {
        int x = Math.round(fromX);
        int y = Math.round(fromY);
        int endX = Math.round(toX);
        int endY = Math.round(toY);
        IMapTileInformation tile = start;
        if (x != start.getX() || y != start.getY()) {
            IMapTileInformation next = mapController.getMapData().getTile(x, y);
            if (next == null || !canStep(tile, next, navigable, pathTiles, steps)) {
                return false;
            }
            tile = next;
        }

        float dx = toX - fromX;
        float dy = toY - fromY;
        int stepX = (int) Math.signum(dx);
        int stepY = (int) Math.signum(dy);
        float deltaX = stepX == 0 ? Float.POSITIVE_INFINITY : Math.abs(1 / dx);
        float deltaY = stepY == 0 ? Float.POSITIVE_INFINITY : Math.abs(1 / dy);
        float nextX = stepX == 0 ? Float.POSITIVE_INFINITY : (x + 0.5f * stepX - fromX) / dx;
        float nextY = stepY == 0 ? Float.POSITIVE_INFINITY : (y + 0.5f * stepY - fromY) / dy;
        for (int left = Math.abs(endX - x) + Math.abs(endY - y); left > 0; left--) {
            if (nextX < nextY) {
                x += stepX;
                nextX += deltaX;
            } else if (nextY < nextX) {
                y += stepY;
                nextY += deltaY;
            } else {

                // Through a corner
                x += stepX;
                y += stepY;
                nextX += deltaX;
                nextY += deltaY;
                left--;
            }
            IMapTileInformation next = mapController.getMapData().getTile(x, y);
            if (next == null || !canStep(tile, next, navigable, pathTiles, steps)) {
                return false;
            }
            tile = next;
        }
        return true;
    }

    private boolean canStep(IMapTileInformation from, IMapTileInformation to, INavigable navigable, Set<Point> pathTiles, Map<Long, Boolean> steps) {
        return steps.computeIfAbsent((long) from.getIndex() << 32 | to.getIndex(), key -> {
            Float cost = navigable.getCost(from, to, mapController, entityPositionLookup);
            return cost != null && (cost <= INavigable.DEFAULT_COST || pathTiles.contains(to.getLocation()));
        });
    }

}
