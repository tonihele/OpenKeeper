/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.io.File;
import toniarts.opendungeonkeeper.Main;
import toniarts.opendungeonkeeper.tools.convert.map.KwdFile;
import toniarts.opendungeonkeeper.world.MapLoader;

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
    private final KwdFile kwdFile;

    public GameState(String level, AssetManager assetManager) {

        // Load the level data
        kwdFile = new KwdFile(Main.getDkIIFolder(), new File(Main.getDkIIFolder().concat("Data".concat(File.separator).concat("editor").concat(File.separator).concat("maps").concat(File.separator).concat(level).concat(".kwd"))));

        // Create the actual level
        worldNode = new Node("World");
        worldNode.attachChild(new MapLoader().load(assetManager, kwdFile));

        // Add some ambient light to it
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.multLocal(5f));
        worldNode.addLight(al);
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
