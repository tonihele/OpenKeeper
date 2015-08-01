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
package toniarts.openkeeper.world.room;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Holds a room instance, series of coordinates that together form a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomInstance {

    private final Room room;
    private List<Point> coordinates = new ArrayList<>();
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private List<Integer> wallIndexes = new ArrayList<>();
    private int wallPointer = 0;
    public short waterResource;

    public RoomInstance(Room room) {
        if (room == null) {
            throw new RuntimeException("Room can not be null");
        }
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }

    public void addCoordinate(Point p) {
        coordinates.add(~Collections.binarySearch(coordinates, p, new PointComparator()), p);
        minX = Math.min(p.x, minX);
        maxX = Math.max(p.x, maxX);
        minY = Math.min(p.y, minY);
        maxY = Math.max(p.y, maxY);
    }

    public boolean hasCoordinate(Point p) {
        return coordinates.contains(p);
    }

    public Point getCenter() {
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    /**
     * Get the coordinates as a sorted list
     *
     * @return the coordinates list
     * @see #getCoordinatesAsMatrix()
     */
    public List<Point> getCoordinates() {
        return coordinates;
    }

    public void addWallIndexes(Integer... index) {
        this.wallIndexes.addAll(Arrays.asList(index));
    }

    public int getWallIndexNext() {
        if (wallPointer >= wallIndexes.size()) {
            wallPointer = 0;
        }
        return wallIndexes.get(wallPointer);
    }

    /**
     * Some building are nowhere near squares, the matrix will help in building
     * such rooms<br>
     * The matrix is build in room constraints, so you need to add the first
     * coordinate to the matrix coordinate to get the real world coordinate
     *
     * @return coordinates as matrix, true value signifies presense of the room
     * instance in the coordinate
     * @see #getCoordinates()
     */
    public boolean[][] getCoordinatesAsMatrix() {
        int width = maxX - minX + 1;
        Point start = coordinates.get(0);
        int height = coordinates.get(coordinates.size() - 1).y - start.y + 1;
        boolean[][] matrix = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                matrix[x][y] = (Collections.binarySearch(coordinates, new Point(start.x + x, start.y + y), new PointComparator()) > -1);
            }
        }
        return matrix;
    }

    @Override
    public String toString() {
        return room.toString();
    }

    /**
     * Sorts coordinates so that they are in order (helps room
     * construction?)<br>
     * The order is "natural", starting from origin
     */
    private final class PointComparator implements Comparator<Point> {

        @Override
        public int compare(Point o1, Point o2) {
            int result = Integer.compare(o1.y, o2.y);
            if (result == 0) {
                result = Integer.compare(o1.x, o2.x);
            }
            return result;
        }
    }
}
