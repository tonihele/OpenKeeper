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

/**
 * How far a cave ceiling may rise above any point of the map, in a
 * {@code 0..MAX_CLEARANCE} unit: four times the distance from that point to
 * the nearest solid tile's face, capped four tiles out.
 * <p>
 * This is queried at a continuous world position rather than precomputed per
 * tile, so a gap as narrow as one tile still domes in the middle. Anchoring
 * the value only at tile corners and interpolating between them can't do
 * that: in a 1-wide corridor, both wall-adjacent corners of the corridor
 * tile are themselves zero (each wall face is distance zero from the corner
 * that touches it), and a bilinear blend of {@code 0, 0, 0, 0} is flat no
 * matter where in the tile you sample it - the corners simply never see the
 * half-tile of clearance that exists exactly between them.
 */
public final class ClearanceField {

    public static final float MAX_CLEARANCE = 15f;

    /**
     * Distance is capped four tiles out - beyond that a point just reads as
     * fully clear, i.e. saturated at {@link #MAX_CLEARANCE}.
     */
    private static final float SEARCH_LIMIT = 4f;

    /**
     * How many tiles out to look for a candidate solid tile. Needs to cover
     * {@link #SEARCH_LIMIT} plus how far a query point can land from its own
     * tile's centre (up to a tile, for the normal samples in
     * {@link Ceiling}) plus the tile's own half-width.
     */
    private static final int SEARCH_RADIUS = 5;

    private final int width;
    private final int height;
    private final boolean[] solid;

    /**
     * @param width map width, in tiles
     * @param height map height, in tiles
     * @param solid this map's solidity, one entry per tile, row-major
     * ({@code y * width + x}) - copied defensively
     */
    public ClearanceField(int width, int height, boolean[] solid) {
        this.width = width;
        this.height = height;
        this.solid = solid.clone();
    }

    /**
     * @param worldX world x, in tiles
     * @param worldY world y (map z), in tiles
     * @return the clearance at that point
     */
    public float clearanceAt(float worldX, float worldY) {
        int baseX = Math.round(worldX);
        int baseY = Math.round(worldY);
        float bestDistance = SEARCH_LIMIT;

        for (int oy = -SEARCH_RADIUS; oy <= SEARCH_RADIUS; oy++) {
            int ty = baseY + oy;
            if (ty < 0 || ty >= height) {
                continue;
            }
            for (int ox = -SEARCH_RADIUS; ox <= SEARCH_RADIUS; ox++) {
                int tx = baseX + ox;
                if (tx < 0 || tx >= width || !solid[ty * width + tx]) {
                    continue;
                }

                // Clamped point-to-box distance: 0 once the point is over
                // the tile's own footprint, otherwise the distance to its
                // nearest face
                float dx = Math.max(0f, Math.abs(worldX - tx) - 0.5f);
                float dy = Math.max(0f, Math.abs(worldY - ty) - 0.5f);
                float distance = FastMath.sqrt(dx * dx + dy * dy);
                if (distance < bestDistance) {
                    bestDistance = distance;
                }
            }
        }

        return Math.min(MAX_CLEARANCE, 4f * bestDistance);
    }
}
