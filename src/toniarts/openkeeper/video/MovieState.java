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
package toniarts.openkeeper.video;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import java.io.File;
import org.lwjgl.opengl.Display;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.video.tgq.TgqFrame;

/**
 * AppState for watching TGQ movies, very simple, just create and attach
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class MovieState extends AbstractAppState {

    private final String movie;
    private Main app;
    private MovieMaterial movieMaterial;
    private Geometry movieScreen;
    private TgqPlayer player;

    public MovieState(String movie) {
        this.movie = movie;
    }

    @Override
    public void initialize(AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;

        // Create the canvas
        boolean squareScreen = ((0f + Display.getWidth()) / Display.getHeight()) < 1.6f;
        movieMaterial = new MovieMaterial(app, squareScreen ? false : true);
        movieMaterial.setLetterboxColor(ColorRGBA.Black);
        movieScreen = new Geometry("MovieScreen", new Quad(Display.getWidth(), Display.getHeight()));
        movieScreen.setMaterial(movieMaterial.getMaterial());
        this.app.getGuiNode().attachChild(movieScreen);

        // Create the player
        player = new TgqPlayer(new File(movie)) {
            @Override
            protected void onPlayingEnd() {
                app.getStateManager().detach(MovieState.this);
                MovieState.this.onPlayingEnd();
            }

            @Override
            protected void onNewVideoFrame(TgqFrame frame) {
                movieMaterial.videoFrameUpdated(frame);
            }
        };
        player.play();
    }

    /**
     * Called when the playing has finished, you probably want to move on with
     * your life
     */
    protected abstract void onPlayingEnd();

    @Override
    public void update(float tpf) {
        movieMaterial.update(tpf);
    }

    @Override
    public void cleanup() {

        // Make sure the player is stopped
        if (player != null) {
            player.stop();
        }

        // Dispose the movie material
        if (movieMaterial != null) {
            movieMaterial.dispose();
        }

        // Detach the canvas
        if (movieScreen != null) {
            app.getGuiNode().detachChild(movieScreen);
        }

        super.cleanup();
    }
}
