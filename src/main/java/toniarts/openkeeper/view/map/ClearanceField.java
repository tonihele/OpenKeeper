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

/**
 * A per-tile field of how far a cave ceiling may rise above a tile, in a
 * {@code 0..15} unit anchored on the tile's own upper-left grid corner. Both
 * solid and open tiles carry a value, each measured to the nearest tile of
 * the <i>opposite</i> solidity, so the field is one continuous "distance to
 * the nearest solidity boundary" surface rather than two separate ones - it
 * has no discontinuity at a wall face.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ClearanceField {

    public static final int MAX_CLEARANCE = 15;

    /**
     * Distance is measured in a 9x9 window (4 tiles in every direction) and
     * capped there; beyond that a tile just reads as fully clear/enclosed,
     * i.e. saturated at {@link #MAX_CLEARANCE}.
     */
    private static final int SEARCH_RADIUS = 4;
    private static final int SATURATION_D2 = SEARCH_RADIUS * SEARCH_RADIUS;

    private final int width;
    private final int height;
    private final byte[] values;

    public ClearanceField(int width, int height) {
        this.width = width;
        this.height = height;
        this.values = new byte[width * height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * @param x tile x, may be outside the grid
     * @param y tile y, may be outside the grid
     * @return the clearance at {@code (x, y)}, or {@code 0} outside the map
     */
    public int at(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return values[y * width + x] & 0xFF;
    }

    /**
     * Recomputes the whole field from the given solidity predicate.
     *
     * @param solidity queried for every tile inside the map bounds
     */
    public void rebuild(Solidity solidity) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                values[y * width + x] = (byte) computeClearance(x, y, solidity);
            }
        }
    }

    private int computeClearance(int x, int y, Solidity solidity) {
        boolean solid = solidity.isSolid(x, y);
        int bestD2 = SATURATION_D2;
        for (int oy = -SEARCH_RADIUS; oy <= SEARCH_RADIUS; oy++) {
            int ty = y + oy;
            if (ty < 0 || ty >= height) {
                continue;
            }
            // The +1 on a negative delta re-anchors it to the tile's
            // upper-left corner rather than its centre - the four tiles
            // meeting at that corner are all at distance 0.
            int dy = (oy < 0) ? oy + 1 : oy;
            int dy2 = dy * dy;
            if (dy2 >= bestD2) {
                continue;
            }
            for (int ox = -SEARCH_RADIUS; ox <= SEARCH_RADIUS; ox++) {
                int tx = x + ox;
                if (tx < 0 || tx >= width) {
                    continue;
                }
                if (solidity.isSolid(tx, ty) == solid) {
                    continue;
                }
                int dx = (ox < 0) ? ox + 1 : ox;
                int d2 = dx * dx + dy2;
                if (d2 < bestD2) {
                    bestD2 = d2;
                }
            }
        }
        return Math.min(MAX_CLEARANCE, Math.round(4f * (float) Math.sqrt(bestD2)));
    }

    @FunctionalInterface
    public interface Solidity {

        /**
         * @param x tile x, always inside the map bounds
         * @param y tile y, always inside the map bounds
         * @return is the tile solid
         */
        boolean isSolid(int x, int y);
    }
}
