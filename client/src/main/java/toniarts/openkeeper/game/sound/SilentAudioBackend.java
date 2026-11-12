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
 * Backend that plays nothing. Used when there is no audio device, e.g. in the
 * model viewer before an {@code Application} exists, and in tests.
 */
public final class SilentAudioBackend implements AudioBackend {

    private final AudioSpec spec;
    private final float duration;

    private Status status = Status.STOPPED;
    private float gain;
    private float pitch;
    private float playbackTime;
    private boolean looping;

    public SilentAudioBackend(AudioSpec spec) {
        this(spec, 0.0f);
    }

    public SilentAudioBackend(AudioSpec spec, float duration) {
        this.spec = spec;
        this.duration = duration;
        this.gain = spec.gain();
        this.pitch = spec.pitch();
        this.looping = spec.looping();
    }

    public AudioSpec getSpec() {
        return spec;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void play() {
        status = Status.PLAYING;
    }

    @Override
    public void pause() {
        if (status == Status.PLAYING) {
            status = Status.PAUSED;
        }
    }

    @Override
    public void stop() {
        status = Status.STOPPED;
        playbackTime = 0.0f;
    }

    @Override
    public void setGain(float gain) {
        this.gain = gain;
    }

    public float getGain() {
        return gain;
    }

    @Override
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    @Override
    public void setTimeOffset(float seconds) {
        playbackTime = seconds;
    }

    @Override
    public float getPlaybackTime() {
        return playbackTime;
    }

    @Override
    public float getDuration() {
        return duration;
    }
}