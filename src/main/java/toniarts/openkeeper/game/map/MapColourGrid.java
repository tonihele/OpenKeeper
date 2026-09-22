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

import java.util.Arrays;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * The per-tile colour-class cache the minimap renderers read
 */
public final class MapColourGrid {

    private final int width;
    private final int height;
    private final short[] classes;
    private final MapColourClassifier classifier;
    private final IFogOfWarInformation fogOfWarInformation;

    public MapColourGrid(int width, int height, MapColourClassifier classifier, IFogOfWarInformation fogOfWarInformation) {
        this.width = width;
        this.height = height;
        this.classifier = classifier;
        this.fogOfWarInformation = fogOfWarInformation;
        this.classes = new short[width * height];
        // Java default-initializes short[] to 0 (NEUTRAL_OWNED_SENTINEL, the
        // magenta bug tile) - fill with the "haven't looked here yet" class
        // instead, so the grid is safe to read even before the caller runs
        // the initial recomputeRect() at level load (design §3.1).
        Arrays.fill(classes, MapColourClass.UNEXPLORED_OR_IMPENETRABLE);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * The live array, index {@code y * width + x}. Read-only: nothing
     * outside this class may write to it.
     */
    public short[] colourClasses() {
        return classes;
    }

    /**
     * @return the stored class at (x, y), or
     * {@link MapColourClass#UNEXPLORED_OR_IMPENETRABLE} if out of bounds
     */
    public short get(int x, int y) {
        if (!inBounds(x, y)) {
            return MapColourClass.UNEXPLORED_OR_IMPENETRABLE;
        }
        return classes[index(x, y)];
    }

    /**
     * Pure - classify without storing. Callers that just want the value
     * (rather than refreshing the cache) should use this instead of
     * {@link #recompute}.
     */
    public short classify(int x, int y) {
        return classifier.classify(x, y);
    }

    /**
     * Recomputes and stores a single tile. A no-op out of bounds.
     */
    public void recompute(int x, int y) {
        if (!inBounds(x, y)) {
            return;
        }
        classes[index(x, y)] = classifier.classify(x, y);
    }

    /**
     * Recomputes and stores every tile in the rectangle, clamped to the map.
     * Used at level start and after a full "recompute everything" (zoom
     * reset, map right-click).
     */
    public void recomputeRect(int x, int y, int w, int h) {
        int startX = Math.max(x, 0);
        int startY = Math.max(y, 0);
        int endX = Math.min(x + w, width);
        int endY = Math.min(y + h, height);
        for (int ty = startY; ty < endY; ty++) {
            for (int tx = startX; tx < endX; tx++) {
                classes[index(tx, ty)] = classifier.classify(tx, ty);
            }
        }
    }

    /**
     * The drag-box highlight rule (design §3.4): turning the highlight on
     * only actually stores {@link MapColourClass#HIGHLIGHT} if the tile is
     * unexplored or taggable - reusing
     * {@link IFogOfWarInformation#isHighlightable} rather than
     * re-implementing that rule. Turning it off just recomputes the tile
     * normally.
     */
    public void setHighlight(int x, int y, boolean on) {
        if (!inBounds(x, y)) {
            return;
        }
        if (on) {
            if (fogOfWarInformation.isHighlightable(new Point(x, y))) {
                classes[index(x, y)] = MapColourClass.HIGHLIGHT;
            }
        } else {
            recompute(x, y);
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private int index(int x, int y) {
        return y * width + x;
    }

}
