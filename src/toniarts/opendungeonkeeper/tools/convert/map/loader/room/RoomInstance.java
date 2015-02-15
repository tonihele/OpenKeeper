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

    public RoomInstance(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }

    public void addCoordinate(Point p) {
        coordinates.add(~Collections.binarySearch(coordinates, p, new PointComparator()), p);
    }

    public List<Point> getCoordinates() {
        return coordinates;
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
