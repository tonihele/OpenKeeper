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

import toniarts.openkeeper.game.map.MapColourClass;
import toniarts.openkeeper.game.map.MapColourGrid;

/**
 * Fills the 128x128 B,G,R raster from a {@link MapColourGrid} and
 * {@link MinimapPalette} (minimap_design.md §5.3). Pure Java/int/float
 * math, no jME dependency - headlessly testable - matching how
 * {@link toniarts.openkeeper.game.map.MapColourClassifier} was kept
 * separate from rendering.
 *
 * <p>
 * Fit mode ({@code zoom == -1}, design §5.5) and zoomed mode
 * ({@code zoom} 0..4, design §5.4/§5.6) are both implemented. UVs stay
 * identity for both (no camera-yaw rotation) - that's Step 6's job.
 */
public final class MinimapRasteriser {

    public static final int RASTER_SIZE = 128;

    private static final int FIT_AXIS_PIXELS = 90;
    private static final int FIT_AXIS_OFFSET = 20;

    private MinimapRasteriser() {
    }

    /**
     * @param grid the live colour-class grid to read
     * @param palette the palette to read colours from; {@link
     * MinimapPalette#applyNeutralGuard} is called on it as the first step,
     * per design §5.3
     * @param rockTextureBgr 128x128x3 B,G,R bytes (see {@link
     * MinimapAssets#rockTextureBgr})
     * @param neutralPlayerNumber {@code playerNumber(neutralPlayerId)}
     * @param outBgr 128x128x3 B,G,R bytes, overwritten in place; must
     * already be allocated by the caller (so it can be reused/reshared with
     * a texture's backing buffer without reallocating every rebuild)
     */
    public static void rebuildFitMode(MapColourGrid grid, MinimapPalette palette, byte[] rockTextureBgr,
            short neutralPlayerNumber, byte[] outBgr) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        palette.applyNeutralGuard(neutralPlayerNumber);

        // Whole raster starts as rock. Class-1 (unexplored/impenetrable)
        // tiles are meant to show rock anyway (design §5.3 step 4), so
        // leaving this untouched for those pixels is correct, not just a
        // placeholder - this also covers "outside the map rectangle is
        // rock" for free: fit mode's border bands (design §5.6) always use
        // texX=texY=0, which for a 128x128 raster is exactly this copy.
        System.arraycopy(rockTextureBgr, 0, outBgr, 0, outBgr.length);

        // Fit-mode geometry (design §5.5). Camera position is ignored.
        FitGeometry fit = FitGeometry.of(width, height);
        int originX = fit.originX;
        int endX = fit.endX;
        int originY = fit.originY;
        int endY = fit.endY;

