/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.audio.AudioStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.sound.MentorType;
import toniarts.openkeeper.game.sound.MusicLevel;
import toniarts.openkeeper.game.sound.SequencedSoundStream;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundEvent;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.SoundSequencer;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * This state plays different sounds
 *
 * @author ArchDemon
 */
public final class SoundState extends AbstractPauseAwareState {

    public enum Background {
        AMBIENCE("AMBIENCE"),
        OPTIONS("OPTIONS"),
        MUSIC("MUSIC"),
        ONE_SHOT_ATMOS("ONE_SHOT_ATMOS");

        private final String name;

        private Background(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    private static final Logger logger = System.getLogger(SoundState.class.getName());

    private Main app;
    private AppStateManager stateManager;
    private int speechHandle = SoundEngine.NO_HANDLE;
    private int backgroundHandle = SoundEngine.NO_HANDLE;
    private IKwdFile kwdFile;

    /**
     * AMBIENCE(341) | OPTIONS_MUSIC(838) | MUSIC(343, 345) | maybe ONE_SHOT_ATMOS(746)
     */
    private final BackgroundState backgroundState = new BackgroundState("MUSIC");
    private final Queue<Speech> speechQueue = new ArrayDeque<>();

    /**
     * Music event per level sound track, as in DK II (track 0 plays track 1).
     * The game refers to a track 2 (event 344) that does not exist in the data.
     */
    private static final int[] MUSIC_TRACKS = {GlobalType.MUSIC_TRACK_1.getId(), GlobalType.MUSIC_TRACK_1.getId(),
        344, GlobalType.MUSIC_TRACK_3.getId()};

    /**
     * The in-game music: the level's track, played gaplessly and driven by the
     * music level
     */
    private final MusicLevel musicLevel = new MusicLevel();
    private boolean levelMusic;
    private AudioNode musicNode;
    private SequencedSoundStream musicStream;

    public SoundState(boolean enabled) {
        this.setEnabled(enabled);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.stateManager = stateManager;
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    @Override
    public final void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (isInitialized()) {
            app.enqueue(() -> {
                SoundManager.setPaused(!enabled);
                if (speechHandle != SoundEngine.NO_HANDLE) {
                    SoundManager.stopSound(speechHandle);
                    speechHandle = SoundEngine.NO_HANDLE;
                }
                if (backgroundHandle != SoundEngine.NO_HANDLE) {
                    SoundManager.stopSound(backgroundHandle);
                    backgroundHandle = SoundEngine.NO_HANDLE;
                }
                if (musicNode != null) {
                    if (enabled) {
                        musicNode.play();
                    } else {
                        musicNode.pause();
                    }
                }
            });
        }
    }

    public void setKwdFile(IKwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    public void changeBackground(Background category) {
        backgroundState.setCategory(category.getName());
        app.enqueue(() -> {
            levelMusic = false;
            stopMusic();
            if (backgroundHandle != SoundEngine.NO_HANDLE) {
                SoundManager.stopSound(backgroundHandle);
                backgroundHandle = SoundEngine.NO_HANDLE;
            }
        });
    }

    /**
     * Starts the music of the current level, replacing the background. The
     * music level starts silent, so the music begins with the first fight.
     */
    public void startLevelBackground() {
        int track = kwdFile != null ? kwdFile.getGameLevel().getSoundTrack() : 0;
        app.enqueue(() -> {
            levelMusic = true;
            if (backgroundHandle != SoundEngine.NO_HANDLE) {
                SoundManager.stopSound(backgroundHandle);
                backgroundHandle = SoundEngine.NO_HANDLE;
            }
            stopMusic();
            musicLevel.set(MusicLevel.SILENT);
            musicLevel.update();
            startMusic(track < MUSIC_TRACKS.length ? MUSIC_TRACKS[track] : 0);
        });
    }

    /**
     * The music level drives the music and ambience intensity. Request levels
     * every update with {@link MusicLevel#raise(int)}.
     *
     * @return the music level
     */
    public MusicLevel getMusicLevel() {
        return musicLevel;
    }

    private void startMusic(int eventId) {
        if (!Main.getUserSettings().getBoolean(Settings.Setting.MUSIC_ENABLED)) {
            return;
        }
        SoundCategory category = SoundManager.loadCategory(GlobalCategory.MUSIC);
        SoundEvent event = category != null ? category.getEvent(eventId) : null;
        if (event == null) {
            logger.log(Level.WARNING, "Music event {0} not found", eventId);
            return;
        }

        musicStream = new SequencedSoundStream(app.getAssetManager(), new SoundSequencer(event),
                musicLevel::getIntensity, 2);
        AudioStream stream = new AudioStream();
        stream.setupFormat(2, 16, SequencedSoundStream.SAMPLE_RATE);
        stream.updateData(musicStream, -1);
        musicNode = new AudioNode(stream, new AudioKey(GlobalCategory.MUSIC + "/" + eventId, true));
        musicNode.setPositional(false);
        musicNode.setLooping(false);
        musicNode.setVolume(getMusicGain());
        musicNode.play();
    }

    private void stopMusic() {
        if (musicNode != null) {
            musicNode.stop();
            musicNode = null;
        }
        if (musicStream != null) {
            musicStream.close();
            musicStream = null;
        }
    }

    private static float getMusicGain() {
        SoundEngine engine = SoundManager.getEngine();
        if (engine == null) {
            return 1.0f;
        }
        return engine.getMasterVolume() / 100f * engine.getChannelVolume(SoundManager.CHANNEL_MUSIC) / 100f;
    }

    /**
     * Plays mentor speeches for the current level. The speech will be put to a queue and played in
     * sequence
     *
     * @param speechId the speech ID in level resource bundle
     * @param listener can be {@code null}, allows you to listen when the speech starts playing
     */
    public void attachLevelSpeech(int speechId, ISpeechListener listener) {
        String soundCategory = kwdFile.getGameLevel().getSoundCategory();
        attachSpeech(soundCategory, speechId, listener);
    }

    /**
     * Plays general mentor speeches The speech will be put to a queue and played in sequence
     *
     * @param type the speech
     * @param listener can be {@code null}, allows you to listen when the speech starts playing
     */
    public void attachMentorSpeech(MentorType type, ISpeechListener listener) {
        attachSpeech(SoundCategory.SPEECH_MENTOR, type.getId(), listener);
    }

    private void attachSpeech(String soundCategory, int speechId, ISpeechListener listener) {
        if (!Main.getUserSettings().getBoolean(Settings.Setting.VOICE_ENABLED)) {
            return;
        }

        if (SoundCategory.load(soundCategory, false) == null) {
            logger.log(Level.WARNING, "Sound category {0} not found", soundCategory);
            return;
        }

        speechQueue.add(new Speech(soundCategory, speechId, listener));
    }

    private void playSpeech(Speech speech) {
        speechHandle = SoundManager.playSound(speech.soundCategory(), speech.speechId());
        if (speechHandle == SoundEngine.NO_HANDLE) {
            logger.log(Level.WARNING, "Failed to attach speech from category {0} with id {1}",
                    new Object[]{speech.soundCategory(), speech.speechId()});
            return;
        }
        if (speech.listener != null) {
            speech.listener.onStart();
        }
    }

    public void stopSpeech() {
        app.enqueue(() -> {
            if (speechHandle != SoundEngine.NO_HANDLE) {
                SoundManager.stopSound(speechHandle);
                speechHandle = SoundEngine.NO_HANDLE;
            }
        });
    }

    private void playBackground() {
        if (!Main.getUserSettings().getBoolean(Settings.Setting.MUSIC_ENABLED)) {
            return;
        }

        backgroundHandle = SoundManager.playSound(backgroundState.getCategory(), backgroundState.getNext());
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }

        if (!speechQueue.isEmpty()) {
            if (speechHandle == SoundEngine.NO_HANDLE || !SoundManager.isPlaying(speechHandle)) {
                speechHandle = SoundEngine.NO_HANDLE;
                playSpeech(speechQueue.poll());
            }
        }

        musicLevel.update();
        if (levelMusic) {
            if (musicNode != null && musicNode.getStatus() == AudioSource.Status.Playing) {
                musicNode.setVolume(getMusicGain());
            }
        } else if (backgroundHandle == SoundEngine.NO_HANDLE || !SoundManager.isPlaying(backgroundHandle)) {
            backgroundHandle = SoundEngine.NO_HANDLE;
            playBackground();
        }

        super.update(tpf);
    }

    @Override
    public void cleanup() {
        stopMusic();

        super.cleanup();
    }

    private static final class BackgroundState {

        private String category;
        private SoundCategory sc;
        private Iterator<SoundEvent> itEvent;

        public BackgroundState(String category) {
            this.setCategory(category);
        }

        public final synchronized void setCategory(String category) {
            this.category = category;
            this.sc = SoundCategory.load(category, true);
            if (sc == null) {
                throw new RuntimeException("Category " + category + " does not exist");
            }

            if (this.sc.getEvents().isEmpty()) {
                throw new RuntimeException("We have no events in category " + category);
            }

            itEvent = null;
        }

        public synchronized String getCategory() {
            return category;
        }

        /**
         * @return the next event to play, cycling through the category
         */
        public synchronized int getNext() {
            if (itEvent == null || !itEvent.hasNext()) {
                itEvent = sc.getEvents().values().iterator();
            }
            return itEvent.next().getId();
        }
    }

    /**
         * Simple wrapper for a speech to play
         */
    private record Speech(String soundCategory, int speechId, ISpeechListener listener) {

    }

    /**
     * A simple listener for getting notified when we actually play the queued speech. If we need
     * more, we might need to use the Java native audio playing. There are already existing
     * listeners unlike JME.
     */
    public interface ISpeechListener {

        void onStart();

    }

}
