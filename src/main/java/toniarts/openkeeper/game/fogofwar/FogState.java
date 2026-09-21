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

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The fog-of-war knowledge of a single viewer: which tiles have been
 * explored (sticky) and which have merely been perceived (sticky, only
 * matters for terrain that reveals through fog). This is local, per-viewer
 * view state - it is never part of the deterministic simulation.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class FogState {

    private final short viewerId;
    private final int width;
    private final int height;
    private boolean fogEnabled = true;
    private final BitSet explored;
    private final BitSet perceived;
    private final Set<Point> dirtyTiles = new HashSet<>();

    public FogState(short viewerId, int width, int height) {
        this.viewerId = viewerId;
        this.width = width;
        this.height = height;
        this.explored = new BitSet(width * height);
        this.perceived = new BitSet(width * height);
    }

    public short getViewerId() {
        return viewerId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private int index(int x, int y) {
        return y * width + x;
    }

    public boolean isExplored(int x, int y) {
        return inBounds(x, y) && explored.get(index(x, y));
    }

    public boolean isPerceived(int x, int y) {
        return inBounds(x, y) && perceived.get(index(x, y));
    }

    /**
     * @return {@code true} if this call actually flipped the bit
     */
    boolean setExplored(int x, int y, boolean value) {
        if (!inBounds(x, y)) {
            return false;
        }
        int i = index(x, y);
        if (explored.get(i) == value) {
            return false;
        }
        explored.set(i, value);
        dirtyTiles.add(new Point(x, y));
        return true;
    }

    /**
     * @return {@code true} if this call actually flipped the bit
     */
    boolean setPerceived(int x, int y, boolean value) {
        if (!inBounds(x, y)) {
            return false;
        }
        int i = index(x, y);
        if (perceived.get(i) == value) {
            return false;
        }
        perceived.set(i, value);
        dirtyTiles.add(new Point(x, y));
        return true;
    }

    public boolean isFogEnabled() {
        return fogEnabled;
    }

    public void setFogEnabled(boolean fogEnabled) {
        this.fogEnabled = fogEnabled;
    }

    public void clearAll() {
        markAllSetBitsDirty(explored);
        markAllSetBitsDirty(perceived);
        explored.clear();
        perceived.clear();
    }

    public void revealAll() {
        for (int i = 0; i < width * height; i++) {
            if (!explored.get(i)) {
                dirtyTiles.add(new Point(i % width, i / width));
            }
        }
        explored.set(0, width * height);
    }

    private void markAllSetBitsDirty(BitSet bits) {
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            dirtyTiles.add(new Point(i % width, i / width));
        }
    }

    /**
     * Peek at the tiles that changed since the last {@link #drainDirtyTiles()},
     * without clearing them. Useful for logic that needs to react within the
     * same pass that produced the changes (e.g. "enemy sighted" detection).
     */
    public Set<Point> peekDirtyTiles() {
        return Collections.unmodifiableSet(dirtyTiles);
    }

    /**
     * @return the tiles that changed since the last call, coalesced; empties the set
     */
    public Set<Point> drainDirtyTiles() {
        if (dirtyTiles.isEmpty()) {
            return Set.of();
        }
        Set<Point> result = new HashSet<>(dirtyTiles);
        dirtyTiles.clear();
        return result;
    }

}
