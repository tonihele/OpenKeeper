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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.MainMenuScreenController.Cutscene;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.video.MovieState;

import static toniarts.openkeeper.Main.getDkIIFolder;

/**
 * Plays the level-won cutscene sequence: the mentor speech is played over the black screen showing
 * the movie name (see the "cutscene" screen), and once the speech has finished, the actual cutscene
 * movie is played. Calls back once everything has finished so the caller can move on (e.g. to the
 * debriefing screen).
 *
 * @author OpenKeeper
 */
public final class CutsceneState extends AbstractAppState {

    private static final String CUTSCENE_SPEECH_URL = "Sounds/speech_mentor/speech_mentorHD/misc%03d.mp2";

    /**
     * How long to keep showing the movie name after the speech has finished, so the player has time
     * to read it before the movie starts.
     */
    private static final float POST_SPEECH_READING_DELAY = 2f;

    private static final Logger logger = System.getLogger(CutsceneState.class.getName());

    private final Cutscene cutscene;
    private final Runnable onFinished;

    private Main app;
    private AppStateManager stateManager;
    private AudioNode speechNode;
    private boolean speechFinished = false;
    private float readingDelay = 0f;
    private boolean moviePlaying = false;
    private boolean finished = false;

    public CutsceneState(Cutscene cutscene, Runnable onFinished) {
        this.cutscene = cutscene;
        this.onFinished = onFinished;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.stateManager = stateManager;

        // No mouse cursor while the cutscene (speech + movie) is playing
        this.app.getInputManager().setCursorVisible(false);

        if (cutscene.speechId != null && !cutscene.speechId.isEmpty()) {
            String speechFile = AssetUtils.getCanonicalAssetKey(
                    String.format(CUTSCENE_SPEECH_URL, Integer.parseInt(cutscene.speechId)));
            speechNode = new AudioNode(app.getAssetManager(), speechFile, AudioData.DataType.Buffer);
            speechNode.setLooping(false);
            speechNode.setPositional(false);
            speechNode.setDirectional(false);
            speechNode.play();
        }
    }

    @Override
    public void update(float tpf) {
        if (moviePlaying) {
            return;
        }

        if (speechNode == null) {
            playMovie();
            return;
        }

        if (!speechFinished) {
            if (speechNode.getStatus() == AudioSource.Status.Stopped) {
                speechFinished = true;
            }
            return;
        }

        // Give the player a moment to read the movie name before the movie starts
        readingDelay += tpf;
        if (readingDelay >= POST_SPEECH_READING_DELAY) {
            playMovie();
        }
    }

    private void playMovie() {
        moviePlaying = true;
        try {
            MovieState movieState = new MovieState(getDkIIFolder() + PathUtils.DKII_MOVIES_FOLDER + cutscene.click + ".TGQ") {
                @Override
                protected void onPlayingEnd() {
                    finish();
                }
            };
            stateManager.attach(movieState);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to play cutscene movie " + cutscene.click, e);
            finish();
        }
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;

        // The movie signals its end from its own audio playback thread, so make sure we detach
        // and hand off to the callback (which drives Nifty screen changes) on the render thread
        app.enqueue(() -> {
            app.getInputManager().setCursorVisible(true);
            stateManager.detach(this);
            onFinished.run();
            return null;
        });
    }

    @Override
    public void cleanup() {
        if (speechNode != null) {
            speechNode.stop();
        }
        super.cleanup();
    }
}
