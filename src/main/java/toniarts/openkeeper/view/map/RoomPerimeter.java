/* Copyright (C) 2026 OpenKeeper. Licensed under the GNU General Public License, version 3 or later. */
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import java.util.*;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/** The directed outer tile edges of a connected room, with the room on the right. */
public final class RoomPerimeter {
    private record Edge(Point tile, int side) {
        Point start() {
            return new Point(tile.x + (side == 1 || side == 2 ? 1 : 0),
                    tile.y + (side >= 2 ? 1 : 0));
        }
        Point end() {
            return new Point(tile.x + (side <= 1 ? 1 : 0),
                    tile.y + (side == 1 || side == 2 ? 1 : 0));
        }
    }

    private final List<Edge> edges;

    public RoomPerimeter(Collection<Point> coordinates) {
        Set<Point> tiles = new HashSet<>(coordinates);
        Map<Point, List<Edge>> outgoing = new HashMap<>();
        Point first = tiles.stream().min(Comparator.comparingInt((Point p) -> p.y)
                .thenComparingInt(p -> p.x)).orElse(null);
        for (Point tile : tiles) {
            for (int side = 0; side < 4; side++) {
                int dx = side == 1 ? 1 : side == 3 ? -1 : 0;
                int dy = side == 2 ? 1 : side == 0 ? -1 : 0;
                if (!tiles.contains(new Point(tile.x + dx, tile.y + dy))) {
                    Edge edge = new Edge(tile, side);
                    outgoing.computeIfAbsent(edge.start(), p -> new ArrayList<>()).add(edge);
                }
            }
        }
        List<Edge> outline = new ArrayList<>();
        if (first != null) {
            Edge start = new Edge(first, 3);
            Edge edge = start;
            do {
                outline.add(edge);
                Edge current = edge;
                // Turn right first at vertices shared by diagonally touching tiles.
                edge = outgoing.get(edge.end()).stream().min(Comparator.comparingInt(next -> {
                    int turn = Math.floorMod(next.side - current.side, 4);
                    return turn == 1 ? 0 : turn == 0 ? 1 : turn == 3 ? 2 : 3;
                })).orElseThrow();
            } while (!edge.equals(start));
        }
        edges = List.copyOf(outline);
    }

    public int size() {
        return edges.size();
    }

    public Point tileAt(double distance) {
        return edgeAt(distance).tile;
    }

    private Edge edgeAt(double distance) {
        return edges.get(Math.floorMod((int) Math.floor(distance), edges.size()));
    }

    /** Each tile edge occupies one travel unit; pause briefly at its inset corners. */
    public Vector3f positionAt(double distance) {
        Edge edge = edgeAt(distance);
        float phase = (float) (distance - Math.floor(distance));
        float along = Math.clamp(phase, 0.125f, 0.875f) - 0.5f;
        float x = edge.tile.x;
        float z = edge.tile.y;
        switch (edge.side) {
            case 0 -> { x += along; z -= 0.375f; }
            case 1 -> { x += 0.375f; z += along; }
            case 2 -> { x -= along; z += 0.375f; }
            case 3 -> { x -= 0.375f; z -= along; }
            default -> throw new AssertionError(edge.side);
        }
        return new Vector3f(x, WorldUtils.FLOOR_HEIGHT, z);
    }
}
