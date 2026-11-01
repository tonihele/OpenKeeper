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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.view.lighting.LightSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TileLightSets} and {@link ShadowLightSelector}.
 */
class ShadowLightSelectionTest {

    private static LightSource light(LightSource.Kind kind, float x, float y, float z, float radius) {
        return new LightSource(kind, null, new Vector3f(x, y, z), 255, 255, 255, 1f, radius);
    }

    private static LightSource torch(float x, float z) {
        return light(LightSource.Kind.TORCH, x, 1.8f, z, 1.5f);
    }

    @Test
    void onlyTorchesAndDynamicLightsCastShadows() {
        assertTrue(TileLightSets.castsShadows(light(LightSource.Kind.TORCH, 0, 0, 0, 1.5f)));
        assertTrue(TileLightSets.castsShadows(light(LightSource.Kind.THING, 0, 0, 0, 1.5f)));
        assertTrue(TileLightSets.castsShadows(light(LightSource.Kind.CURSOR, 0, 0, 0, 1.5f)));
        assertFalse(TileLightSets.castsShadows(light(LightSource.Kind.TERRAIN, 0, 0, 0, 1.125f)));
        assertFalse(TileLightSets.castsShadows(light(LightSource.Kind.SLAB, 0, 0, 0, 1.125f)));
        assertFalse(TileLightSets.castsShadows(light(LightSource.Kind.PORTAL, 0, 0, 0, 1.125f)));
    }

    @Test
    void lightReachesTilesWhoseCentreIsWithinItsRadiusPlusOne() {
        TileLightSets sets = new TileLightSets();
        LightSource torch = torch(10.5f, 10.5f);
        sets.beginFrame(List.of(torch));

        assertTrue(sets.get(12, 10).contains(torch)); // centre 2.0 away
        assertTrue(sets.get(10, 12).contains(torch));
        assertFalse(sets.get(13, 10).contains(torch)); // centre 3.0 away, reach is 2.5
        assertFalse(sets.get(20, 20).contains(torch));
    }

    @Test
    void disabledLightsAreLeftOut() {
        TileLightSets sets = new TileLightSets();
        LightSource cursor = light(LightSource.Kind.CURSOR, 5.5f, 2f, 5.5f, 1.75f);
        cursor.setEnabled(false);
        sets.beginFrame(List.of(cursor));

        assertTrue(sets.get(5, 5).isEmpty());
    }

    @Test
    void staticPlacesAreCappedAtTwelveAndTheNearestWin() {
        TileLightSets sets = new TileLightSets();
        List<LightSource> lights = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            lights.add(light(LightSource.Kind.TERRAIN, 10.5f + 0.1f * (i + 1), 1f, 10.5f, 1.125f));
        }
        LightSource nearTorch = torch(10.5f, 10.5f);
        lights.add(nearTorch);
        sets.beginFrame(lights);

        List<LightSource> set = sets.get(10, 10);

        assertEquals(12, set.size());
        assertTrue(set.contains(nearTorch));
        assertFalse(set.contains(lights.get(11))); // the farthest glow made room for it
    }

    @Test
    void staticLightsComeFirstAndTheTotalIsCappedAtTwentyFour() {
        TileLightSets sets = new TileLightSets();
        List<LightSource> lights = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            lights.add(light(LightSource.Kind.THING, 10.5f + 0.01f * i, 1.5f, 10.5f, 1f));
        }
        LightSource torch = torch(11f, 10.5f);
        lights.add(torch);
        sets.beginFrame(lights);

        List<LightSource> set = sets.get(10, 10);

        assertEquals(24, set.size());
        assertSame(torch, set.get(0));
    }

    @Test
    void selectionTakesTheThreeNearestCastersWithinTheRange() {
        LightSource a = torch(0f, 1f);
        LightSource b = torch(0f, 2f);
        LightSource c = torch(0f, -1.5f);
        LightSource tooFar = torch(0f, 4f);
        LightSource glow = light(LightSource.Kind.TERRAIN, 0f, 1.8f, 0.2f, 1.125f);

        ShadowLightSelector.Selection selection = ShadowLightSelector.select(
                List.of(tooFar, glow, b, c, a), new Vector3f(0f, 1.8f, 0f), 2.5f, null);

        assertEquals(3, selection.chosen().size());
        assertSame(a, selection.chosen().get(0).light());
        assertSame(c, selection.chosen().get(1).light());
        assertSame(b, selection.chosen().get(2).light());
        assertEquals(2.5f, selection.fadeRange());
    }

    @Test
    void fourthNearestLightSetsTheFadeRangeSoNothingPops() {
        LightSource a = torch(0f, 0.5f);
        LightSource b = torch(0f, 1f);
        LightSource c = torch(0f, 1.5f);
        LightSource d = torch(0f, -2f);

        ShadowLightSelector.Selection selection = ShadowLightSelector.select(
                List.of(a, b, c, d), new Vector3f(0f, 1.8f, 0f), 2.5f, null);

        assertEquals(3, selection.chosen().size());
        assertEquals(2f, selection.fadeRange(), 1e-6f);
        assertFalse(selection.chosen().stream().anyMatch(candidate -> candidate.light() == d));
    }

    @Test
    void selectionUsesTheThreeDimensionalDistance() {
        LightSource high = light(LightSource.Kind.TORCH, 0f, 4.5f, 1f, 1.5f);

        ShadowLightSelector.Selection selection = ShadowLightSelector.select(
                List.of(high), new Vector3f(0f, 1.8f, 0f), 2.5f, null);

        assertTrue(selection.chosen().isEmpty()); // 2.7 up, 1 across
    }

    @Test
    void ownLightNeverShadowsItsCarrierAndNoRangeMeansNoShadows() {
        LightSource own = light(LightSource.Kind.THING, 0f, 1.5f, 0.3f, 1f);
        LightSource other = torch(1f, 0f);

        ShadowLightSelector.Selection selection = ShadowLightSelector.select(
                List.of(own, other), new Vector3f(0f, 1.5f, 0f), 2.5f, own);
        assertEquals(1, selection.chosen().size());
        assertSame(other, selection.chosen().get(0).light());

        assertTrue(ShadowLightSelector.select(List.of(other), new Vector3f(0f, 1.5f, 0f), 0f, null).chosen().isEmpty());
    }
}
