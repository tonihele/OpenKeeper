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
package toniarts.openkeeper.game.controller.creature;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.utils.Point;

/**
 * Picks where a creature runs to when it flees from a threat, following
 * DKII.EXE's run away (RunAway at 0x4764A0 and 0x476390).
 *
 * @see #findAwayFrom(Point, Point, int)
 */
public final class FleeDestinationFinder {

    /**
     * Tells if a creature can walk to a tile
     */
    @FunctionalInterface
    public interface TileCheck {

        /**
         * Checks a tile that is inside the map
         *
         * @param x the tile x
         * @param y the tile y
         * @return true if the creature can walk to the tile
         */
        boolean isReachable(int x, int y);
    }

    /**
     * Width and height of the area, in tiles, a destination is picked from
     */
    private static final int AREA_SIZE = 5;

    /**
     * How many random tiles the fallbacks try before giving up
     */
    private static final int AROUND_TRIES = 10;
    private static final int AWAY_TRIES = 25;

    private final int mapWidth;
    private final int mapHeight;
    private final TileCheck tileCheck;
    private final RandomGenerator random;

    /**
     * Creates a finder
     *
     * @param mapWidth map width in tiles
     * @param mapHeight map height in tiles
     * @param tileCheck tells the tiles the creature can walk to
     * @param random the random source
     */
    public FleeDestinationFinder(int mapWidth, int mapHeight, TileCheck tileCheck, RandomGenerator random) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.tileCheck = tileCheck;
        this.random = random;
    }

    /**
     * Picks a tile away from the threat. The point on the line from the
     * threat through the creature, the given distance from the threat, is the
     * centre of a 5x5 area. A random reachable tile of the area, other than the
     * creature's own, is the destination. If the area has none, the creature runs from the area's far
     * corner instead, a random number of tiles on each axis.
     *
     * @param creature the creature's tile
     * @param threat the threat's tile
     * @param distance how far from the threat, in tiles, to aim
     * @return the destination, empty if nothing was found
     */
    public Optional<Point> findAwayFrom(Point creature, Point threat, int distance) {
        int dx = creature.x - threat.x;
        int dy = creature.y - threat.y;
        double length = Math.hypot(dx, dy);
        if (length == 0) {
            length = 1;
        }
        int minX = clamp(threat.x + (int) Math.round(dx / length * distance) - AREA_SIZE / 2, mapWidth);
        int minY = clamp(threat.y + (int) Math.round(dy / length * distance) - AREA_SIZE / 2, mapHeight);
        int maxX = clamp(minX + AREA_SIZE - 1, mapWidth);
        int maxY = clamp(minY + AREA_SIZE - 1, mapHeight);

        List<Point> candidates = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (tileCheck.isReachable(x, y) && !(x == creature.x && y == creature.y)) {
                    candidates.add(new Point(x, y));
                }
            }
        }
        if (!candidates.isEmpty()) {
            return Optional.of(candidates.get(random.nextInt(candidates.size())));
        }
        return findAwayFromCorner(creature, maxX, maxY);
    }

    private Optional<Point> findAwayFromCorner(Point creature, int cornerX, int cornerY) {
        int dx = nonZero(cornerX - creature.x);
        int dy = nonZero(cornerY - creature.y);
        for (int i = 0; i < AWAY_TRIES; i++) {
            int x = creature.x - Integer.signum(dx) * random.nextInt(Math.abs(dx) * 4);
            int y = creature.y - Integer.signum(dy) * random.nextInt(Math.abs(dy) * 4);
            if (isReachable(x, y)) {
                return Optional.of(new Point(x, y));
            }
        }
        return Optional.empty();
    }

    private int nonZero(int value) {
        return value != 0 ? value : (random.nextBoolean() ? 1 : -1);
    }

    private static int clamp(int value, int size) {
        return Math.max(0, Math.min(value, size - 1));
    }

    private boolean isReachable(int x, int y) {
        return x >= 0 && y >= 0 && x < mapWidth && y < mapHeight && tileCheck.isReachable(x, y);
    }

    /**
     * Picks a random reachable tile within two tiles of the creature, in a
     * 5x5 area, trying ten times
     *
     * @param creature the creature's tile
     * @return the destination, empty if nothing was found
     */
    public Optional<Point> findAround(Point creature) {
        for (int i = 0; i < AROUND_TRIES; i++) {
            int x = creature.x + random.nextInt(AREA_SIZE) - AREA_SIZE / 2;
            int y = creature.y + random.nextInt(AREA_SIZE) - AREA_SIZE / 2;
            if (isReachable(x, y)) {
                return Optional.of(new Point(x, y));
            }
        }
        return Optional.empty();
    }
}
