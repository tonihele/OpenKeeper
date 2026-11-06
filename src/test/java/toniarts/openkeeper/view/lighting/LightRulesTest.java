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
package toniarts.openkeeper.view.lighting;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LightRules}.
 */
class LightRulesTest {

    @Test
    void cheapLightingStopsAndRestoresFlickerOnAllCapableSources() {
        LightRules rules = new LightRules();
        LightSource torch = rules.addTorchLight(new Point(1, 1), Vector3f.ZERO, 300, 200, 100, 1.41f, 1.5f);
        LightSource slab = rules.addSlabLight(new Point(2, 2), Vector3f.ZERO, 100, 100, 100, 3f, false, true);
        LightSource steady = rules.addSlabLight(new Point(3, 3), Vector3f.ZERO, 100, 100, 100, 3f, false, false);
        assertTrue(torch.isFlickering());
        assertTrue(slab.isFlickering());
        assertFalse(steady.isFlickering());

        rules.publishReady();
        rules.setCheapLighting(true);
        assertFalse(torch.isFlickering());
        assertFalse(slab.isFlickering());

        rules.setCheapLighting(false);
        assertTrue(torch.isFlickering());
        assertTrue(slab.isFlickering());
        assertFalse(steady.isFlickering());
    }

    @Test
    void lightsCreatedWhileCheapDoNotFlickerButRecover() {
        LightRules rules = new LightRules();
        rules.setCheapLighting(true);
        LightSource torch = rules.addTorchLight(new Point(1, 1), Vector3f.ZERO, 300, 200, 100, 1.41f, 1.5f);
        assertFalse(torch.isFlickering());

        rules.publishReady();
        rules.setCheapLighting(false);
        assertTrue(torch.isFlickering());
    }

    @Test
    void updateIsNoOpUntilPublishReady() {
        LightRules rules = new LightRules();
        rules.addTorchLight(new Point(1, 1), Vector3f.ZERO, 300, 200, 100, 1.41f, 1.5f);
        assertFalse(rules.isReady());
        rules.update(1f / 60f, null);
        assertEquals(0, rules.getUploadedLightCount());

        rules.publishReady();
        assertTrue(rules.isReady());
        rules.update(1f / 60f, null);
        assertTrue(rules.getUploadedLightCount() >= 1);
    }

    @Test
    void disposeStopsConsumerApis() {
        LightRules rules = new LightRules();
        rules.addTorchLight(new Point(1, 1), Vector3f.ZERO, 300, 200, 100, 1.41f, 1.5f);
        rules.publishReady();
        rules.update(1f / 60f, null);
        int uploaded = rules.getUploadedLightCount();
        assertTrue(uploaded >= 1);

        rules.dispose();
        assertFalse(rules.isReady());
        assertEquals(LightRules.Phase.DISPOSED, rules.getPhase());
        rules.update(1f / 60f, null);
        assertEquals(uploaded, rules.getUploadedLightCount());
    }

    @Test
    void slabLightRadiusIsNotScaledByTwoThirds() {
        LightRules rules = new LightRules();
        LightSource slab = rules.addSlabLight(new Point(1, 1), Vector3f.ZERO, 100, 100, 100, 1.5f, false, false);
        assertEquals(LightSource.Kind.SLAB, slab.getKind());
        assertEquals(Math.min(1.5f, LightConstants.MAX_LIGHT_RADIUS_TILES), slab.getRadius());
    }

    @Test
    void clearTileLightsRemovesSlabLights() {
        LightRules rules = new LightRules();
        Point tile = new Point(4, 4);
        rules.addSlabLight(tile, Vector3f.ZERO, 100, 100, 100, 3f, false, false);
        rules.clearTileLights(tile);
        // A second clear must be a harmless no-op
        rules.clearTileLights(tile);
    }
}
