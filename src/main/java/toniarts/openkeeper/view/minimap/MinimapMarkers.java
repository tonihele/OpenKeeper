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
package toniarts.openkeeper.view.minimap;

/**
 * The marker-overlay drawing primitives and zoomed-mode pixel mapping
 * Pure int/float math over a B,G,R raster buffer,
 * no jME dependency - headlessly testable, matching
 * {@link MinimapRasteriser}. (Fit-mode pixel mapping is already covered by
 * {@link MinimapRasteriser.FitGeometry}; only zoomed mode needs a new
 * helper here.)
 */
public final class MinimapMarkers {

    private static final int RASTER_SIZE = MinimapRasteriser.RASTER_SIZE;

    private MinimapMarkers() {
    }

    /**
     * design §5.8's zoomed {@code toPixel}: the camera look-at tile is
     * always the image centre (64,64).
     */
    public static float zoomedPixelX(float tileX, float cameraTileX, int pixelsPerTile) {
        return RASTER_SIZE / 2f + (tileX - cameraTileX) * pixelsPerTile;
    }

    public static float zoomedPixelY(float tileY, float cameraTileY, int pixelsPerTile) {
        return RASTER_SIZE / 2f + (tileY - cameraTileY) * pixelsPerTile;
    }

    /**
     * The inverse of {@link #zoomedPixelX}/{@link #zoomedPixelY} - for
     * click resolution (design §5.11).
     */
    public static float zoomedTileX(float pixelX, float cameraTileX, int pixelsPerTile) {
        return (pixelX - RASTER_SIZE / 2f) / pixelsPerTile + cameraTileX;
    }

    public static float zoomedTileY(float pixelY, float cameraTileY, int pixelsPerTile) {
        return (pixelY - RASTER_SIZE / 2f) / pixelsPerTile + cameraTileY;
    }

    /**
     * {@code blink(a, b)}: {@code a} on the "odd" half of the
     * blink cycle, {@code b} on the "even" half - driven by a local
     * fixed-interval accumulator's parity bit, not a real synced game tick
     * since no such tick is exposed client-side.
     */
    public static int blink(int colourA, int colourB, boolean oddParity) {
        return oddParity ? colourA : colourB;
    }

    public static void dot(byte[] raster, int cx, int cy, int argbColour) {
        disc(raster, cx, cy, 1, argbColour);
    }

    public static void disc(byte[] raster, int cx, int cy, int radius, int argbColour) {
        int r2 = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= r2) {
                    setPixel(raster, cx + dx, cy + dy, argbColour);
                }
            }
        }
    }

    /**
     * A ring - the boundary of a disc of the given radius, not its
     * interior.
     */
    public static void circleOutline(byte[] raster, int cx, int cy, int radius, int argbColour) {
        int outer2 = radius * radius;
        int inner2 = (radius - 1) * (radius - 1);
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 <= outer2 && d2 > inner2) {
                    setPixel(raster, cx + dx, cy + dy, argbColour);
                }
            }
        }
    }

    public static void line(byte[] raster, int x0, int y0, int x1, int y1, int argbColour) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int x = x0;
        int y = y0;
        while (true) {
            setPixel(raster, x, y, argbColour);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /**
     * A dashed line: pixels along the line are grouped into runs of
     * {@code dashLength}, alternating drawn/skipped. {@code phaseOffset}
     * shifts where that alternation starts, so calling this with an
     * incrementing {@code phaseOffset} each tick animates the dashes
     * marching along the line.
     */
    public static void dottedLine(byte[] raster, int x0, int y0, int x1, int y1, int argbColour,
            int dashLength, int phaseOffset) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int x = x0;
        int y = y0;
        int step = 0;
        while (true) {
            boolean on = Math.floorMod((step + phaseOffset) / dashLength, 2) == 0;
            if (on) {
                setPixel(raster, x, y, argbColour);
            }
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y += sy;
            }
            step++;
        }
    }

    public static void rectOutline(byte[] raster, int x0, int y0, int x1, int y1, int argbColour) {
        line(raster, x0, y0, x1, y0, argbColour);
        line(raster, x1, y0, x1, y1, argbColour);
        line(raster, x1, y1, x0, y1, argbColour);
        line(raster, x0, y1, x0, y0, argbColour);
    }

    /**
     * A small bar. The exact glyph shape is unconfirmed against the
     * original (design §9 item 2) - this is a best guess.
     */
    public static void doorGlyphH(byte[] raster, int cx, int cy, int argbColour) {
        line(raster, cx - 2, cy, cx + 2, cy, argbColour);
    }

    public static void doorGlyphV(byte[] raster, int cx, int cy, int argbColour) {
        line(raster, cx, cy - 2, cx, cy + 2, argbColour);
    }

    /**
     * the point where a ray from {@code (cx,cy)} in direction
     * {@code (dx,dy)} leaves the {@code [0,size]} square - for the
     * heart-direction line, clipped to the raster edge.
     */
    public static float[] clipToEdge(float cx, float cy, float dx, float dy, int size) {
        float t = Float.MAX_VALUE;
        if (dx > 0) {
            t = Math.min(t, (size - cx) / dx);
        } else if (dx < 0) {
            t = Math.min(t, -cx / dx);
        }
        if (dy > 0) {
            t = Math.min(t, (size - cy) / dy);
        } else if (dy < 0) {
            t = Math.min(t, -cy / dy);
        }
        return new float[]{cx + t * dx, cy + t * dy};
    }

    private static void setPixel(byte[] raster, int x, int y, int argbColour) {
        if (x < 0 || y < 0 || x >= RASTER_SIZE || y >= RASTER_SIZE) {
            return;
        }
        int i = (y * RASTER_SIZE + x) * 3;
        raster[i] = (byte) argbColour;
        raster[i + 1] = (byte) (argbColour >> 8);
        raster[i + 2] = (byte) (argbColour >> 16);
    }

}
