/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.shadow;

import com.jme3.math.Vector3f;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.view.lighting.LightSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ShadowRenderer} and {@link Silhouette}.
 */
class ShadowRendererTest {

    private static LightSource light(LightSource.Kind kind, float x, float y, float z, float radius) {
        return new LightSource(kind, null, new Vector3f(x, y, z), 255, 255, 255, 1f, radius);
    }

    private static LightSource torch(float x, float z) {
        return light(LightSource.Kind.TORCH, x, 1.8f, z, 1.5f);
    }

    private static ShadowRenderer.Selection select(ShadowRenderer renderer, List<LightSource> lights, LightSource own) {
        renderer.sortLightsByTile(lights);
        return renderer.select(10, 10, new Vector3f(10.5f, 1.8f, 10.5f), own);
    }

    @Test
    void opacityIsFullStrengthUpToAboutHalfTheRangeThenFadesToNothing() {
        assertEquals(0.7f, ShadowRenderer.opacity(2.5f, 0f), 1e-6f);
        assertEquals(0.7f, ShadowRenderer.opacity(2.5f, 1.25f), 1e-6f);
        assertEquals(0.375f, ShadowRenderer.opacity(2.5f, 1.875f), 1e-6f);
        assertEquals(0f, ShadowRenderer.opacity(2.5f, 2.5f), 1e-6f);
        assertTrue(ShadowRenderer.opacity(2.5f, 3f) < 0f);
        assertEquals(0f, ShadowRenderer.opacity(0f, 0f));
    }

    @Test
    void rangeStaysAtItsCapFromTwentyFramesPerSecond() {
        ShadowRenderer renderer = new ShadowRenderer(null);
        for (int i = 0; i < 100; i++) {
            renderer.updateRange(1f / 20f);
        }

        assertEquals(2.5f, renderer.getRange(), 1e-6f);
    }

    @Test
    void rangeFallsStepwiseAndIsGoneBelowTenFramesPerSecond() {
        ShadowRenderer renderer = new ShadowRenderer(null);
        renderer.updateRange(1f / 10f);
        assertEquals(2.4f, renderer.getRange(), 1e-5f);

        // The dead band leaves a bit of range at exactly ten
        for (int i = 0; i < 100; i++) {
            renderer.updateRange(1f / 10f);
        }
        assertTrue(renderer.getRange() <= 0.5f + 1e-5f);

        for (int i = 0; i < 100; i++) {
            renderer.updateRange(1f / 5f);
        }
        assertEquals(0f, renderer.getRange(), 1e-6f);

        renderer.updateRange(1f / 20f);
        assertEquals(0.1f, renderer.getRange(), 1e-5f);
    }

    @Test
    void lowLevelQuadIsAPointLightProjectionFromAVirtualHeightOfTwo() {
        float[] extent = ShadowRenderer.extent(2, 2f, 0.5f, 0.4f);

        assertEquals(0.1f, extent[0], 1e-6f);
        assertEquals(0.9f, extent[1], 1e-6f);
    }

    @Test
    void tiltedQuadMatchesTheTiltedSquareProjection() {
        float distance = 1.8f;
        double theta = distance * 0.4712f;

        float[] extent = ShadowRenderer.extent(3, distance, 0.6f, 0.45f);

        assertEquals((0.6 + 0.45 * Math.sin(theta)) * Math.tan(theta) + 0.45 * Math.cos(theta), extent[1], 1e-4);
        assertEquals((0.6 - 0.45 * Math.sin(theta)) * Math.tan(theta) - 0.45 * Math.cos(theta), extent[0], 1e-4);
        assertEquals(-0.45f, ShadowRenderer.extent(3, 0f, 0.6f, 0.45f)[0], 1e-6f); // Under the light: the footprint
    }

