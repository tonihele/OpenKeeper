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
package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.logic.MovementSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionPredictionBufferTest {

    private final PositionPredictionBuffer buffer = new PositionPredictionBuffer(MovementSettings.DEFAULT);
    private final Vector3f sampledPosition = new Vector3f();

    @Test
    void interpolatesBetweenAuthoritativeSamples() {
        buffer.addSample(0d, new Position(0f, new Vector3f(0, 0, 0)));
        buffer.addSample(1d, new Position(1f, new Vector3f(0.5f, 0, 1)));

        float rotation = buffer.sample(0.5d, sampledPosition);

        assertEquals(0.25f, sampledPosition.x, 0.0001f);
        assertEquals(0.5f, sampledPosition.z, 0.0001f);
        assertEquals(0.5f, rotation, 0.0001f);
    }

    @Test
    void extrapolatesOnlyForTheConfiguredWindow() {
        buffer.addSample(0d, new Position(0f, new Vector3f(0, 0, 0)));
        buffer.addSample(1d, new Position(0f, new Vector3f(1, 0, 0)));

        buffer.sample(2d, sampledPosition);

        assertEquals(1.1f, sampledPosition.x, 0.0001f);
    }

    @Test
    void latePositionSamplesRemainPredictableAcrossTheDefaultWindow() {
        buffer.addSample(0d, new Position(0f, new Vector3f(0, 0, 0)));
        buffer.addSample(0.05d, new Position(0f, new Vector3f(0.05f, 0, 0)));

        buffer.sample(0.14d, sampledPosition);

        assertEquals(0.14f, sampledPosition.x, 0.0001f);

        buffer.sample(0.2d, sampledPosition);
        assertEquals(0.15f, sampledPosition.x, 0.0001f);
    }

    @Test
    void largeCorrectionsSnapAndDiscardOldHistory() {
        buffer.addSample(0d, new Position(0f, new Vector3f(0, 0, 0)));
        assertTrue(buffer.addSample(0.1d, new Position(0f, new Vector3f(10, 0, 0))));

        assertFalse(buffer.hasHistory());
        buffer.sample(0d, sampledPosition);
        assertEquals(10f, sampledPosition.x, 0f);
    }

    @Test
    void rotationUsesTheShortArcAcrossPi() {
        buffer.addSample(0d, new Position(3.1f, new Vector3f(0, 0, 0)));
        buffer.addSample(1d, new Position(-3.1f, new Vector3f(1, 0, 0)));

        float rotation = buffer.sample(0.5d, sampledPosition);

        assertEquals(Math.PI, Math.abs(rotation), 0.05f);
    }

    @Test
    void unchangedPositionsContributeAStationaryVelocitySample() {
        Position position = new Position(0f, new Vector3f(1, 2, 3));
        buffer.addSample(0d, position);

        assertTrue(buffer.addSample(1d, new Position(0f, new Vector3f(1, 2, 3))));
        assertTrue(buffer.hasHistory());
        buffer.sample(2d, sampledPosition);
        assertEquals(1f, sampledPosition.x, 0f);
    }

    @Test
    void staleTeleportCannotReplaceNewerHistory() {
        buffer.addSample(0d, new Position(0f, new Vector3f(0, 0, 0)));
        buffer.addSample(1d, new Position(0f, new Vector3f(1, 0, 0)));

        assertFalse(buffer.addSample(0.5d, new Position(0f, new Vector3f(100, 0, 0))));

        assertTrue(buffer.hasHistory());
        buffer.sample(1d, sampledPosition);
        assertEquals(1f, sampledPosition.x, 0f);
    }

    @Test
    void wrappedHistoryInterpolatesBetweenItsMostRecentSamples() {
        for (int sample = 0; sample <= 20; sample++) {
            buffer.addSample(sample * 0.05d,
                    new Position(0f, new Vector3f(sample * 0.1f, 0f, 0f)));
        }

        buffer.sample(0.925d, sampledPosition);

        assertEquals(1.85f, sampledPosition.x, 0.0001f);
    }

    @Test
    void wrappedHistoryReplacesTheLatestSampleAtTheSameTime() {
        for (int sample = 0; sample <= 20; sample++) {
            buffer.addSample(sample * 0.05d,
                    new Position(0f, new Vector3f(sample * 0.1f, 0f, 0f)));
        }
        assertTrue(buffer.addSample(1d, new Position(0f, new Vector3f(2.1f, 0f, 0f))));

        buffer.sample(1d, sampledPosition);

        assertEquals(2.1f, sampledPosition.x, 0.0001f);
        assertTrue(buffer.hasHistory());
    }

    @Test
    void teleportAfterHistoryWrapKeepsOnlyTheCorrection() {
        for (int sample = 0; sample <= 20; sample++) {
            buffer.addSample(sample * 0.05d,
                    new Position(0f, new Vector3f(sample * 0.1f, 0f, 0f)));
        }
        assertTrue(buffer.addSample(1.05d, new Position(0f, new Vector3f(20f, 0f, 0f))));

        assertFalse(buffer.hasHistory());
        buffer.sample(1.05d, sampledPosition);
        assertEquals(20f, sampledPosition.x, 0f);
    }

    @Test
    void invalidSampleTimeFallsBackToTheLatestPosition() {
        buffer.addSample(0d, new Position(0f, new Vector3f(1f, 0f, 0f)));
        buffer.addSample(0.05d, new Position(0f, new Vector3f(2f, 0f, 0f)));

        buffer.sample(Double.NaN, sampledPosition);

        assertEquals(2f, sampledPosition.x, 0f);
    }
}
