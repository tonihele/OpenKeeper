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
import java.util.Locale;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.Point;

/**
 * Static entry point of the sound system, used by everything that wants to make
 * noise without knowing about the engine.
 */
public final class SoundManager {

    /**
     * The volume channels a category can be affected by, taken from the channel
     * mask of the category's {@code SFX.map} entry.
     */
    public static final int CHANNEL_MASTER = 0;
    public static final int CHANNEL_MUSIC = 1;
    public static final int CHANNEL_SPEECH = 2;
    public static final int CHANNEL_EFFECTS = 3;

    @Nullable
    private static SoundEngine engine;

    private SoundManager() {
    }

    /**
     * Creates the engine with a jME backed audio device.
     */
    public static void init(Application app) {
        engine = new SoundEngine(spec -> new JmeAudioBackend(app.getAssetManager(), spec,
                () -> app.getListener().getLocation().y));
    }

    /**
     * Creates the engine with an arbitrary audio device, mainly for tests.
     */
    public static void init(AudioBackend.AudioBackendFactory factory) {
        engine = new SoundEngine(factory);
    }

    public static void shutdown() {
        if (engine != null) {
            engine.stopAll();
        }
        engine = null;
    }

    @Nullable
    public static SoundEngine getEngine() {
        return engine;
    }

    public static boolean isInitialized() {
        return engine != null;
    }

    /**
     * Plays a sound, resolving the category and the event through the sound
     * data.
     *
     * @return a generation checked handle, {@link SoundEngine#NO_HANDLE} if
     * nothing was played
     */
    public static int playSound(String category, int eventId) {
        return playSound(category, eventId, null, -1);
    }

    /**
     * Plays a sound of an event that is identified by an {@link IIndexable},
     * such as {@code GlobalType}.
     */
    public static int playSound(String category, IIndexable event) {
        return playSound(category, event.getId(), null, -1);
    }

    /**
     * Plays a sound at a world position, i.e. a 3D sound.
     */
    public static int playSound(String category, int eventId, Point position) {
        return playSound(category, eventId, position, -1);
    }

    /**
     * Plays a sound of an {@link IIndexable} event at a world position.
     */
    public static int playSound(String category, IIndexable event, Point position) {
        return playSound(category, event.getId(), position, -1);
    }

    public static int playSound(String category, int eventId, @Nullable Point position, int property) {
        SoundEngine current = engine;
        if (current == null) {
            return SoundEngine.NO_HANDLE;
        }
        SoundCategory soundCategory = loadCategory(category);
        if (soundCategory == null) {
            return SoundEngine.NO_HANDLE;
        }
        // speech playback disables the slow frame throttle around its own call
        boolean throttled = current.isThrottleEnabled();
        current.setThrottleEnabled(throttled && !isSpeech(soundCategory));
        try {
            return current.play(soundCategory, eventId, position, property);
        } finally {
            current.setThrottleEnabled(throttled);
        }
    }

    private static boolean isSpeech(SoundCategory category) {
        return category.getName().toUpperCase(Locale.ROOT).startsWith(SoundCategory.SPEECH_PREFIX);
    }

    /**
     * @return the asset path of a random clip of the event, for callers that
     * drive their own player, such as Nifty
     */
    @Nullable
    public static String getSoundAssetPath(String category, int eventId) {
        SoundCategory soundCategory = loadCategory(category);
        if (soundCategory == null) {
            return null;
        }
        SoundEventGroup group = selectGroup(soundCategory, eventId);
        if (group == null) {
            return null;
        }
        SoundSample sample = group.pickSample();
        String path = sample == null ? null : sample.getAssetPath();
        return path == null ? null : AssetsConverter.SOUNDS_FOLDER + "/" + path;
    }

    /**
     * @return the asset path of a random clip of the event, for callers that
     * drive their own player, such as Nifty
     */
    @Nullable
    public static String getSoundAssetPath(String category, IIndexable event) {
        return getSoundAssetPath(category, event.getId());
    }

    /**
     * Plays a single clip of a sound file, ignoring the event rules.
     */
    public static void playFile(SoundFile file) {
        SoundEngine current = engine;
        if (current != null && file != null) {
            current.playClip(AssetsConverter.SOUNDS_FOLDER + "/" + file.getFilename());
        }
    }

    /**
     * Loads a category, looking into the global sound folder first and into the
     * category folder of the non global sounds afterwards.
 */
    @Nullable
    public static SoundCategory loadCategory(String category) {
        if (category == null || category.isEmpty()) {
            return null;
        }

        // speech categories live in their own folders, do not even look into the global one
        boolean local = category.toUpperCase(Locale.ROOT).startsWith(SoundCategory.SPEECH_PREFIX);
        SoundCategory result = SoundCategory.load(category, !local);
        if (result == null) {
            result = SoundCategory.load(category, local);
        }
        return result;
    }

    @Nullable
    private static SoundEventGroup selectGroup(SoundCategory category, int eventId) {
        SoundEvent event = category.getEvent(eventId);
        if (event == null || event.getGroups().isEmpty()) {
            return null;
        }
        return event.getGroup(1);
    }

    public static void stopSound(int handle) {
        if (engine != null) {
            engine.stop(handle);
        }
    }

    public static void stopAll(String category, int eventId) {
        SoundEngine current = engine;
        SoundCategory soundCategory = current == null ? null : loadCategory(category);
        if (current != null && soundCategory != null) {
            current.stopAll(soundCategory, eventId);
        }
    }

    public static void stopAll() {
        if (engine != null) {
            engine.stopAll();
        }
    }

    public static void tick() {
        if (engine != null) {
            engine.tick();
        }
    }

    public static void setListener(Point position) {
        if (engine != null) {
            engine.setListener(position);
        }
    }

    public static void setPaused(boolean paused) {
        if (engine != null) {
            engine.setPaused(paused);
        }
    }

    public static boolean isPlaying(int handle) {
        return engine != null && engine.isPlaying(handle);
    }

    /**
     * Applies the volume settings of the game, i.e. 0..1 percentages.
     */
    public static void applyVolumes(float master, float music, float speech, float effects) {
        SoundEngine current = engine;
        if (current == null) {
            return;
        }
        current.setMasterVolume(percent(master));
        current.setChannelVolume(CHANNEL_MUSIC, percent(music));
        current.setChannelVolume(CHANNEL_SPEECH, percent(speech));
        current.setChannelVolume(CHANNEL_EFFECTS, percent(effects));
    }

    private static int percent(float value) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 100.0f);
    }
}