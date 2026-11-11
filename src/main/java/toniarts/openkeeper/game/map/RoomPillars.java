/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.map;

import com.jme3.math.FastMath;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.map.WallSection.WallDirection;
import toniarts.openkeeper.utils.Point;

/**
 * Room pillar definitions and placement rules shared by room construction and
 * terrain rendering. The object id is part of the room data contract; the
 * placement policy describes the two layouts used by the original game.
 */
public final class RoomPillars {

    private enum PlacementPolicy {
        NONE,
        INTERIOR_CORNERS,
        EXPOSED_CORNERS
    }

    private record Definition(short objectId, PlacementPolicy policy) {
    }

    /** A pillar object and its room tile-space transform. */
    public record Placement(short objectId, Point tile, float rotation) {
    }

    private static final Map<Short, Definition> DEFINITIONS = Map.of(
            (short) 1, new Definition((short) 76, PlacementPolicy.INTERIOR_CORNERS), // Treasury
            (short) 4, new Definition((short) 78, PlacementPolicy.INTERIOR_CORNERS), // Hatchery
            (short) 10, new Definition((short) 80, PlacementPolicy.EXPOSED_CORNERS), // Workshop
            (short) 12, new Definition((short) 82, PlacementPolicy.INTERIOR_CORNERS), // Torture
            (short) 13, new Definition((short) 111, PlacementPolicy.NONE), // Temple candlesticks
            (short) 14, new Definition((short) 84, PlacementPolicy.INTERIOR_CORNERS), // Graveyard
            (short) 15, new Definition((short) 85, PlacementPolicy.INTERIOR_CORNERS), // Casino
            (short) 16, new Definition((short) 79, PlacementPolicy.INTERIOR_CORNERS), // Pit
            (short) 26, new Definition((short) 141, PlacementPolicy.INTERIOR_CORNERS) // Crypt
    );

    private RoomPillars() {
    }

    /**
     * Gets the native decorative object for a room, including the Temple's
     * candlesticks. Lair and Prison deliberately have no active native pillar.
     */
    public static Short getObjectId(short roomId) {
        Definition definition = DEFINITIONS.get(roomId);
        return definition == null ? null : definition.objectId();
    }

    /**
     * Computes every pillar transform for a room instance. Workshop keeps its
     * native exposed-corner rule, which may place multiple corner pieces on
     * one tile; other pillar rooms require a full three-by-three interior.
     */
    public static List<Placement> getPlacements(RoomInstance room) {
        Definition definition = DEFINITIONS.get(room.getRoom().getRoomId());
        if (definition == null || definition.policy() == PlacementPolicy.NONE) {
            return List.of();
        }

        boolean[][] map = room.getCoordinatesAsMatrix();
        Point start = room.getMatrixStartPoint();
        List<Placement> placements = new ArrayList<>();
        for (Point p : room.getCoordinates()) {
            Set<WallDirection> free = freeDirections(map, p, start);
            if (definition.policy() == PlacementPolicy.EXPOSED_CORNERS) {
                addExposedCorners(placements, definition.objectId(), p, free);
            } else if (isCorner(free) && hasInteriorBlock(map, start, p, free)) {
                placements.add(new Placement(definition.objectId(), p, rotation(free)));
            }
        }
        return List.copyOf(placements);
    }

    /**
     * Gets the distinct tiles that contain a configured pillar. These tiles
     * affect cave clearance but do not alter the terrain deformation field.
     */
    public static Set<Point> getPillarTiles(RoomInstance room) {
        Set<Point> tiles = new HashSet<>();
        for (Placement placement : getPlacements(room)) {
            tiles.add(placement.tile());
        }
        return Set.copyOf(tiles);
    }

    private static Set<WallDirection> freeDirections(boolean[][] map, Point p, Point start) {
        Set<WallDirection> free = EnumSet.noneOf(WallDirection.class);
        int x = p.x - start.x;
        int y = p.y - start.y;
        if (!hasSameTile(map, x, y - 1)) {
            free.add(WallDirection.NORTH);
        }
        if (!hasSameTile(map, x, y + 1)) {
            free.add(WallDirection.SOUTH);
        }
        if (!hasSameTile(map, x + 1, y)) {
            free.add(WallDirection.EAST);
        }
        if (!hasSameTile(map, x - 1, y)) {
            free.add(WallDirection.WEST);
        }
        return free;
    }

    private static boolean isCorner(Set<WallDirection> free) {
        return free.size() == 2
                && !(free.contains(WallDirection.NORTH) && free.contains(WallDirection.SOUTH))
                && !(free.contains(WallDirection.WEST) && free.contains(WallDirection.EAST));
    }

    private static void addExposedCorners(List<Placement> placements, short objectId, Point p,
            Set<WallDirection> free) {
        if (free.contains(WallDirection.NORTH) && free.contains(WallDirection.EAST)) {
            placements.add(new Placement(objectId, p, -FastMath.HALF_PI));
        }
        if (free.contains(WallDirection.SOUTH) && free.contains(WallDirection.EAST)) {
            placements.add(new Placement(objectId, p, FastMath.PI));
        }
        if (free.contains(WallDirection.SOUTH) && free.contains(WallDirection.WEST)) {
            placements.add(new Placement(objectId, p, FastMath.HALF_PI));
        }
        if (free.contains(WallDirection.NORTH) && free.contains(WallDirection.WEST)) {
            placements.add(new Placement(objectId, p, 0));
        }
    }

    private static float rotation(Set<WallDirection> free) {
        if (free.contains(WallDirection.NORTH) && free.contains(WallDirection.EAST)) {
            return -FastMath.HALF_PI;
        } else if (free.contains(WallDirection.SOUTH) && free.contains(WallDirection.EAST)) {
            return FastMath.PI;
        } else if (free.contains(WallDirection.SOUTH) && free.contains(WallDirection.WEST)) {
            return FastMath.HALF_PI;
        }
        return 0;
    }

    private static boolean hasInteriorBlock(boolean[][] map, Point start, Point p,
            Set<WallDirection> free) {
        boolean goWest = free.contains(WallDirection.EAST);
        boolean goNorth = free.contains(WallDirection.SOUTH);
        for (int y = 0; y < 3; y++) {
            int yPoint = goNorth ? -y : y;
            for (int x = 0; x < 3; x++) {
                int xPoint = goWest ? -x : x;
                if (!hasSameTile(map, p.x - start.x + xPoint, p.y - start.y + yPoint)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasSameTile(boolean[][] map, int x, int y) {
        return x >= 0 && x < map.length && y >= 0 && y < map[x].length && map[x][y];
    }
}
