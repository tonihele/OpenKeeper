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
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.sound.MentorType;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.game.sound.SoundGroup;
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
    private AudioNode backgroundNode = null;
    private IKwdFile kwdFile;

    /**
     * AMBIENCE(341) | OPTIONS_MUSIC(838) | MUSIC(343, 345) | maybe ONE_SHOT_ATMOS(746)
     */
    private final BackgroundState backgroundState = new BackgroundState("MUSIC");
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

                if (backgroundNode != null && backgroundNode.getStatus() != AudioSource.Status.Stopped) {
                    if (enabled) {
                        backgroundNode.play();
                    } else {
                        backgroundNode.pause();
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
            if (backgroundNode != null) {
                backgroundNode.stop();
            }
        });
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

    private void playBackground() {
        if (!Main.getUserSettings().getBoolean(Settings.Setting.MUSIC_ENABLED)) {
            return;
        }

        String file = AssetsConverter.SOUNDS_FOLDER + File.separator + backgroundState.getNext();

        backgroundNode = new AudioNode(app.getAssetManager(), file, DataType.Buffer);
        if (backgroundNode == null) {
            logger.log(Level.WARNING, "Audio file {0} not found", file);
            return;
        }
        backgroundNode.setLooping(false);
        backgroundNode.setPositional(false);
        float volume = Main.getUserSettings().getFloat(Settings.Setting.MASTER_VOLUME)
                * Main.getUserSettings().getFloat(Settings.Setting.MUSIC_VOLUME);
        backgroundNode.setVolume(volume);
        app.enqueue(() -> {
            backgroundNode.play();
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

        if (backgroundNode == null || backgroundNode.getStatus() == AudioSource.Status.Stopped) {
            playBackground();
        }

        super.update(tpf);
    }

    private static final class BackgroundState {

        private SoundCategory sc;

        private Iterator<SoundGroup> itGroup;
        private Iterator<SoundFile> itFile;

        public BackgroundState(String category) {
            this.setCategory(category);
        }

        public final synchronized void setCategory(String category) {
            this.sc = SoundsLoader.load(category);
            if (sc == null) {
                throw new RuntimeException("Category " + category + " does not exist");
            }

            if (this.sc.getGroups().isEmpty()) {
                throw new RuntimeException("We have no groups in category " + category);
            }

            int total = 0;
            for (SoundGroup group : this.sc.getGroups().values()) {
                total += group.getFiles().size();
            }
            if (total == 0) {
                throw new RuntimeException("We have no files in groups in category " + category);
            }

            itGroup = null;
            itFile = null;
        }

        public synchronized String getNext() {
            if (itGroup == null) {
                itGroup = sc.getGroups().values().iterator();
            }

            if (itFile == null) {
                if (itGroup.hasNext()) {
                    itFile = itGroup.next().getFiles().iterator();
                } else {
                    itGroup = null;
                    return this.getNext();
                }
            }

            if (itFile.hasNext()) {
                return itFile.next().getFilename();
            } else {
                itFile = null;
                return this.getNext();
            }
        }
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
