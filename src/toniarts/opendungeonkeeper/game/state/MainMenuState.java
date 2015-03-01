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
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.io.File;
import java.util.regex.Pattern;
import toniarts.opendungeonkeeper.Main;
import toniarts.opendungeonkeeper.cinematics.CameraSweepData;
import toniarts.opendungeonkeeper.cinematics.CameraSweepDataLoader;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.map.KwdFile;
import toniarts.opendungeonkeeper.world.MapLoader;

/**
 * The main menu state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MainMenuState extends AbstractAppState implements ScreenController {

    private Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private Nifty nifty;
    private Screen screen;
    private Node menuNode;
    private final KwdFile kwdFile;

    public MainMenuState() {

        // Load the 3D Front end
        kwdFile = new KwdFile(Main.getDkIIFolder(), new File(Main.getDkIIFolder().concat("Data".concat(File.separator).concat("editor").concat(File.separator).concat("maps").concat(File.separator).concat("FrontEnd3DLevel.kwd"))));
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

        // Disable the fly cam
        this.app.getFlyByCamera().setEnabled(false);
        this.app.getFlyByCamera().setDragToRotate(true);

        // Attach the 3D Front end
        if (menuNode == null) {
            menuNode = new Node("Main menu");
            menuNode.attachChild(new MapLoader().load(assetManager, kwdFile));

            // Without light it is invisible
            AmbientLight al = new AmbientLight();
            al.setColor(ColorRGBA.White.multLocal(5f));
            menuNode.addLight(al);
        }
        rootNode.attachChild(menuNode);

        // Set the camera position
        // TODO: an utility, this is map start position really, so general
        Vector3f startLocation = new Vector3f((3 - MapLoader.TILE_WIDTH / 2) * MapLoader.TILE_WIDTH, 0f, (12 - MapLoader.TILE_WIDTH * 0.35f) * MapLoader.TILE_HEIGHT);
        CameraSweepData csd = (CameraSweepData) assetManager.loadAsset(AssetsConverter.PATHS_FOLDER.concat("\\").replaceAll(Pattern.quote(File.separator), "/").concat("EnginePath250".concat(".").concat(CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION)));
        this.app.getCamera().setLocation(startLocation.addLocal(csd.getEntries().get(0).getPosition()));
    }

    @Override
    public void cleanup() {

        // Detach our start menu
        rootNode.detachChild(menuNode);

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

    public void quitGame() {
        app.stop();
    }
}
