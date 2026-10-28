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
package toniarts.openkeeper.game.fogofwar;

import toniarts.openkeeper.utils.Point;

import java.util.Arrays;
import java.util.Collection;

/**
 * The primitive fog-of-war mutations and the pure algorithms built on top of
 * them (line-of-sight ring-cast, perception disc). Everything here is
 * idempotent and free of engine/ECS dependencies, per the fog-of-war design
 * document's §5/§6.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class FogOfWarRules {

    private FogOfWarRules() {
    }

    public static boolean explore(FogState state, int x, int y) {
        return state.setExplored(x, y, true);
    }

    public static boolean unexplore(FogState state, int x, int y) {
        return state.setExplored(x, y, false);
    }

    public static boolean perceive(FogState state, int x, int y) {
        return state.setPerceived(x, y, true);
    }

    public static boolean unperceive(FogState state, int x, int y) {
        return state.setPerceived(x, y, false);
    }

    public static void exploreRect(FogState state, Point start, Point end) {
        forEachInRect(start, end, (x, y) -> explore(state, x, y));
    }

    public static void unexploreRect(FogState state, Point start, Point end) {
        forEachInRect(start, end, (x, y) -> unexplore(state, x, y));
    }

    private interface TileVisitor {

        void visit(int x, int y);
    }

    private static void forEachInRect(Point start, Point end, TileVisitor visitor) {
        int minX = Math.min(start.x, end.x);
        int maxX = Math.max(start.x, end.x);
        int minY = Math.min(start.y, end.y);
        int maxY = Math.max(start.y, end.y);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                visitor.visit(x, y);
            }
        }
    }

    public static void exploreTiles(FogState state, Collection<Point> tiles) {
        for (Point p : tiles) {
            explore(state, p.x, p.y);
        }
    }

    public static void unexploreTiles(FogState state, Collection<Point> tiles) {
        for (Point p : tiles) {
            unexplore(state, p.x, p.y);
        }
    }

    /**
     * A creature's explore radius: {@code distanceCanSee} rounded to whole
     * tiles and clamped to 2..7, per §6.3.
     */
    public static int clampVisionRadius(float distanceCanSeeTiles) {
        int r = Math.round(distanceCanSeeTiles);
        return Math.max(2, Math.min(7, r));
    }

    /**
     * Explores the tiles visible from (cx, cy) out to {@code radius} tiles,
     * per the ring-cast algorithm in §6.3. The centre tile is always
     * explored; sight-blocking cells are explored themselves (you see the
     * wall face) but close off the angles behind them.
     */
    public static void exploreVisionRing(FogState state, IBlocksSight blocksSight, LineOfSightTable losTable,
            int cx, int cy, int radius) {
        explore(state, cx, cy);

        boolean[] openAngle = new boolean[256];
        Arrays.fill(openAngle, true);

        long radiusSq = (long) radius * radius;
        for (LineOfSightTable.Offset o : losTable.getOffsets()) {
            long distSq = (long) o.dx * o.dx + (long) o.dy * o.dy;
            if (distSq > radiusSq) {
                continue;
            }
            if (!anyOpen(openAngle, o.angleStart, o.angleEnd)) {
                continue;
            }

            int x = cx + o.dx;
            int y = cy + o.dy;
            explore(state, x, y);

            if (blocksSight.blocksSight(x, y)) {
                closeRange(openAngle, o.angleStart, o.angleEnd);
            }
        }
    }

    private static boolean anyOpen(boolean[] openAngle, int start, int end) {
        if (start <= end) {
            for (int a = start; a <= end; a++) {
                if (openAngle[a]) {
                    return true;
                }
            }
        } else {
            for (int a = start; a < 256; a++) {
                if (openAngle[a]) {
                    return true;
                }
            }
            for (int a = 0; a <= end; a++) {
                if (openAngle[a]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void closeRange(boolean[] openAngle, int start, int end) {
        if (start <= end) {
            for (int a = start; a <= end; a++) {
                openAngle[a] = false;
            }
        } else {
            for (int a = start; a < 256; a++) {
                openAngle[a] = false;
            }
            for (int a = 0; a <= end; a++) {
                openAngle[a] = false;
            }
        }
    }

    /**
     * Perceives every tile whose centre lies strictly within {@code range}
     * tiles of (cx, cy), Euclidean distance, ignoring line of sight (§6.3).
     */
    public static void perceiveDisc(FogState state, int cx, int cy, float range) {
        int r = (int) Math.ceil(range);
        double rangeSq = (double) range * range;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                double distSq = (double) dx * dx + (double) dy * dy;
                if (distSq < rangeSq) {
                    perceive(state, cx + dx, cy + dy);
                }
            }
        }
    }

    /**
     * Explores every tile whose centre lies within {@code radius} tiles of
     * (cx, cy), Euclidean distance, ignoring line of sight. Used for the
     * dungeon-heart start-of-level "unfolding" reveal (§6.6).
     */
    public static void exploreDisc(FogState state, int cx, int cy, float radius) {
        int r = (int) Math.ceil(radius);
        double radiusSq = (double) radius * radius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                double distSq = (double) dx * dx + (double) dy * dy;
                if (distSq <= radiusSq) {
                    explore(state, cx + dx, cy + dy);
                }
            }
        }
    }

    /**
     * The rendering visibility rule (§8.1), minus the transient reveal
     * window and camera-mode short-circuit, which are folded in by the
     * caller.
     */
    public static boolean isVisible(FogState state, int x, int y, boolean revealThroughFogTerrain) {
        if (state.isExplored(x, y)) {
            return true;
        }
        return revealThroughFogTerrain && state.isPerceived(x, y);
    }

}
