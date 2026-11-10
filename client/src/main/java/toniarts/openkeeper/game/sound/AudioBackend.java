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

import javax.annotation.Nullable;

/**
 * Abstraction of the audio device, so that the sound engine can be exercised
 * without one.
 */
public interface AudioBackend {

    Status getStatus();

    void play();

    void pause();

    void stop();

    void setGain(float gain);

    void setPitch(float pitch);

    void setLooping(boolean looping);

    void setTimeOffset(float seconds);

    float getPlaybackTime();

    /**
     * Moves a positional sound. Backends that cannot move a sound ignore this.
     */
    default void setPosition(float x, float y, float z) {
    }

    /**
     * @return clip length in seconds, or {@code 0} if it is not known
     */
    float getDuration();

    /**
     * Sound backend abstraction of {@code AudioSource.Status}.
     */
    enum Status {

        PLAYING,
        PAUSED,
        STOPPED
    }

    interface AudioBackendFactory {

        /**
         * @param assetPath asset path of the clip, or {@code null} if the clip
         * cannot be loaded
         */
        @Nullable
        AudioBackend create(AudioSpec spec);
    }
}