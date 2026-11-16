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
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.map.IMapDataInformation;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Offers navigation related services
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class NavigationService implements INavigationService, IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(NavigationService.class.getName());

    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final MapIndexedGraph pathFindingMap;
    private final MapPathFinder pathFinder;
    private final MapDistance heuristic;
    private final Map<NavigableKey, Map<Point, Set<Point>>> reachableAreas = new ConcurrentHashMap<>();

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
    public boolean isReachable(Point start, Point target, INavigable navigable) {
        Map<Point, Set<Point>> areas = reachableAreas.computeIfAbsent(new NavigableKey(navigable), key -> new ConcurrentHashMap<>());
        Set<Point> area = areas.get(start);
        if (area == null) {
            area = getReachableArea(start, navigable);

            // Every tile in the area has the same area. Unless the start can't be stepped back on, a locked door say
            IMapDataInformation<?> mapData = mapController.getMapData();
            Set<Point> startArea = area;
            if (Arrays.stream(WorldUtils.getSurroundingTiles(mapData, start, false)).anyMatch(p -> !p.equals(start)
                    && startArea.contains(p) && isAccessible(mapData.getTile(p), mapData.getTile(start), navigable))) {
                area.forEach(p -> areas.put(p, startArea));
            } else {
                areas.put(start, area);
            }
        }
        return area.contains(target);
    }

    /**
     * The tiles the entity can get to from the start, the same steps the path
     * finding takes. DK II keeps the connected areas of the map, so a path to
     * somewhere out of reach fails at once (DKII.EXE 0x4C9400, 0x4D0150)
     *
     * @param start the start point
     * @param navigable the entity
     * @return the reachable tiles, the start included
     */
    private Set<Point> getReachableArea(Point start, INavigable navigable) {
        IMapDataInformation<?> mapData = mapController.getMapData();
        Set<Point> area = new HashSet<>();
        area.add(start);
        Deque<Point> open = new ArrayDeque<>(area);
        while (!open.isEmpty()) {
            Point p = open.poll();
            for (Point next : WorldUtils.getSurroundingTiles(mapData, p, false)) {
                if (!area.contains(next) && isAccessible(mapData.getTile(p), mapData.getTile(next), navigable)) {
                    area.add(next);
                    open.add(next);
                }
            }
        }
        return area;
    }

    @Override
    public boolean isAccessible(IMapTileInformation from, IMapTileInformation to, INavigable navigable) {
        Float cost = navigable.getCost(from, to, mapController, entityPositionLookup);
        return cost != null;
    }

    @Override
    public void processTick(float tpf) {

        // The reachable areas are a cache for a tick, the map and the doors change
        reachableAreas.clear();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    /**
     * What the reachable area depends on. A navigable with costs of its own,
     * one that digs through rock say, is kept apart by its type
     */
    private record NavigableKey(Class<?> type, short ownerId, boolean canFly, boolean canWalkOnWater, boolean canWalkOnLava,
            boolean canMoveDiagonally) {

        NavigableKey(INavigable navigable) {
            this(navigable.getClass(), navigable.getOwnerId(), navigable.canFly(), navigable.canWalkOnWater(),
                    navigable.canWalkOnLava(), navigable.canMoveDiagonally());
        }

    }

}
