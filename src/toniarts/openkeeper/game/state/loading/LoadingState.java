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
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private Node titleScreen;
    private final Thread loadingThread = new LoadingThread();
    protected int imageWidth;
    protected int imageHeight;

    private static final Logger LOGGER = Logger.getLogger(LoadingState.class.getName());

    public LoadingState(final Main app) {
        this.app = app;
        loadingThread.start();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = stateManager;
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();

        // Disable the cursor
        inputManager.setCursorVisible(false);

        // Load up the title screen
        setupTitleScreen();
    }

    /**
     * Gets the localized texture folder, some updated versions of the game come
     * also with Japanese loading screens
     *
     * @return loading screen texture folder, ready for asset key
     */
    protected String getLocalizedLoadingScreenTextureFolder() {
        Locale locale = Locale.getDefault();
        if (locale.getISO3Language().equals("jpn")) {
            return "Textures/LoadingScreen-Japanese/";
        }

        return "Textures/LoadingScreen/";
    }

    /**
     * Get the background texture for the loading screen
     *
     * @return the loading screen texture
     */
    protected abstract Texture getLoadingScreenTexture();

    private void setupTitleScreen() {
        Texture tex = getLoadingScreenTexture();
        titleScreen = new Node("LoadingScreenNode");

        // Set full screen but obey the aspect ratio
        int imgWidth = tex.getImage().getWidth();
        int imgHeight = tex.getImage().getHeight();
        float imageRatio = imgWidth / (float) imgHeight;
        int height = Main.getUserSettings().getAppSettings().getHeight();
        int width = Main.getUserSettings().getAppSettings().getWidth();
        imageWidth = (int) (height * imageRatio);
        imageHeight = (int) (width / imageRatio);
        if (width / (float) height > imageRatio) {
            imageHeight = height;
        } else {
            imageWidth = width;
        }

        // Since the loading screen might not fill the whole screen
        // and we might pop in the actual scene when we are done with this still in...
        // It looks ugly, so hide everything
        Geometry black = new Geometry("BlackCanvas", new Quad(width, height));
        black.setLocalTranslation(0, 0, 0);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Black);
        black.setMaterial(mat);
        titleScreen.attachChild(black);

        // The canvas
        Geometry title = new Geometry("TitleScreen", new Quad(imageWidth, imageHeight));
        title.setLocalTranslation((width - imageWidth) / 2, (height - imageHeight) / 2, 0);
        mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        title.setMaterial(mat);
        titleScreen.attachChild(title);

        this.app.getGuiNode().attachChild(titleScreen);
    }

    @Override
    public void update(float tpf) {

    }

    @Override
    public void cleanup() {
        if (loadingThread.isAlive()) {
            loadingThread.interrupt();
            try {
                loadingThread.join();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "Failed to wait for the thread to complete!", ex);
            }
        }

        // Remove the title screen
        if (titleScreen != null) {
            app.getGuiNode().detachChild(titleScreen);
        }

        super.cleanup();
    }

    /**
     * Add your loading logic here, <b>do NOT</b> manipulate the scene from
     * here!
     *
     * @return void
     */
    abstract public Void onLoad();

    /**
     * Called when loading is complete. Called in render thread.
     */
    abstract public void onLoadComplete();

    private class LoadingThread extends Thread {

        public LoadingThread() {
            super("Loading");
        }

        @Override
        public void run() {
            try {
                Callable<Void> loadingCallable = () -> onLoad();
                loadingCallable.call();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load!", ex);
            }

            // Enqueue to the render thread
            // 1. It is safe
            // 2. Things will start on render, not when app is minimized etc (depends on settings of course but in principle)
            app.enqueue(() -> {

                // Finally call the completion
                onLoadComplete();

                // Detach us
                app.getStateManager().detach(LoadingState.this);
            });
        }

    }
}
