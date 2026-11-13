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
import com.jme3.math.Vector3f;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * {@link AudioBackend} on top of a jME {@link AudioNode}.
 * <p>
 * The listener is the jME application listener, which
 * {@code PlayerCameraState} already keeps in sync with the camera.
 */
public final class JmeAudioBackend implements AudioBackend {

    private final AudioNode node;
    private final Supplier<Vector3f> listenerPosition;
    private final AudioSpec spec;
    private float dryGain;

    public JmeAudioBackend(Application app, AudioSpec spec) {
        this(app.getAssetManager(), spec, () -> app.getListener().getLocation());
    }

    public JmeAudioBackend(AssetManager assetManager, AudioSpec spec) {
        this(assetManager, spec, () -> new Vector3f(0, spec.y(), 0));
    }

    /**
     * @param listenerPosition current listener position; sources follow its height so
     * top-down camera height does not increase their horizontal distance
     */
    public JmeAudioBackend(AssetManager assetManager, AudioSpec spec, Supplier<Vector3f> listenerPosition) {
        this(loadNode(assetManager, spec), spec, listenerPosition);
    }

    JmeAudioBackend(AudioNode node, AudioSpec spec, Supplier<Vector3f> listenerPosition) {
        this.node = node;
        this.spec = spec;
        this.listenerPosition = listenerPosition;
        node.setLooping(spec.looping());
        node.setPositional(spec.positional());
        node.setPitch(spec.pitch());
        if (spec.positional()) {
            node.setLocalTranslation(spec.x(), listenerPosition.get().y, spec.z());
            // AudioNode cannot set AL_ROLLOFF_FACTOR. Calculate QMixer's curve here;
            // equal, large distance bounds keep OpenAL's own distance gain at one.
            // The source remains positional for OpenAL panning / HRTF.
            node.setRefDistance(Float.MAX_VALUE);
            node.setMaxDistance(Float.MAX_VALUE);
        }
        setGain(spec.gain());
    }

    private static AudioNode loadNode(AssetManager assetManager, AudioSpec spec) {
        if (spec.assetPath() == null) {
            throw new IllegalArgumentException("No asset path");
        }
        try {
            // Streams must not be shared: an ended cached stream cannot rewind.
            return new AudioNode(assetManager, spec.assetPath(), spec.streamed(), false);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Failed to load " + spec.assetPath(), ex);
        }
    }

    private void updateGain() {
        float distance = 0;
        if (spec.positional()) {
            Vector3f listener = listenerPosition.get();
            Vector3f source = node.getLocalTranslation();
            node.setLocalTranslation(source.x, listener.y, source.z);
            float dx = source.x - listener.x;
            float dz = source.z - listener.z;
            distance = (float) Math.sqrt(dx * dx + dz * dz);
        }
        node.setVolume(dryGain * spec.distanceGain(distance));
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
        dryGain = clampVolume(gain);
        updateGain();
    }

    @Override
    public void setPitch(float pitch) {
        node.setPitch(Math.max(AudioSpec.MIN_PITCH, Math.min(AudioSpec.MAX_PITCH, pitch)));
    }

    @Override
    public void setPosition(float x, float y, float z) {
        node.setLocalTranslation(x, listenerPosition.get().y, z);
        updateGain();
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