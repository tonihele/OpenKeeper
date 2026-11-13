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
import com.jme3.math.FastMath;
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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Offers navigation related services
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class NavigationService implements INavigationService {

    private static final Logger logger = System.getLogger(NavigationService.class.getName());

    /**
     * The rooms an idle creature may walk to (0x480A00): Treasury, Lair,
     * Hatchery, Library, Training Room, Guard Room, Work Shop, Prison, Torture
     * Chamber, Temple, Graveyard, Casino and Combat Pit
     */
    private static final Set<Short> IDLE_WALK_ROOM_IDS = Set.of((short) 1, (short) 2, (short) 4, (short) 6, (short) 7,
            (short) 9, (short) 10, (short) 11, (short) 12, (short) 13, (short) 14, (short) 15, (short) 16);

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
    public Point findIdleWalkTile(Point start, INavigable navigable) {

        // One walk in eight goes to a room instead (0x480A00)
        if (FastMath.rand.nextInt(8) == 0) {
            Point p = findIdleWalkTileInRoom(navigable);
            if (p != null) {
                return p;
            }
        }

        for (int radius = 4 + FastMath.rand.nextInt(4); radius > 0; radius--) {
            int ringSize = radius * 8;
            int first = FastMath.rand.nextInt(ringSize);
            for (int i = 0; i < ringSize; i++) {
                int index = (first + i) % ringSize;
                int side = index / (radius * 2);
                int along = index % (radius * 2) - radius;
                Point p = switch (side) {
                    case 0 -> new Point(start.x + along, start.y - radius);
                    case 1 -> new Point(start.x + radius, start.y + along);
                    case 2 -> new Point(start.x - along, start.y + radius);
                    default -> new Point(start.x - radius, start.y - along);
                };
                IMapTileInformation tile = mapController.getMapData().getTile(p);
                if (tile != null && tile.getOwnerId() == navigable.getOwnerId()
                        && navigable.getCost(null, tile, mapController, entityPositionLookup) != null
                        && !isDungeonHeart(p)) {
                    return p;
                }
            }
        }

        // No ring has a tile: DK II walks to a spot inside the creature's own tile (0x4808E0)
        return start;
    }

    /**
     * DK II counts the keeper's rooms other than the Portal, the Dungeon
     * Heart, the bridges and the Hero Gates, and then counts them down again
     * without drawing a random number. So it always ends on the last of them
     * in the keeper's list, where a new room is put first (0x4E3800): the
     * oldest one. The creature walks to a random tile of it that it can stand
     * on (0x4E8430).
     */
    private Point findIdleWalkTileInRoom(INavigable navigable) {
        IRoomController oldest = null;
        for (IRoomController room : mapController.getRoomControllers()) {
            if (room.getOwnerId() == navigable.getOwnerId() && IDLE_WALK_ROOM_IDS.contains(room.getRoomId())
                    && !room.isDestroyed() && (oldest == null || room.getEntityId().getId() < oldest.getEntityId().getId())) {
                oldest = room;
            }
        }
        if (oldest == null) {
            return null;
        }
        List<Point> tiles = new ArrayList<>();
        for (Point p : oldest.getRoomInstance().getCoordinates()) {
            IMapTileInformation tile = mapController.getMapData().getTile(p);
            if (tile != null && navigable.getCost(null, tile, mapController, entityPositionLookup) != null) {
                tiles.add(p);
            }
        }
        return tiles.isEmpty() ? null : Utils.getRandomItem(tiles);
    }

    private boolean isDungeonHeart(Point p) {
        IRoomController room = mapController.getRoomControllerByCoordinates(p);
        return room != null && room.isDungeonHeart();
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

}
