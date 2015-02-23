/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map.loader.room;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import toniarts.opendungeonkeeper.tools.convert.map.Room;

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

    public RoomInstance(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }

    public void addCoordinate(Point p) {
        coordinates.add(~Collections.binarySearch(coordinates, p, new PointComparator()), p);
        minX = Math.min(p.x, minX);
        maxX = Math.max(p.x, maxX);
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
