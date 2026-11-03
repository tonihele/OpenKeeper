/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.view.map;

import com.jme3.math.FastMath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

/**
 * Single wall section
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public record WallSection(toniarts.openkeeper.view.map.WallSection.WallDirection direction, List<Point> coordinates) {

    public enum WallDirection {

        EAST(-FastMath.HALF_PI), NORTH(0), WEST(FastMath.HALF_PI), SOUTH(FastMath.PI);

        WallDirection(float angle) {
            this.angle = angle;
        }

        public float getAngle() {
            return angle;
        }

        private final float angle;
    }

    /**
     * Find room wall sections, continuous sections facing the same way
     *
     * @param roomInstance room instance
     * @param mapData the map
     * @param kwdFile the KWD file
     * @return the wall sections of the room
     */
    public static List<WallSection> findWallSections(RoomInstance roomInstance, IMapDataInformation<?> mapData,
            IKwdFile kwdFile) {
        List<WallSection> sections = new ArrayList<>();
        Map<Point, Set<WallDirection>> alreadyWalledPoints = new HashMap<>();
        for (Point p : roomInstance.getCoordinates()) {

            // Traverse in all four directions to find wallable sections facing the same direction
            traverseRoomWalls(p, roomInstance, WallDirection.NORTH, sections, alreadyWalledPoints, mapData, kwdFile);
            traverseRoomWalls(p, roomInstance, WallDirection.EAST, sections, alreadyWalledPoints, mapData, kwdFile);
            traverseRoomWalls(p, roomInstance, WallDirection.SOUTH, sections, alreadyWalledPoints, mapData, kwdFile);
            traverseRoomWalls(p, roomInstance, WallDirection.WEST, sections, alreadyWalledPoints, mapData, kwdFile);
        }
        return sections;
    }

    /**
     * Traverses room walls facing in certain direction from a room point. Finds
     * a section.
     *
     * @param p starting room point
     * @param roomInstance the room instance of which walls we need to find
     * @param direction the direction of which the want the walls to face
     * @param sections list of already find sections
     * @param alreadyWalledPoints already found wall points
     */
    private static void traverseRoomWalls(Point p, RoomInstance roomInstance, WallDirection direction,
            List<WallSection> sections, Map<Point, Set<WallDirection>> alreadyWalledPoints,
            IMapDataInformation<?> mapData, IKwdFile kwdFile) {

        Set<WallDirection> alreadyTraversedDirections = alreadyWalledPoints.get(p);
        if (alreadyTraversedDirections == null || !alreadyTraversedDirections.contains(direction)) {

            List<Point> section = getRoomWalls(p, roomInstance, direction, mapData, kwdFile);
            if (!section.isEmpty()) {

                // Add section
                sections.add(new WallSection(direction, section));

                // Add to known points
                for (Point sectionPoint : section) {
                    Set<WallDirection> directions = alreadyWalledPoints.get(sectionPoint);
                    if (directions == null) {
                        directions = EnumSet.noneOf(WallDirection.class);
                    }
                    directions.add(direction);
                    alreadyWalledPoints.put(sectionPoint, directions);
                }
            }
        }
    }

    private static List<Point> getRoomWalls(Point p, RoomInstance roomInstance, WallDirection wallDirection,
            IMapDataInformation<?> mapData, IKwdFile kwdFile) {

        // A missing direction is treated as WEST
        WallDirection direction = wallDirection != null ? wallDirection : WallDirection.WEST;

        // See if the starting point has a wall to the given direction
        Point wallPoint = getWallNeighbour(p, direction);
        IMapTileInformation tile = mapData.getTile(wallPoint.x, wallPoint.y);
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                || !terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS)) {
            return Collections.emptyList();
        }

        // Found wallable
        List<Point> wallPoints = new ArrayList<>();
        wallPoints.add(p);

        // Traverse possible directions, well one direction, "to the right"
        Point nextPoint = getNextWallPoint(p, direction);
        if (roomInstance.hasCoordinate(nextPoint)) {
            wallPoints.addAll(getRoomWalls(nextPoint, roomInstance, wallDirection, mapData, kwdFile));
        }

        return wallPoints;
    }

    /**
     * The tile on the wall side of the room tile
     *
     * @param p the room tile
     * @param direction the direction the wall faces
     * @return the wall tile
     */
    public static Point getWallNeighbour(Point p, WallDirection direction) {
        return switch (direction) {
            case NORTH ->
                new Point(p.x, p.y + 1);
            case EAST ->
                new Point(p.x - 1, p.y);
            case SOUTH ->
                new Point(p.x, p.y - 1);
            default ->
                new Point(p.x + 1, p.y); // West
        };
    }

    /**
     * The next tile along the wall ("to the right", sorting forces the
     * directions to be what they are)
     */
    private static Point getNextWallPoint(Point p, WallDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH ->
                new Point(p.x + 1, p.y); // East
            default ->
                new Point(p.x, p.y + 1); // South
        };
    }

}
