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

/**
 * The transient reveal window of the Sight of Evil keeper spell. The tiles in
 * a square around the cast tile are revealed while they are inside a disc
 * that grows from nothing to 5 tiles, stays at full size for a moment and
 * shrinks away again. Nothing is explored for good: when the spell ends the
 * area is back under the fog. Pure rules, no engine dependencies.
 */
public final class SightOfEvilReveal {

    /**
     * Largest radius of the revealed square, in tiles
     */
    public static final int MAX_RADIUS = 7;

    /**
     * Full radius of the revealed disc, in 1/256 tiles
     */
    private static final int DISC_RADIUS = 5 * 256;

    /**
     * The disc edge fades out over this, in 1/256 tiles. A tile is revealed
     * while it is not fully faded.
     */
    private static final int FADE = 2;

    /**
     * Game ticks the disc spends at its full size
     */
    private static final int FULL_SIZE_TICKS = 16;

    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
    private final int durationTicks;

    /**
     * A new reveal window
     *
     * @param x the cast tile x
     * @param y the cast tile y
     * @param radius the radius of the revealed square, in tiles (capped at
     * {@link #MAX_RADIUS})
     * @param durationTicks how long the window lasts, in game ticks
     * @param mapWidth the map width
     * @param mapHeight the map height
     */
    public SightOfEvilReveal(int x, int y, int radius, int durationTicks, int mapWidth, int mapHeight) {
        int r = Math.min(radius, MAX_RADIUS);
        this.minX = clamp(x - r, mapWidth);
        this.minY = clamp(y - r, mapHeight);
        this.maxX = clamp(x + r, mapWidth);
        this.maxY = clamp(y + r, mapHeight);
        this.durationTicks = durationTicks;
    }

    private static int clamp(int value, int size) {
        return Math.max(0, Math.min(value, size));
    }

    /**
     * Is the tile revealed at the given moment
     *
     * @param x tile x
     * @param y tile y
     * @param elapsedTicks game ticks since the spell was cast
     * @return {@code true} if the tile is revealed
     */
    public boolean isRevealed(int x, int y, double elapsedTicks) {
        if (x < minX || x >= maxX || y < minY || y >= maxY || elapsedTicks < 0 || isOver(elapsedTicks)) {
            return false;
        }

        double discRadius = getDiscRadius(elapsedTicks);
        if (discRadius <= 0) {
            return false;
        }

        // Measured from the middle of the (possibly clipped) square
        int dx = x - (minX + maxX) / 2;
        int dy = y - (minY + maxY) / 2;
        double distance = Math.floor(Math.sqrt(dx * dx + dy * dy) * 256);
        return distance < discRadius - FADE;
    }

    /**
     * The disc radius grows over the first part of the spell, stays at full
     * size for {@link #FULL_SIZE_TICKS} and shrinks away over the last part
     */
    private double getDiscRadius(double elapsedTicks) {
        int growTicks = (durationTicks - FULL_SIZE_TICKS) / 2;
        if (growTicks <= 0) {
            return DISC_RADIUS;
        }

        double ticks = elapsedTicks;
        if (ticks > durationTicks / 2) {
            ticks = durationTicks - ticks;
        }
        ticks = Math.min(ticks, growTicks);
        return ticks * DISC_RADIUS / growTicks;
    }

    /**
     * Has the spell ended
     *
     * @param elapsedTicks game ticks since the spell was cast
     * @return {@code true} if nothing is revealed anymore
     */
    public boolean isOver(double elapsedTicks) {
        return elapsedTicks >= durationTicks;
    }

    /**
     * @return the smallest x of the revealed square, inclusive
     */
    public int getMinX() {
        return minX;
    }

    /**
     * @return the smallest y of the revealed square, inclusive
     */
    public int getMinY() {
        return minY;
    }

    /**
     * @return the largest x of the revealed square, exclusive
     */
    public int getMaxX() {
        return maxX;
    }

    /**
     * @return the largest y of the revealed square, exclusive
     */
    public int getMaxY() {
        return maxY;
    }

}
