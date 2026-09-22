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

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimapMarkersTest {

    private static byte[] blankRaster() {
        return new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];
    }

    private static void assertPixel(byte[] raster, int x, int y, int expectedArgb) {
        int i = (y * MinimapRasteriser.RASTER_SIZE + x) * 3;
        assertEquals((byte) expectedArgb, raster[i], "blue at (" + x + "," + y + ")");
        assertEquals((byte) (expectedArgb >> 8), raster[i + 1], "green at (" + x + "," + y + ")");
        assertEquals((byte) (expectedArgb >> 16), raster[i + 2], "red at (" + x + "," + y + ")");
    }

    @Test
    void zoomedPixelIsCentredAtTheCameraTile() {
        assertEquals(64f, MinimapMarkers.zoomedPixelX(5f, 5f, 4), 1e-4f);
        assertEquals(64f + 8, MinimapMarkers.zoomedPixelX(7f, 5f, 4), 1e-4f); // 2 tiles right at 4px/tile
    }

    @Test
    void blinkPicksAOnOddParityAndBOnEven() {
        assertEquals(0xAA, MinimapMarkers.blink(0xAA, 0xBB, true));
        assertEquals(0xBB, MinimapMarkers.blink(0xAA, 0xBB, false));
    }

    @Test
    void dotWritesASinglePixelNeighbourhood() {
        byte[] raster = blankRaster();
        MinimapMarkers.dot(raster, 10, 10, 0xFF102030);
        assertPixel(raster, 10, 10, 0xFF102030);
        // A dot (radius 1, disc) also touches the 4 orthogonal neighbours.
        assertPixel(raster, 11, 10, 0xFF102030);
        assertPixel(raster, 9, 10, 0xFF102030);
    }

    @Test
    void discDoesNotPaintOutsideItsRadius() {
        byte[] raster = blankRaster();
        MinimapMarkers.disc(raster, 10, 10, 2, 0xFFFFFFFF);
        // Corner of the bounding box, outside the radius.
        assertPixel(raster, 12, 12, 0);
    }

    @Test
    void dottedLineAlternatesInRunsOfTheGivenDashLength() {
        byte[] raster = blankRaster();
        MinimapMarkers.dottedLine(raster, 0, 0, 11, 0, 0xFFFFFFFF, 3, 0);

        assertPixel(raster, 0, 0, 0xFFFFFFFF); // first dash
        assertPixel(raster, 2, 0, 0xFFFFFFFF);
        assertPixel(raster, 3, 0, 0); // first gap
        assertPixel(raster, 5, 0, 0);
        assertPixel(raster, 6, 0, 0xFFFFFFFF); // second dash
        assertPixel(raster, 9, 0, 0); // second gap
    }

    @Test
    void dottedLinePhaseOffsetShiftsTheDashPattern() {
        byte[] raster = blankRaster();
        MinimapMarkers.dottedLine(raster, 0, 0, 11, 0, 0xFFFFFFFF, 3, 3);

        // A phase offset of one full dash length flips what was on to off.
        assertPixel(raster, 0, 0, 0);
        assertPixel(raster, 3, 0, 0xFFFFFFFF);
    }

    @Test
    void circleOutlineDoesNotPaintTheInterior() {
        byte[] raster = blankRaster();
        MinimapMarkers.circleOutline(raster, 10, 10, 4, 0xFFFFFFFF);
        assertPixel(raster, 10, 10, 0); // centre, untouched
        assertPixel(raster, 14, 10, 0xFFFFFFFF); // right edge, on the ring
        assertPixel(raster, 10, 14, 0xFFFFFFFF); // bottom edge, on the ring
    }

    @Test
    void discIsClippedAtTheRasterEdgeWithoutThrowing() {
        byte[] raster = blankRaster();
        MinimapMarkers.disc(raster, 0, 0, 3, 0xFFFFFFFF);
        assertPixel(raster, 0, 0, 0xFFFFFFFF);
    }

    @Test
    void lineConnectsItsTwoEndpoints() {
        byte[] raster = blankRaster();
        MinimapMarkers.line(raster, 0, 0, 5, 0, 0xFF00FF00);
        assertPixel(raster, 0, 0, 0xFF00FF00);
        assertPixel(raster, 5, 0, 0xFF00FF00);
        assertPixel(raster, 3, 0, 0xFF00FF00);
    }

    @Test
    void rectOutlineDoesNotFillTheInterior() {
        byte[] raster = blankRaster();
        MinimapMarkers.rectOutline(raster, 10, 10, 14, 14, 0xFFFFFFFF);
        assertPixel(raster, 10, 10, 0xFFFFFFFF); // corner
        assertPixel(raster, 12, 10, 0xFFFFFFFF); // top edge
        assertPixel(raster, 12, 12, 0); // interior, untouched
    }

    @Test
    void clipToEdgeReturnsAPointOnTheRasterBoundary() {
        float[] edge = MinimapMarkers.clipToEdge(64, 64, 1f, 0f, MinimapRasteriser.RASTER_SIZE);
        assertEquals(128f, edge[0], 1e-3f);
        assertEquals(64f, edge[1], 1e-3f);
    }

    @Test
    void neutralRotationColourCyclesAndWrapsAroundThePhaseCount() {
        int phase0 = MinimapMarkers.neutralRotationColour(0);
        int phase1 = MinimapMarkers.neutralRotationColour(1);
        assertEquals(0xFFFF0000, phase0);
        assertEquals(0xFFFF8000, phase1);
        // Wraps back to the same colour after a full cycle.
        assertEquals(phase0, MinimapMarkers.neutralRotationColour(6));
        // Negative phases (Math.floorMod, not %) don't throw or go out of bounds.
        assertEquals(phase0, MinimapMarkers.neutralRotationColour(-6));
    }

    @Test
    void clipToEdgeHandlesADiagonalDirection() {
        float[] edge = MinimapMarkers.clipToEdge(64, 64, 1f, 1f, MinimapRasteriser.RASTER_SIZE);
        // Symmetric diagonal from centre hits the corner (128,128).
        assertEquals(128f, edge[0], 1e-3f);
        assertEquals(128f, edge[1], 1e-3f);
    }

}
