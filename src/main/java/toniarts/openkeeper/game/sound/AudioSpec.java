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
package toniarts.openkeeper.game.sound;

/**
 * Everything the engine needs to know to start a clip.
 */
public record AudioSpec(String assetPath, boolean positional, boolean looping, boolean streamed, float gain,
        float pitch, float x, float y, float z, float refDistance, float maxDistance, float rolloffFactor) {

    public static final float MIN_PITCH = 0.5f;
    public static final float MAX_PITCH = 2.0f;

    public static AudioSpec stereo(String assetPath, float gain) {
        return new AudioSpec(assetPath, false, false, false, gain, 1.0f, 0, 0, 0, 1.0f, 1000.0f, 1.0f);
    }

    public static AudioSpec stereo(String assetPath, float gain, boolean looping, boolean streamed) {
        return new AudioSpec(assetPath, false, looping, streamed, gain, 1.0f, 0, 0, 0, 1.0f, 1000.0f, 1.0f);
    }

    /**
     * DKII initializes two mixer units per tile (0x567354) and supplies minimum 2,
     * scale 0.5, and the group maximum to QMixer (0x60CF62). The SFX category's
     * three header floats describe EAX reverb and are unrelated to this mapping.
     */
    public AudioSpec withQmixerDistance(float groupMaximum) {
        return new AudioSpec(assetPath, positional, looping, streamed, gain, pitch, x, y, z,
                1.0f, qmixerMaximumInTiles(groupMaximum), 0.5f);
    }

    static float qmixerMaximumInTiles(float groupMaximum) {
        // The native path raises maxima <= 2 mixer units to about 2.1.
        return (groupMaximum > 2.0f ? groupMaximum : 2.1f) / 2.0f;
    }

    /** QMixer's inverse-distance gain, including its explicit maximum-distance mute. */
    float distanceGain(float distance) {
        if (!positional) {
            return 1.0f;
        }
        if (distance > maxDistance) {
            return 0.0f;
        }
        return refDistance / (refDistance + rolloffFactor * (Math.max(distance, refDistance) - refDistance));
    }

    public AudioSpec withGain(float newGain) {
        return new AudioSpec(assetPath, positional, looping, streamed, newGain, pitch, x, y, z,
                refDistance, maxDistance, rolloffFactor);
    }

    /**
     * Sets the pitch, held to the range jME's AudioNode accepts. The group pitch of a sound is a
     * signed byte on top of 100, so it can reach 0 to 2.27.
     */
    public AudioSpec withPitch(float newPitch) {
        return new AudioSpec(assetPath, positional, looping, streamed, gain,
                Math.max(MIN_PITCH, Math.min(MAX_PITCH, newPitch)), x, y, z, refDistance, maxDistance, rolloffFactor);
    }

    public AudioSpec withPosition(float nx, float ny, float nz) {
        return new AudioSpec(assetPath, true, looping, streamed, gain, pitch, nx, ny, nz,
                refDistance, maxDistance, rolloffFactor);
    }
}