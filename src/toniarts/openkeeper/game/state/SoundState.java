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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;

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
    private final Queue<String> speechQueue = new LinkedList<>();
    private static final Logger logger = Logger.getLogger(SoundState.class.getName());

    public SoundState() {
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
     * Plays mentor speeches for the current level
     *
     * @param speechId
     */
    public void attachLevelSpeech(int speechId) {
        String file = String.format("Sounds/%s/lvlspe%02d.mp2", stateManager.getState(GameState.class).getLevelData().getGameLevel().getSpeechStr().toLowerCase(), speechId);
        speechQueue.add(file);
    }

    /**
     * Plays general mentor speeches
     *
     * @param audioFile Name of the audio file in the speech_mentor folder,
     * without extension!
     */
    public void attachMentorSpeech(String audioFile) {
        String file = String.format("Sounds/speech_mentor/%s.mp2", audioFile);
        speechQueue.add(file);
    }

    private void playSpeech(String file) {
        speech = new AudioNode(app.getAssetManager(), file, DataType.Buffer);
        if (speech == null) {
            logger.log(Level.WARNING, "Audio file {0} not found", file);
            return;
        }
        speech.setLooping(false);
        speech.setPositional(false);
        speech.play();
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
            logger.log(Level.WARNING, "Audio file {0} not found", file);
            return;
        }
        background.setLooping(false);
        background.setPositional(false);
        background.setVolume(0.2f);
        background.play();
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

            return String.format("Sounds/Global/%dpt%d-%03d.mp2", first, second, third);
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
