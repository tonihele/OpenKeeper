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
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.audio.AudioStream;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.sound.MentorType;
import toniarts.openkeeper.game.sound.MusicLevel;
import toniarts.openkeeper.game.sound.SequencedSoundStream;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundGroup;
import toniarts.openkeeper.game.sound.SoundSequencer;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;

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
    private AudioNode speechNode = null;
    private IKwdFile kwdFile;

    /**
     * Music group per level sound track, as in DK II (track 0 plays track 1).
     * The game refers to a track 2 (group 344) that does not exist in the data.
     */
    private static final int[] MUSIC_TRACKS = {GlobalType.MUSIC_TRACK_1.getId(), GlobalType.MUSIC_TRACK_1.getId(),
        344, GlobalType.MUSIC_TRACK_3.getId()};

    /**
     * The background sound layers currently playing. Each one is an endless
     * stream following a sequenced sound group: AMBIENCE(341),
     * OPTIONS_MUSIC(838), MUSIC(343, 345), ONE_SHOT_ATMOS(746)
     */
    private final List<AudioNode> backgroundNodes = new ArrayList<>();
    private final List<SequencedSoundStream> backgroundStreams = new ArrayList<>();
    private final MusicLevel musicLevel = new MusicLevel();
    private final Random random = new Random();
    private final Queue<Speech> speechQueue = new ArrayDeque<>();

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
                if (speechNode != null && speechNode.getStatus() != AudioSource.Status.Stopped) {
                    if (enabled) {
                        speechNode.play();
                    } else {
                        speechNode.pause();
                    }
                }

                for (AudioNode backgroundNode : backgroundNodes) {
                    if (backgroundNode.getStatus() != AudioSource.Status.Stopped) {
                        if (enabled) {
                            backgroundNode.play();
                        } else {
                            backgroundNode.pause();
                        }
                    }
                }
            });
        }
    }

    public void setKwdFile(IKwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    /**
     * Plays all the groups of a background category, replacing the current
     * background
     *
     * @param category the category
     */
    public void changeBackground(Background category) {
        SoundCategory sc = SoundsLoader.load(category.getName());
        if (sc == null) {
            logger.log(Level.WARNING, "Background category {0} not found", category.getName());
            return;
        }
        app.enqueue(() -> {
            stopBackground();
            for (SoundGroup group : sc.getGroups().values()) {
                startBackground(group, category == Background.MUSIC || category == Background.OPTIONS);
            }
        });
    }

    /**
     * Starts the in-game background of the current level: the level's music
     * track, the ambience and the atmospheric one shots. The music level starts
     * silent, so only the ambience is heard until the first fight.
     */
    public void startLevelBackground() {
        int track = kwdFile != null ? kwdFile.getGameLevel().getSoundTrack() : 0;
        app.enqueue(() -> {
            stopBackground();
            musicLevel.set(MusicLevel.SILENT);
            musicLevel.update();
            startBackground(GlobalCategory.MUSIC, track < MUSIC_TRACKS.length ? MUSIC_TRACKS[track] : 0, true);
            startBackground(GlobalCategory.AMBIENCE, GlobalType.AMBIENCE.getId(), false);
            startBackground(GlobalCategory.ONE_SHOT_ATMOS, GlobalType.ONE_SHOT_ATMOS.getId(), false);
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

    private void startBackground(String category, int groupId, boolean music) {
        SoundCategory sc = SoundsLoader.load(category);
        SoundGroup group = sc != null ? sc.getGroup(groupId) : null;
        if (group == null) {
            logger.log(Level.WARNING, "Background sound group {0} of category {1} not found", groupId, category);
            return;
        }
        startBackground(group, music);
    }

    private void startBackground(SoundGroup group, boolean music) {
        Settings settings = Main.getUserSettings();
        if (!settings.getBoolean(music ? Settings.Setting.MUSIC_ENABLED : Settings.Setting.SFX_ENABLED)
                || group.getFiles().isEmpty()) {
            return;
        }

        int channels = group.isLinked() ? 2 : 1;
        SequencedSoundStream in = new SequencedSoundStream(app.getAssetManager(),
                new SoundSequencer(group.getEntries(), random), musicLevel::getIntensity, channels);
        AudioStream stream = new AudioStream();
        stream.setupFormat(channels, 16, SequencedSoundStream.SAMPLE_RATE);
        stream.updateData(in, -1);

        AudioNode node = new AudioNode(stream, new AudioKey(group.getCategory().getName() + "/" + group.getId(), true));
        node.setLooping(false);
        node.setPositional(false);
        node.setVolume(settings.getFloat(Settings.Setting.MASTER_VOLUME)
                * settings.getFloat(music ? Settings.Setting.MUSIC_VOLUME : Settings.Setting.SFX_VOLUME));
        backgroundNodes.add(node);
        backgroundStreams.add(in);
        node.play();
    }

    private void stopBackground() {
        for (AudioNode node : backgroundNodes) {
            node.stop();
        }
        for (SequencedSoundStream in : backgroundStreams) {
            in.close();
        }
        backgroundNodes.clear();
        backgroundStreams.clear();
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

        try {
            SoundCategory sc = SoundsLoader.load(soundCategory, false);
            if (sc == null) {
                throw new RuntimeException("Sound category " + soundCategory + " not found");
            }

            String file = AssetsConverter.SOUNDS_FOLDER + File.separator
                    + sc.getGroup(speechId).getFiles().get(0).getFilename();
            speechQueue.add(new Speech(speechId, file, listener));
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to attach speech from category " + soundCategory + " with id " + speechId, e);
        }
    }

    private void playSpeech(Speech speech) {
        speechNode = new AudioNode(app.getAssetManager(), speech.file, DataType.Buffer);
        if (speechNode == null) {
            logger.log(Level.WARNING, "Audio file {0} not found", speech.file);
            return;
        }
        speechNode.setLooping(false);
        speechNode.setPositional(false);
        float volume = Main.getUserSettings().getFloat(Settings.Setting.MASTER_VOLUME)
                * Main.getUserSettings().getFloat(Settings.Setting.VOICE_VOLUME);
        speechNode.setVolume(volume);
        app.enqueue(() -> {
            speechNode.play();
            if (speech.listener != null) {
                speech.listener.onStart();
            }
        });
    }

    public void stopSpeech() {
        app.enqueue(() -> {
            if (speechNode != null) {
                speechNode.stop();
            }
        });
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }

        if (!speechQueue.isEmpty()) {
            if (speechNode == null || speechNode.getStatus() == AudioSource.Status.Stopped) {
                playSpeech(speechQueue.poll());
            }
        }

        musicLevel.update();

        super.update(tpf);
    }

    @Override
    public void cleanup() {
        stopBackground();

        super.cleanup();
    }

    /**
         * Simple wrapper for a speech to play
         */
        private record Speech(int speechId, String file, ISpeechListener listener) {

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