        int[] argb = palette.rawArgb();
        for (int py = Math.max(originY, 0); py < Math.min(endY, RASTER_SIZE); py++) {
            int ty = (py - originY) * fit.big / FIT_AXIS_PIXELS;
            if (ty < 0 || ty >= height) {
                continue;
            }
            for (int px = Math.max(originX, 0); px < Math.min(endX, RASTER_SIZE); px++) {
                int tx = (px - originX) * fit.big / FIT_AXIS_PIXELS;
                if (tx < 0 || tx >= width) {
                    continue;
                }
                short cls = grid.get(tx, ty);
                if (cls == MapColourClass.UNEXPLORED_OR_IMPENETRABLE) {
                    continue; // already rock from the base fill
                }
                writeColour(outBgr, px, py, argb[cls]);
            }
        }
    }

    /**
     * @param grid the live colour-class grid to read
     * @param palette the palette to read colours from
     * @param rockTextureBgr 128x128x3 B,G,R bytes
     * @param neutralPlayerNumber {@code playerNumber(neutralPlayerId)}
     * @param cameraTileX, cameraTileY the camera's look-at point, in
     * fractional tile-space coordinates (design §5.4's sub-tile camera
     * position, translated to float tile units per minimap_jmonkey.md
     * Step 1)
     * @param zoom 0 (1px/tile) .. 4 (16px/tile)
     * @param outBgr 128x128x3 B,G,R bytes, overwritten in place
     */
    public static void rebuildZoomedMode(MapColourGrid grid, MinimapPalette palette, byte[] rockTextureBgr,
            short neutralPlayerNumber, float cameraTileX, float cameraTileY, int zoom, byte[] outBgr) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        palette.applyNeutralGuard(neutralPlayerNumber);

        // Zoomed geometry (design §5.4). The camera look-at point is always
        // the image centre (64,64). The original computes this in 16.16
        // fixed point over sub-tile camera coordinates; translated to plain
        // float tile-unit arithmetic per minimap_jmonkey.md Step 1 - same
        // origin/step/clamped-range logic, including the original's own
        // integer-pixel snapping (origins are floored to int before the
        // per-pixel tile lookup, exactly as design's own originX>>16 does),
        // not smooth sub-pixel scrolling.
        int pixelsPerTile = 1 << zoom;
        float halfRaster = RASTER_SIZE / 2f;
        float originXf = halfRaster - cameraTileX * pixelsPerTile;
        float originYf = halfRaster - cameraTileY * pixelsPerTile;
        int originX = (int) Math.floor(originXf);
        int originY = (int) Math.floor(originYf);
        int endX = (int) Math.floor(originXf + width * pixelsPerTile);
        int endY = (int) Math.floor(originYf + height * pixelsPerTile);
        int mapPixelHeight = endY - originY;

        // Rock border bands (design §5.6): texX/texY are the negative
        // origins here, so the rock texture scrolls with the map.
        fillRock(outBgr, rockTextureBgr, 0, 0, RASTER_SIZE, originY, -originX, -originY);
        fillRock(outBgr, rockTextureBgr, 0, originY, originX, mapPixelHeight, -originX, -originY);
        fillRock(outBgr, rockTextureBgr, endX, originY, RASTER_SIZE - endX, mapPixelHeight, -originX, -originY);
        fillRock(outBgr, rockTextureBgr, 0, endY, RASTER_SIZE, RASTER_SIZE - endY, -originX, -originY);

        int[] argb = palette.rawArgb();
        float tilesPerPixel = 1f / pixelsPerTile;
        for (int py = Math.max(originY, 0); py < Math.min(endY, RASTER_SIZE); py++) {
            int ty = (int) Math.floor((py - originY) * tilesPerPixel);
            if (ty < 0 || ty >= height) {
                continue;
            }
            for (int px = Math.max(originX, 0); px < Math.min(endX, RASTER_SIZE); px++) {
                int tx = (int) Math.floor((px - originX) * tilesPerPixel);
                if (tx < 0 || tx >= width) {
                    continue;
                }
                short cls = grid.get(tx, ty);
                if (cls == MapColourClass.UNEXPLORED_OR_IMPENETRABLE) {
                    // Unlike fit mode, the border bands above don't cover the
                    // inside of the map rectangle, so class-1 tiles here need
                    // an explicit (non-scrolling: design §5.3 step 4 samples
                    // rock at the raw pixel coordinate, not the scrolling
                    // texX/texY the border bands use) rock write.
                    int rockIndex = ((py & (RASTER_SIZE - 1)) * RASTER_SIZE + (px & (RASTER_SIZE - 1))) * 3;
                    int outIndex = (py * RASTER_SIZE + px) * 3;
                    outBgr[outIndex] = rockTextureBgr[rockIndex];
                    outBgr[outIndex + 1] = rockTextureBgr[rockIndex + 1];
                    outBgr[outIndex + 2] = rockTextureBgr[rockIndex + 2];
                    continue;
                }
                writeColour(outBgr, px, py, argb[cls]);
            }
        }
    }

    /**
     * design §5.6: clip to the raster, then for each pixel copy
     * {@code rock[(y+texY) & 127][(x+texX) & 127]}.
     */
    private static void fillRock(byte[] outBgr, byte[] rockBgr, int x, int y, int w, int h, int texX, int texY) {
        int startX = Math.max(x, 0);
        int startY = Math.max(y, 0);
        int endX = Math.min(x + w, RASTER_SIZE);
        int endY = Math.min(y + h, RASTER_SIZE);
        for (int py = startY; py < endY; py++) {
            int rockY = Math.floorMod(py + texY, RASTER_SIZE);
            for (int px = startX; px < endX; px++) {
                int rockX = Math.floorMod(px + texX, RASTER_SIZE);
                int outIndex = (py * RASTER_SIZE + px) * 3;
                int rockIndex = (rockY * RASTER_SIZE + rockX) * 3;
                outBgr[outIndex] = rockBgr[rockIndex];
                outBgr[outIndex + 1] = rockBgr[rockIndex + 1];
                outBgr[outIndex + 2] = rockBgr[rockIndex + 2];
            }
        }
    }

    private static void writeColour(byte[] outBgr, int px, int py, int argbColour) {
        int i = (py * RASTER_SIZE + px) * 3;
        outBgr[i] = (byte) argbColour; // blue
        outBgr[i + 1] = (byte) (argbColour >> 8); // green
        outBgr[i + 2] = (byte) (argbColour >> 16); // red
    }

    /**
     * Fit-mode's origin/step geometry (design §5.5), factored out so the
     * frustum overlay (design §5.7, fit mode only) can place things in
     * exactly the same pixel space the raster itself uses, rather than
     * risking the two computations drifting apart.
     */
    static final class FitGeometry {

        final int big;
        final int originX;
        final int endX;
        final int originY;
        final int endY;

        private FitGeometry(int big, int originX, int endX, int originY, int endY) {
            this.big = big;
            this.originX = originX;
            this.endX = endX;
            this.originY = originY;
            this.endY = endY;
        }

        static FitGeometry of(int width, int height) {
            int big = Math.max(width, height);
            int small = Math.min(width, height);
            int offSmall = FIT_AXIS_OFFSET + ((big - small) / 2) * FIT_AXIS_PIXELS / big;
            int smallSpan = small * FIT_AXIS_PIXELS / big;

            if (width >= height) {
                return new FitGeometry(big, FIT_AXIS_OFFSET, FIT_AXIS_OFFSET + FIT_AXIS_PIXELS, offSmall, offSmall + smallSpan);
            }
            return new FitGeometry(big, offSmall, offSmall + smallSpan, FIT_AXIS_OFFSET, FIT_AXIS_OFFSET + FIT_AXIS_PIXELS);
        }

        /**
         * Maps a fractional tile-space coordinate to fit-mode raster pixel
         * space - the continuous version of the per-pixel {@code tx}/{@code
         * ty} lookup {@link #rebuildFitMode} does in the other direction.
         */
        float pixelX(float tileX) {
            return originX + tileX * FIT_AXIS_PIXELS / (float) big;
        }

        float pixelY(float tileY) {
            return originY + tileY * FIT_AXIS_PIXELS / (float) big;
        }
    }

}
