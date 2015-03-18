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
import com.jme3.input.InputManager;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.io.File;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.world.MapLoader;

/**
 * The GAME state!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameState extends AbstractAppState implements ScreenController {

    private Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private Nifty nifty;
    private Screen screen;
    private Node worldNode;
    private NiftyJmeDisplay niftyDisplay;
    private final KwdFile kwdFile;

    public GameState(String level, AssetManager assetManager) {

        // Load the level data
        kwdFile = new KwdFile(Main.getDkIIFolder(), new File(Main.getDkIIFolder().concat("Data".concat(File.separator).concat("editor").concat(File.separator).concat("maps").concat(File.separator).concat(level).concat(".kwd"))));

        // Create the actual level
        worldNode = new Node("World");
        worldNode.attachChild(new MapLoader().load(assetManager, kwdFile));
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();

        // Enable the fly cam
        this.app.getFlyByCamera().setEnabled(true);
        this.app.getFlyByCamera().setDragToRotate(false);
        this.app.getFlyByCamera().setMoveSpeed(10);

        rootNode.attachChild(worldNode);

        // Init Nifty
        niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                this.app.getAudioRenderer(),
                this.app.getGuiViewPort());

        // Attach the nifty display to the gui view port as a processor
        this.app.getGuiViewPort().addProcessor(niftyDisplay);

        // Load the HUD
        niftyDisplay.getNifty().fromXml("Interface/GameHUD.xml", "hud", this);
    }

    @Override
    public void cleanup() {

        // Detach our map
        rootNode.detachChild(worldNode);

        super.cleanup();
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }
}