    @Test
    void onlyTorchesAndDynamicLightsCastShadows() {
        assertTrue(ShadowRenderer.castsShadows(light(LightSource.Kind.TORCH, 0, 0, 0, 1.5f)));
        assertTrue(ShadowRenderer.castsShadows(light(LightSource.Kind.THING, 0, 0, 0, 1.5f)));
        assertTrue(ShadowRenderer.castsShadows(light(LightSource.Kind.CURSOR, 0, 0, 0, 1.5f)));
        assertFalse(ShadowRenderer.castsShadows(light(LightSource.Kind.TERRAIN, 0, 0, 0, 1.125f)));
        assertFalse(ShadowRenderer.castsShadows(light(LightSource.Kind.SLAB, 0, 0, 0, 1.125f)));
        assertFalse(ShadowRenderer.castsShadows(light(LightSource.Kind.PORTAL, 0, 0, 0, 1.125f)));
    }

    @Test
    void lightReachesTilesWhoseCentreIsWithinItsRadiusPlusOne() {
        ShadowRenderer renderer = new ShadowRenderer(null);
        LightSource near = torch(12.5f, 10.5f); // tile centre 2.0 away, reach 2.5
        LightSource far = torch(13.4f, 10.5f); // 2.9 away

        ShadowRenderer.Selection selection = select(renderer, List.of(near, far), null);

        assertEquals(1, selection.chosen().size());
        assertSame(near, selection.chosen().get(0).light());
    }

    @Test
    void terrainGlowAndDisabledLightsAreLeftOut() {
        ShadowRenderer renderer = new ShadowRenderer(null);
        LightSource glow = light(LightSource.Kind.TERRAIN, 10.5f, 1f, 10.5f, 1.125f);
        LightSource cursor = light(LightSource.Kind.CURSOR, 11f, 2f, 10.5f, 1.75f);
        cursor.setEnabled(false);

        assertTrue(select(renderer, List.of(glow, cursor), null).chosen().isEmpty());
    }

    @Test
    void selectionTakesTheThreeNearestWithinTheRange() {
        ShadowRenderer renderer = new ShadowRenderer(null);
        LightSource a = torch(10.5f, 11.5f);
        LightSource b = torch(10.5f, 12.5f);
        LightSource c = torch(10.5f, 9f);
        LightSource tooFar = torch(10.5f, 14f);

        ShadowRenderer.Selection selection = select(renderer, List.of(tooFar, b, c, a), null);

        assertEquals(List.of(a, c, b), selection.chosen().stream().map(ShadowRenderer.Candidate::light).toList());
        assertEquals(2.5f, selection.fadeRange());
    }

    @Test
    void fourthNearestLightSetsTheFadeRangeSoNothingPops() {
        ShadowRenderer renderer = new ShadowRenderer(null);
        LightSource a = torch(10.5f, 11f);
        LightSource b = torch(10.5f, 11.5f);
        LightSource c = torch(10.5f, 12f);
        LightSource d = torch(10.5f, 8.5f);

        ShadowRenderer.Selection selection = select(renderer, List.of(a, b, c, d), null);

        assertEquals(3, selection.chosen().size());
        assertEquals(2f, selection.fadeRange(), 1e-6f);
        assertFalse(selection.chosen().stream().anyMatch(candidate -> candidate.light() == d));
    }

    @Test
    void ownLightNeverShadowsItsCarrier() {
        ShadowRenderer renderer = new ShadowRenderer(null);
        LightSource own = light(LightSource.Kind.THING, 10.5f, 1.5f, 10.8f, 1f);
        LightSource other = torch(11.5f, 10.5f);

        ShadowRenderer.Selection selection = select(renderer, List.of(own, other), own);

        assertEquals(1, selection.chosen().size());
        assertSame(other, selection.chosen().get(0).light());
    }

    @Test
    void emptyBufferHasNoCoverage() {
        Silhouette silhouette = new Silhouette();

        assertEquals(0, silhouette.get(16, 16));
    }

