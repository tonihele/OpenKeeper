/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class RoomPerimeterTest {
    @Test
    void singleTileMakesAClosedInsetOutline() {
        var path = new RoomPerimeter(List.of(new Point(10, 20)));
        assertEquals(4, path.size());
        assertEquals(new Vector3f(9.625f, 1, 20), path.positionAt(0.5));
        assertEquals(new Vector3f(10, 1, 19.625f), path.positionAt(1.5));
        assertEquals(new Vector3f(10.375f, 1, 20), path.positionAt(2.5));
        assertEquals(new Vector3f(10, 1, 20.375f), path.positionAt(3.5));
        assertEquals(path.positionAt(0.5), path.positionAt(4.5));
        assertEquals(path.positionAt(0), path.positionAt(0.125));
        assertEquals(path.positionAt(0.875), path.positionAt(0.999));
    }

    @Test
    void portalHasTwelveEdgesAndNoInteriorEdges() {
        var path = new RoomPerimeter(square(3));
        assertEquals(12, path.size());
        assertEquals(new Vector3f(0, 1, -0.375f), path.positionAt(1.5));
        assertEquals(new Vector3f(2.375f, 1, 1), path.positionAt(5.5));
        for (int edge = 0; edge < path.size(); edge++) {
            assertNotEquals(new Point(1, 1), path.tileAt(edge));
        }
    }

    @Test
    void concaveRoomFollowsItsRecessInsteadOfItsBoundingBox() {
        var path = new RoomPerimeter(List.of(new Point(0, 0), new Point(1, 0), new Point(0, 1)));
        assertEquals(8, path.size());
        assertEquals(new Vector3f(1, 1, 0.375f), path.positionAt(4.5));
        assertEquals(new Vector3f(0.375f, 1, 1), path.positionAt(5.5));
    }

    @Test
    void innerCourtyardDoesNotStartASecondTrail() {
        var tiles = square(3);
        tiles.remove(new Point(1, 1));
        assertEquals(12, new RoomPerimeter(tiles).size());
    }

    @Test
    void emptyRoomHasNoEdges() {
        assertEquals(0, new RoomPerimeter(List.of()).size());
    }

    private static ArrayList<Point> square(int size) {
        var tiles = new ArrayList<Point>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                tiles.add(new Point(x, y));
            }
        }
        return tiles;
    }
}
