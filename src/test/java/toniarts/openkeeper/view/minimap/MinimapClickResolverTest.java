/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The click round-trip test design §8/§9.5 asks for: the tile a click
 * resolves to must equal the tile visually under that point on the
 * rotated, zoomed octagon.
 */
class MinimapClickResolverTest {

    /**
     * The forward transform {@link MinimapOctagon}/{@link MinimapView}
     * apply to go from a raster pixel to the displayed panel-local
     * position - reimplemented independently here (not by calling
     * production code) so this test can't pass merely because both sides
     * share a bug.
     */
    private static float[] rasterPixelToPanelLocal(float pixelX, float pixelY, float yawRadians) {
        int size = MinimapRasteriser.RASTER_SIZE;
        float rawLocalX = pixelX / size;
        float rawLocalY = 1f - pixelY / size;
        float rawDx = rawLocalX - 0.5f;
        float rawDy = rawLocalY - 0.5f;
        float cos = (float) Math.cos(-yawRadians);
        float sin = (float) Math.sin(-yawRadians);
        float displayedDx = rawDx * cos - rawDy * sin;
        float displayedDy = rawDx * sin + rawDy * cos;
        return new float[]{0.5f + displayedDx, 0.5f + displayedDy};
    }

    @Test
    void panelLocalToRasterPixelInvertsTheForwardTransformAtZeroYaw() {
        float[] local = rasterPixelToPanelLocal(30f, 90f, 0f);
        float[] pixel = MinimapClickResolver.panelLocalToRasterPixel(local[0], local[1], 0f);
        assertEquals(30f, pixel[0], 1e-3f);
        assertEquals(90f, pixel[1], 1e-3f);
    }

    @Test
    void panelLocalToRasterPixelInvertsTheForwardTransformUnderRotation() {
        float yaw = 1.1f; // arbitrary, non-trivial angle
        float[] local = rasterPixelToPanelLocal(45f, 20f, yaw);
        float[] pixel = MinimapClickResolver.panelLocalToRasterPixel(local[0], local[1], yaw);
        assertEquals(45f, pixel[0], 1e-3f);
        assertEquals(20f, pixel[1], 1e-3f);
    }

    @Test
    void panelLocalToRasterPixelRoundTripsForAGridOfPointsAndYaws() {
        int size = MinimapRasteriser.RASTER_SIZE;
        float[] yaws = {0f, 0.3f, 1.0f, 2.1f, -1.7f, (float) Math.PI};
        for (float yaw : yaws) {
            for (int px = 0; px < size; px += 17) {
                for (int py = 0; py < size; py += 17) {
                    float[] local = rasterPixelToPanelLocal(px, py, yaw);
                    float[] resolved = MinimapClickResolver.panelLocalToRasterPixel(local[0], local[1], yaw);
                    assertEquals(px, resolved[0], 0.01f, "px at yaw=" + yaw + " (" + px + "," + py + ")");
                    assertEquals(py, resolved[1], 0.01f, "py at yaw=" + yaw + " (" + px + "," + py + ")");
                }
            }
        }
    }

    @Test
    void resolveTileInvertsFitGeometryPixelMapping() {
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(85, 85);
        for (int tx = 0; tx < 85; tx += 7) {
            for (int ty = 0; ty < 85; ty += 7) {
                float px = fit.pixelX(tx + 0.5f);
                float py = fit.pixelY(ty + 0.5f);
                Point resolved = MinimapClickResolver.resolveTile(px, py, true, fit, 0f, 0f, 0, 85, 85);
                assertNotNull(resolved, "(" + tx + "," + ty + ")");
                assertEquals(tx, resolved.x);
                assertEquals(ty, resolved.y);
            }
        }
    }

    @Test
    void resolveTileInvertsZoomedPixelMapping() {
        int pixelsPerTile = 4; // zoom 2
        float cameraTileX = 20f;
        float cameraTileY = 15f;
        for (int tx = 10; tx < 30; tx += 3) {
            for (int ty = 5; ty < 25; ty += 3) {
                float px = MinimapMarkers.zoomedPixelX(tx + 0.5f, cameraTileX, pixelsPerTile);
                float py = MinimapMarkers.zoomedPixelY(ty + 0.5f, cameraTileY, pixelsPerTile);
                Point resolved = MinimapClickResolver.resolveTile(px, py, false, null, cameraTileX, cameraTileY, pixelsPerTile, 64, 64);
                assertNotNull(resolved, "(" + tx + "," + ty + ")");
                assertEquals(tx, resolved.x);
                assertEquals(ty, resolved.y);
            }
        }
    }

    @Test
    void resolveTileReturnsNullOutsideTheMapInFitMode() {
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(50, 50);
        assertNull(MinimapClickResolver.resolveTile(-10f, 64f, true, fit, 0f, 0f, 0, 50, 50));
    }

    @Test
    void resolveTileReturnsNullOutsideTheMapInZoomedMode() {
        assertNull(MinimapClickResolver.resolveTile(1000f, 1000f, false, null, 0f, 0f, 1, 50, 50));
    }

    @Test
    void fullRoundTripFromTileThroughClickAndBackAtANontrivialYaw() {
        // The full pipeline: tile -> fit pixel -> displayed panel position
        // (as the octagon would actually show it, rotated) -> click ->
        // resolved tile. This is the exact scenario design §9.5 asks to
        // verify: the tile chosen must equal the tile visually under that
        // point on the rotated octagon.
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(64, 64);
        float yaw = 2.4f;

        for (int tx = 5; tx < 60; tx += 11) {
            for (int ty = 5; ty < 60; ty += 11) {
                float px = fit.pixelX(tx + 0.5f);
                float py = fit.pixelY(ty + 0.5f);
                float[] displayedLocal = rasterPixelToPanelLocal(px, py, yaw);

                float[] resolvedPixel = MinimapClickResolver.panelLocalToRasterPixel(displayedLocal[0], displayedLocal[1], yaw);
                Point resolvedTile = MinimapClickResolver.resolveTile(resolvedPixel[0], resolvedPixel[1], true, fit, 0f, 0f, 0, 64, 64);

                assertNotNull(resolvedTile, "(" + tx + "," + ty + ")");
                assertEquals(tx, resolvedTile.x, "(" + tx + "," + ty + ")");
                assertEquals(ty, resolvedTile.y, "(" + tx + "," + ty + ")");
            }
        }
    }

}
