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
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;

/**
 * This state plays different sounds
 *
 * @author ArchDemon
 */
public class SoundState extends AbstractPauseAwareState {

    private Main app;
    private AppStateManager stateManager;
    private AudioNode speech = null;
    private AudioNode background = null;
    private KwdFile kwdFile;
    private final Queue<String> speechQueue = new ArrayDeque<>();
    private static final Logger LOGGER = Logger.getLogger(SoundState.class.getName());

    public SoundState(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

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
        return false;
    }

    /**
     * Plays mentor speeches for the current level. The speech will be put to a
     * queue and played in sequence
     *
     * @param speechId the speech ID in level resource bundle
     */
    public void attachLevelSpeech(int speechId) {
        String soundCategory = kwdFile.getGameLevel().getSoundCategory();
        attachSpeech(soundCategory, speechId);
    }

    /**
     * Plays general mentor speeches The speech will be put to a queue and
     * played in sequence
     *
     * @param speechId the speech ID in resource bundle
     */
    public void attachMentorSpeech(int speechId) {
        attachSpeech(SoundCategory.SPEECH_MENTOR, speechId);
    }

    private void attachSpeech(String soundCategory, int speechId) {
        try {
            SoundCategory sc = SoundsLoader.load(soundCategory, false);
            if (sc == null) {
                throw new RuntimeException("Sound category " + soundCategory + " not found");
            }

            String file = AssetsConverter.SOUNDS_FOLDER + File.separator
                    + sc.getGroup(speechId).getFiles().get(0).getFilename();
            speechQueue.add(file);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to attach speech from category " + soundCategory + " with id " + speechId, e);
        }
    }

    private void playSpeech(String file) {
        speech = new AudioNode(app.getAssetManager(), file, DataType.Buffer);
        if (speech == null) {
            LOGGER.log(Level.WARNING, "Audio file {0} not found", file);
            return;
        }
        speech.setLooping(false);
        speech.setPositional(false);
        app.enqueue(() -> {
            speech.play();
        });
    }

    public void stopSpeech() {
        if (speech != null) {
            speech.stop();
        }
    }

    private void playBackground() {
        String file = this.getRandomSoundFile();
        background = new AudioNode(app.getAssetManager(), file, DataType.Buffer);
        if (background == null) {
            LOGGER.log(Level.WARNING, "Audio file {0} not found", file);
            return;
        }
        background.setLooping(false);
        background.setPositional(false);
        background.setVolume(0.2f);
        app.enqueue(() -> {
            background.play();
        });
    }

    private String getRandomSoundFile() {
        /*
         * TODO need algorithm
         * 1pt1-001 - 1pt1-046
         * 1pt2-001 - 1pt2-035
         * 1pt3-001 - 1pt3-022
         * 1pt4-001 - 1pt4-013
         * 1seg-001 - 1seg-010
         * 3pt1-001 - 3pt1-079
         * 3pt2-001 - 3pt2-020
         * 3pt3-001 - 3pt3-018
         * 3pt4-001 - 3pt4-024
         */
        int first, second, third;
        Random random = new Random();
        while (true) {
            first = random.nextInt(2) + 1;
            switch (first) {
                case 1:
                    break;
                case 3:
                    break;
                default:
                    continue;
            }

            second = random.nextInt(3) + 1;
            if (first == 1 && second == 1) {
                third = random.nextInt(45) + 1;
            } else if (first == 1 && second == 2) {
                third = random.nextInt(34) + 1;
            } else if (first == 1 && second == 3) {
                third = random.nextInt(21) + 1;
            } else if (first == 1 && second == 4) {
                third = random.nextInt(12) + 1;
            } else if (first == 3 && second == 1) {
                third = random.nextInt(78) + 1;
            } else if (first == 3 && second == 2) {
                third = random.nextInt(19) + 1;
            } else if (first == 3 && second == 3) {
                third = random.nextInt(17) + 1;
            } else {
                third = random.nextInt(23) + 1;
            }

            final String formatted = String.format("Sounds/Global/Track_%d_%dHD/%dpt%d-%03d.mp2",
                    first, second, first, second, third);
            return ConversionUtils.getCanonicalAssetKey(formatted);
        }
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!speechQueue.isEmpty()) {
            if (speech == null || speech.getStatus() == AudioSource.Status.Stopped) {
                String speechFile = speechQueue.poll();
                playSpeech(speechFile);
            }
        }

        if (background == null || background.getStatus() == AudioSource.Status.Stopped) {
            playBackground();
        }
    }

}
