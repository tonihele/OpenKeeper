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
        float pitch, float x, float y, float z, float refDistance, float maxDistance) {

    public static AudioSpec stereo(String assetPath, float gain) {
        return new AudioSpec(assetPath, false, false, false, gain, 1.0f, 0, 0, 0, 1.0f, 1000.0f);
    }

    public static AudioSpec stereo(String assetPath, float gain, boolean looping, boolean streamed) {
        return new AudioSpec(assetPath, false, looping, streamed, gain, 1.0f, 0, 0, 0, 1.0f, 1000.0f);
    }

    public AudioSpec withGain(float newGain) {
        return new AudioSpec(assetPath, positional, looping, streamed, newGain, pitch, x, y, z, refDistance, maxDistance);
    }

    public AudioSpec withPitch(float newPitch) {
        return new AudioSpec(assetPath, positional, looping, streamed, gain, newPitch, x, y, z, refDistance, maxDistance);
    }

    public AudioSpec withPosition(float nx, float ny, float nz) {
        return new AudioSpec(assetPath, true, looping, streamed, gain, pitch, nx, ny, nz, refDistance, maxDistance);
    }
}