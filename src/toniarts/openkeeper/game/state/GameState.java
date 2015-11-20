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
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import de.lessvoid.nifty.screen.Screen;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.PlayerManaControl;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.world.WorldHandler;

/**
 * The GAME state!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameState extends AbstractAppState {

    private Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private Screen screen;
    private Node worldNode;
    private String level;
    private KwdFile kwdFile;
    private WorldHandler worldHandler;
    private static final Logger logger = Logger.getLogger(GameState.class.getName());
    private BulletAppState bulletAppState;
    private float tick = 0;
    private PlayerManaControl manaControl;
    /**
     * Single use game states
     *
     * @param level the level to load
     */
    public GameState(String level) {
        this.level = level;
    }

    /**
     * Single use game states
     *
     * @param level the level to load
     */
    public GameState(KwdFile level) {
        this.kwdFile = level;
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();

        // Create physics state
        bulletAppState = new BulletAppState();
        this.stateManager.attach(bulletAppState);

        // Set up the loading screen
        SingleBarLoadingState loader = new SingleBarLoadingState() {
            @Override
            public Void onLoad() {

                try {

                    // Load the level data
                    if (level != null) {
                        kwdFile = new KwdFile(Main.getDkIIFolder(), new File(Main.getDkIIFolder().concat(AssetsConverter.MAPS_FOLDER.concat(level).concat(".kwd"))));
                    } else {
                        kwdFile.load();
                    }
                    setProgress(0.25f);

                    // Create the actual level
                    worldHandler = new WorldHandler(assetManager, kwdFile, bulletAppState) {
                        @Override
                        protected void updateProgress(int progress, int max) {
                            setProgress(0.25f + ((float) progress / max * 0.75f));
                        }
                    };
                    worldNode = worldHandler.getWorld();
                    manaControl = new PlayerManaControl((short) 3, worldHandler);
                    
                    setProgress(1.0f);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load the game!", e);
                }

                return null;
            }

            @Override
            public void onLoadComplete() {

                // Set the processors
                GameState.this.app.setViewProcessors();

                // Attach the world
                rootNode.attachChild(worldNode);

                // Enable player state
                stateManager.getState(PlayerState.class).setEnabled(true);
            }
        };
        stateManager.attach(loader);
    }

    @Override
    public void cleanup() {

        // Detach our map
        if (worldNode != null) {
            rootNode.detachChild(worldNode);
            worldNode = null;
        }

        // Physics away
        stateManager.detach(bulletAppState);

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        tick += tpf;
        if (tick >= 1) {
            if (manaControl != null) {
                manaControl.update();
                manaControl.updateManaFromTiles();
                manaControl.updateManaFromCreatures();
            }
            tick -= 1;
        }
        if (manaControl != null) {
            manaControl.updateManaGet();
            manaControl.updateManaLose();
        }
        super.update(tpf);
    }

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    public KwdFile getLevelData() {
        return kwdFile;
    }

    public WorldHandler getWorldHandler() {
        return worldHandler;
    }
    
    public PlayerManaControl getPlayerManaControl() {
        return this.manaControl;
    }
}
