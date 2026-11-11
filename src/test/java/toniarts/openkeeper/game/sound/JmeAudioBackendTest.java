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
package toniarts.openkeeper.game.sound;

import com.jme3.audio.AudioNode;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JmeAudioBackendTest {

    @Test
    void convertsMixerUnitsAndClampsSmallStoredMaxima() {
        for (float stored : new float[]{0, 0.1f, 1, 2}) {
            assertEquals(1.05f, AudioSpec.qmixerMaximumInTiles(stored), 0.00001f);
        }
        assertEquals(1.025f, AudioSpec.qmixerMaximumInTiles(2.05f), 0.00001f);
        assertEquals(10, AudioSpec.qmixerMaximumInTiles(20));
    }

    @Test
    void appliesQmixerRolloffAndMutesBeyondTheMaximum() {
        AudioSpec spec = positional();
        assertEquals(1, spec.distanceGain(0));
        assertEquals(1, spec.distanceGain(1));
        assertEquals(0.5f, spec.distanceGain(3), 0.00001f);
        assertEquals(2.0f / 11, spec.distanceGain(10), 0.00001f);
        assertEquals(0, spec.distanceGain(10.01f));
    }

    @Test
    void backendUsesSoftwareGainAndKeepsOpenalPositioningWithoutDoubleAttenuation() {
        Vector3f listener = new Vector3f(0, 25, 0);
        AudioNode node = new AudioNode();
        JmeAudioBackend backend = new JmeAudioBackend(node, positional(), () -> listener);
        backend.setGain(0.8f);
        backend.setPosition(3, 0, 0);
        assertEquals(0.4f, node.getVolume(), 0.00001f);
        assertTrue(node.isPositional());
        assertEquals(Float.MAX_VALUE, node.getRefDistance());
        assertEquals(Float.MAX_VALUE, node.getMaxDistance());
        assertEquals(25, node.getLocalTranslation().y);

        // Each engine tick reapplies gain, so a moving listener must recompute attenuation.
        listener.set(2, 40, 0);
        backend.setGain(0.8f);
        assertEquals(0.8f, node.getVolume(), 0.00001f);
        assertEquals(40, node.getLocalTranslation().y);
        backend.setPosition(13, 0, 0);
        assertEquals(0, node.getVolume());
        backend.setPosition(5, 0, 0);
        assertEquals(0.4f, node.getVolume(), 0.00001f);
    }

    @Test
    void fractionalListenerMovementChangesGainWithoutTileRounding() {
        Vector3f listener = new Vector3f(0.5f, 10, 0);
        AudioNode node = new AudioNode();
        JmeAudioBackend backend = new JmeAudioBackend(node, positional(), () -> listener);
        backend.setPosition(3, 0, 0);
        assertEquals(1 / 1.75f, node.getVolume(), 0.00001f);
    }

    @Test
    void speechDoesNotUseDistanceAttenuation() {
        AudioSpec spec = AudioSpec.stereo("synthetic", 0.75f);
        AudioNode node = new AudioNode();
        JmeAudioBackend backend = new JmeAudioBackend(node, spec, () -> new Vector3f(500, 50, 500));
        backend.setGain(0.6f);
        assertFalse(node.isPositional());
        assertEquals(0.6f, node.getVolume());
        assertEquals(1, spec.distanceGain(10000));
    }

    private static AudioSpec positional() {
        return AudioSpec.stereo("synthetic", 1).withPosition(0, 0, 0).withQmixerDistance(20);
    }
}
