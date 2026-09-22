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

import toniarts.openkeeper.utils.Point;

/**
 * Click -&gt; tile resolution (design §5.11), replacing the original's
 * literal 16.16 {@code cosTable}/{@code sinTable} un-rotation with plain
 * {@code Math.cos}/{@code Math.sin} over the continuous float yaw, per
 * minimap_jmonkey.md Step 1's translation decision - and rather than
 * porting the original's {@code >>2} two-extra-fractional-bits adjustment
 * literally (there's no equivalent fixed-point convention here to match),
 * this is derived directly from the octagon's own confirmed position/UV
 * relationship (see {@link MinimapOctagon}), so it is exact by
 * construction rather than needing an empirical correction factor.
 *
 * <p>
 * Pure float/int math, no jME dependency - headlessly testable.
 */
public final class MinimapClickResolver {

    private MinimapClickResolver() {
    }

    /**
     * Un-rotates a click's displayed panel-local position back to the
     * raster's own unrotated pixel coordinates - the inverse of how the
     * octagon's UVs turn the displayed content by {@code -yaw} relative to
     * the raster (see {@link MinimapOctagon#updateUv}), so un-rotating the
     * click needs a {@code +yaw} rotation.
     *
     * @param localX, localY the click position in the overlay's own 0..1
     * local unit space (0,0 = panel bottom-left, matching
     * {@link MinimapView}'s own coordinate convention)
     * @return the corresponding {@code [pixelX, pixelY]} in the 128x128
     * raster's own pixel space (not necessarily in {@code [0,128)} - the
     * caller clips/validates)
     */
    public static float[] panelLocalToRasterPixel(float localX, float localY, float yawRadians) {
        float displayedDx = localX - 0.5f;
        float displayedDy = localY - 0.5f;
        float cos = (float) Math.cos(yawRadians);
        float sin = (float) Math.sin(yawRadians);
        float rawDx = displayedDx * cos - displayedDy * sin;
        float rawDy = displayedDx * sin + displayedDy * cos;
        float rawLocalX = 0.5f + rawDx;
        float rawLocalY = 0.5f + rawDy;

        int size = MinimapRasteriser.RASTER_SIZE;
        float pixelX = rawLocalX * size;
        float pixelY = (1f - rawLocalY) * size; // inverse of MinimapOctagon's confirmed V flip
        return new float[]{pixelX, pixelY};
    }

    /**
     * @return the tile the given raster pixel falls on, or {@code null} if
     * it's outside the map (design's {@code isValidTargetTile} bounds
     * check - the "resolvable tile exists here" half of it; any additional
     * validity rule belongs to the caller, same as
     * {@link MapDataAccess#isValidTargetTile}'s own placeholder note)
     */
    public static Point resolveTile(float pixelX, float pixelY, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileX, float cameraTileY, int pixelsPerTile, int mapWidth, int mapHeight) {
        float tileXf = fit ? fitGeometry.tileX(pixelX) : MinimapMarkers.zoomedTileX(pixelX, cameraTileX, pixelsPerTile);
        float tileYf = fit ? fitGeometry.tileY(pixelY) : MinimapMarkers.zoomedTileY(pixelY, cameraTileY, pixelsPerTile);
        int tileX = (int) Math.floor(tileXf);
        int tileY = (int) Math.floor(tileYf);
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return null;
        }
        return new Point(tileX, tileY);
    }

}
