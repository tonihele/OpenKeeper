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

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * {@link AudioBackend} on top of a jME {@link AudioNode}.
 * <p>
 * The listener is the jME application listener, which
 * {@code PlayerCameraState} already keeps in sync with the camera.
 */
public final class JmeAudioBackend implements AudioBackend {

    private static final float DEFAULT_REF_DISTANCE = 1.0f;

    private final AudioNode node;
    private final Supplier<Float> sourceHeight;

    public JmeAudioBackend(Application app, AudioSpec spec) {
        this(app.getAssetManager(), spec);
    }

    public JmeAudioBackend(AssetManager assetManager, AudioSpec spec) {
        this(assetManager, spec, () -> spec.y());
    }

    /**
     * @param sourceHeight height of the sound sources, the listener (camera) hovers high above the
     * ground, so a source on the ground would be heard from far away. Placing the sources at the height
     * of the listener makes the distance a ground distance
     */
    public JmeAudioBackend(AssetManager assetManager, AudioSpec spec, Supplier<Float> sourceHeight) {
        this.sourceHeight = sourceHeight;
        String path = spec.assetPath();
        if (path == null) {
            throw new IllegalArgumentException("No asset path");
        }

        try {
            // A cached stream is shared by every node of the clip and is not seekable, so a second
            // voice of a clip that has already ended once would fail to rewind it. Streams are not cached.
            node = new AudioNode(assetManager, path, spec.streamed(), false);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Failed to load " + path, ex);
        }

        node.setLooping(spec.looping());
        node.setPositional(spec.positional());
        node.setPitch(spec.pitch());
        node.setVolume(clampVolume(spec.gain()));
        if (spec.positional()) {
            node.setLocalTranslation(spec.x(), sourceHeight.get(), spec.z());
            node.setRefDistance(spec.refDistance() > 0 ? spec.refDistance() : DEFAULT_REF_DISTANCE);
            node.setMaxDistance(spec.maxDistance() > 0 ? spec.maxDistance() : 1000.0f);
        }
    }

    @Nullable
    public AudioNode getNode() {
        return node;
    }

    @Override
    public Status getStatus() {
        AudioSource.Status status = node.getStatus();
        switch (status) {
            case Playing:
                return Status.PLAYING;
            case Paused:
                return Status.PAUSED;
            default:
                return Status.STOPPED;
        }
    }

    @Override
    public void play() {
        node.play();
    }

    @Override
    public void pause() {
        node.pause();
    }

    @Override
    public void stop() {
        node.stop();
    }

    @Override
    public void setGain(float gain) {
        node.setVolume(clampVolume(gain));
    }

    @Override
    public void setPitch(float pitch) {
        node.setPitch(Math.max(0.01f, pitch));
    }

    @Override
    public void setPosition(float x, float y, float z) {
        node.setLocalTranslation(x, sourceHeight.get(), z);
        node.updateGeometricState();
    }

    @Override
    public void setLooping(boolean looping) {
        node.setLooping(looping);
    }

    @Override
    public void setTimeOffset(float seconds) {
        node.setTimeOffset(Math.max(0.0f, seconds));
    }

    @Override
    public float getPlaybackTime() {
        return node.getPlaybackTime();
    }

    @Override
    public float getDuration() {
        try {
            AudioData data = node.getAudioData();
            return data == null ? 0.0f : data.getDuration();
        } catch (RuntimeException ex) {
            return 0.0f;
        }
    }

    private static float clampVolume(float gain) {
        if (gain < 0.0f) {
            return 0.0f;
        }
        return gain > 1.0f ? 1.0f : gain;
    }
}