    @Test
    void squareOfTwoTrianglesCoversItsPixelsFullyAndNothingElse() {
        Silhouette silhouette = new Silhouette();

        // Pixels 8..23 are raster units 64..192
        silhouette.triangle(64, 64, 192, 64, 192, 192);
        silhouette.triangle(64, 64, 192, 192, 64, 192);

        assertEquals(255, silhouette.get(8, 8));
        assertEquals(255, silhouette.get(16, 16));
        assertEquals(255, silhouette.get(23, 23));
        assertEquals(0, silhouette.get(7, 16));
        assertEquals(0, silhouette.get(24, 16));
        assertEquals(0, silhouette.get(16, 7));
        assertEquals(0, silhouette.get(16, 24));
    }

    @Test
    void partlyCoveredPixelGetsFourPerCoveredSubsample() {
        Silhouette silhouette = new Silhouette();

        // The left 4 of the 8 subsample columns of pixel (8, 8)
        silhouette.triangle(64, 64, 68, 64, 68, 72);
        silhouette.triangle(64, 64, 68, 72, 64, 72);

        assertEquals(8 * 4 * 4, silhouette.get(8, 8));
    }

    @Test
    void overlapSaturatesAndTrianglesOutsideAreClipped() {
        Silhouette silhouette = new Silhouette();

        for (int i = 0; i < 3; i++) {
            silhouette.triangle(-500, -500, 800, -500, 100, 900);
        }
        silhouette.triangle(Float.NaN, 0, 1, 1, 2, 2);

        assertEquals(255, silhouette.get(16, 16));
    }

    @Test
    void projectionFillsTheBoundingSphereSquare() {
        // A flat 1x1 square on the floor around the origin
        float[] vertices = {-0.5f, 0, -0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0.5f, -0.5f, 0, 0.5f};
        int[] triangles = {0, 1, 2, 0, 2, 3};
        Silhouette silhouette = new Silhouette();

        silhouette.draw(vertices, 4, triangles, 2, Vector3f.ZERO, 0.5f, 1, 0, 0f);

        // Radius 0.5 maps to +-125 raster units (3..253): pixels 1..30 are
        // covered fully, the border pixels only partly
        assertEquals(255, silhouette.get(16, 16));
        assertEquals(255, silhouette.get(1, 1));
        assertEquals(255, silhouette.get(30, 30));
        assertTrue(silhouette.get(0, 16) > 0 && silhouette.get(0, 16) < 255);
    }

    @Test
    void tiltedViewShiftsHighPointsAlongTheLightDirection() {
        float[] vertices = {-0.05f, 0.4f, -0.05f, 0.05f, 0.4f, -0.05f, 0f, 0.4f, 0.05f};
        int[] triangles = {0, 1, 2};
        Silhouette straight = new Silhouette();
        Silhouette tilted = new Silhouette();

        straight.draw(vertices, 3, triangles, 1, Vector3f.ZERO, 0.5f, 1, 0, 0f);
        tilted.draw(vertices, 3, triangles, 1, Vector3f.ZERO, 0.5f, 1, 0, 0.8f);

        // Tilted, a point above the centre moves along the light direction to rows 23..26
        assertTrue(straight.get(16, 16) > 0);
        assertEquals(0, tilted.get(16, 16));
        int shifted = 0;
        for (int row = 22; row <= 27; row++) {
            shifted += tilted.get(16, row);
        }
        assertTrue(shifted > 0);
    }

    @Test
    void writesTheCoverageAsTheAlphaOfBlackPixels() {
        Silhouette silhouette = new Silhouette();
        silhouette.triangle(0, 0, 256, 0, 0, 256);
        silhouette.triangle(256, 0, 256, 256, 0, 256);
        ByteBuffer pixels = ByteBuffer.allocate(32 * 32 * 4);

        silhouette.writeTo(pixels);

        assertEquals(0, pixels.get(0));
        assertEquals(0, pixels.get(2));
        assertEquals((byte) 255, pixels.get(3));
    }
}
