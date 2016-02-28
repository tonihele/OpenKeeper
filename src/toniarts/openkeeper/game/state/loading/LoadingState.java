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
package toniarts.openkeeper.game.state.loading;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.material.Material;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.lwjgl.opengl.Display;
import toniarts.openkeeper.Main;

/**
 * A base for different kind of loading screen states
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class LoadingState extends AbstractAppState {

    protected Main app;
    protected Node rootNode;
    protected AssetManager assetManager;
    protected AppStateManager stateManager;
    protected InputManager inputManager;
    protected ViewPort viewPort;
    private Geometry titleScreen;
    private boolean load = true;
    private Future loadFuture = null;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    protected int imageWidth;
    protected int imageHeight;

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();

        // Disable the cursor
        inputManager.setCursorVisible(false);

        // Load up the title screen
        setupTitleScreen();

        this.app.getGuiNode().attachChild(titleScreen);
    }

    /**
     * Get the background texture for the loading screen
     *
     * @return the loading screen texture
     */
    protected abstract Texture getLoadingScreenTexture();

    private void setupTitleScreen() {
        Texture tex = getLoadingScreenTexture();

        // Set full screen but obey the aspect ratio
        int imgWidth = tex.getImage().getWidth();
        int imgHeight = tex.getImage().getHeight();
        float imageRatio = imgWidth / (float) imgHeight;
        imageWidth = (int) (Display.getHeight() * imageRatio);
        imageHeight = (int) (Display.getWidth() / imageRatio);
        if (Display.getWidth() / (float) Display.getHeight() > imageRatio) {
            imageHeight = Display.getHeight();
        } else {
            imageWidth = Display.getWidth();
        }

        // The canvas
        titleScreen = new Geometry("TitleScreen", new Quad(imageWidth, imageHeight));
        titleScreen.setLocalTranslation((Display.getWidth() - imageWidth) / 2, (Display.getHeight() - imageHeight) / 2, 0);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        titleScreen.setMaterial(mat);
    }

    @Override
    public void update(float tpf) {
        if (load) {
            if (loadFuture == null) {

                // If we have not started loading yet, submit the Callable to the executor
                loadFuture = exec.submit(loadingCallable);
            }

            // Check if the execution on the other thread is done
            if (loadFuture.isDone()) {

                // Detach us
                stateManager.detach(this);

                load = false;
                onLoadComplete();
            }
        }
    }

    @Override
    public void cleanup() {
        exec.shutdownNow();

        // Remove the title screen
        if (titleScreen != null) {
            app.getGuiNode().detachChild(titleScreen);
        }

        super.cleanup();
    }
    private final Callable<Void> loadingCallable = new Callable<Void>() {
        @Override
        public Void call() {
            return onLoad();
        }
    };

    /**
     * Add your loading logic here, <b>do NOT</b> manipulate the scene from
     * here!
     *
     * @return void
     */
    abstract public Void onLoad();

    /**
     * Called when loading is complete
     */
    abstract public void onLoadComplete();
}
