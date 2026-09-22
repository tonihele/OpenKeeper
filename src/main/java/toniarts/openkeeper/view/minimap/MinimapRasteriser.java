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
 * {@link MinimapPalette}
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

        // 1. Neutral-player guard, before every raster pass (design §5.3/§4.1).
        palette.applyNeutralGuard(neutralPlayerNumber);

        // 2. Whole raster starts as rock. Class-1 (unexplored/impenetrable)
        // tiles are meant to show rock anyway (design §5.3 step 4), so
        // leaving this untouched for those pixels is correct, not just a
        // placeholder - this also covers "outside the map rectangle is
        // rock" for free, standing in for Step 5's border-band fills.
        System.arraycopy(rockTextureBgr, 0, outBgr, 0, outBgr.length);

        // 3. Fit-mode geometry (design §5.5). Camera position is ignored.
        int big = Math.max(width, height);
        int small = Math.min(width, height);
        int offSmall = FIT_AXIS_OFFSET + ((big - small) / 2) * FIT_AXIS_PIXELS / big;
        int smallSpan = small * FIT_AXIS_PIXELS / big;

        int originX;
        int endX;
        int originY;
        int endY;
        if (width >= height) {
            originX = FIT_AXIS_OFFSET;
            endX = FIT_AXIS_OFFSET + FIT_AXIS_PIXELS;
            originY = offSmall;
            endY = offSmall + smallSpan;
        } else {
            originX = offSmall;
            endX = offSmall + smallSpan;
            originY = FIT_AXIS_OFFSET;
            endY = FIT_AXIS_OFFSET + FIT_AXIS_PIXELS;
        }

        // 4. Per-pixel fill inside the map rectangle.
        int[] argb = palette.rawArgb();
        for (int py = Math.max(originY, 0); py < Math.min(endY, RASTER_SIZE); py++) {
            int ty = (py - originY) * big / FIT_AXIS_PIXELS;
            if (ty < 0 || ty >= height) {
                continue;
            }
            for (int px = Math.max(originX, 0); px < Math.min(endX, RASTER_SIZE); px++) {
                int tx = (px - originX) * big / FIT_AXIS_PIXELS;
                if (tx < 0 || tx >= width) {
                    continue;
                }
                short cls = grid.get(tx, ty);
                if (cls == MapColourClass.UNEXPLORED_OR_IMPENETRABLE) {
                    continue; // already rock from the base fill
                }
                int colour = argb[cls];
                int i = (py * RASTER_SIZE + px) * 3;
                outBgr[i] = (byte) colour; // blue
                outBgr[i + 1] = (byte) (colour >> 8); // green
                outBgr[i + 2] = (byte) (colour >> 16); // red
            }
        }
    }

}
