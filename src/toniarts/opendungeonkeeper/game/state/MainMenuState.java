/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.awt.Point;
import java.io.File;
import java.util.regex.Pattern;
import toniarts.opendungeonkeeper.Main;
import toniarts.opendungeonkeeper.cinematics.CameraSweepData;
import toniarts.opendungeonkeeper.cinematics.CameraSweepDataLoader;
import toniarts.opendungeonkeeper.cinematics.Cinematic;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;
import toniarts.opendungeonkeeper.tools.convert.map.KwdFile;
import toniarts.opendungeonkeeper.world.MapLoader;
import toniarts.opendungeonkeeper.world.room.control.FrontEndLevelControl;

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
    private final MouseEventListener mouseListener = new MouseEventListener(this);

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
        Vector3f startLocation = new Vector3f((3 - MapLoader.TILE_WIDTH / 2) * MapLoader.TILE_WIDTH, 0, (12 - MapLoader.TILE_WIDTH * 0.35f) * MapLoader.TILE_HEIGHT);
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
        if ("selectCampaignLevel".equals(nifty.getCurrentScreen().getScreenId())) {
            inputManager.addRawInputListener(mouseListener);
        }
    }

    @Override
    public void onEndScreen() {
        if ("selectCampaignLevel".equals(nifty.getCurrentScreen().getScreenId())) {
            inputManager.removeRawInputListener(mouseListener);
        }
    }

    public void goToScreen(String nextScreen) {
        nifty.gotoScreen(nextScreen);  // Switch to another screen
    }

    public void continueCampaign() {
        doTransitionAndGoToScreen("EnginePath251", "selectCampaignLevel");
    }

    public void cancelCampaign() {
        doTransitionAndGoToScreen("EnginePath252", "singlePlayer");
    }

    private void doTransitionAndGoToScreen(String transition, final String screen) {

        // Remove the current screen
        nifty.gotoScreen(null);

        // Do cinematic transition
        Cinematic c = new Cinematic(assetManager, app.getCamera(), new Point(3, 12), transition, menuNode);
        c.addListener(new CinematicEventListener() {
            @Override
            public void onPlay(CinematicEvent cinematic) {
            }

            @Override
            public void onPause(CinematicEvent cinematic) {
            }

            @Override
            public void onStop(CinematicEvent cinematic) {
                nifty.gotoScreen(screen);
            }
        });
        stateManager.attach(c);
        c.play();
    }

    public void quitGame() {
        app.stop();
    }

    /**
     * This is for the level pick up
     */
    private static class MouseEventListener implements RawInputListener {

        private final MainMenuState mainMenuState;
        private FrontEndLevelControl currentControl;

        public MouseEventListener(MainMenuState mainMenuState) {
            this.mainMenuState = mainMenuState;
        }

        @Override
        public void beginInput() {
        }

        @Override
        public void endInput() {
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {

            // See if we hit a map
            CollisionResults results = new CollisionResults();

            // Convert screen click to 3d position
            Vector3f click3d = mainMenuState.app.getCamera().getWorldCoordinates(
                    new Vector2f(evt.getX(), evt.getY()), 0f);
            Vector3f dir = mainMenuState.app.getCamera().getWorldCoordinates(
                    new Vector2f(evt.getX(), evt.getY()), 1f).subtractLocal(click3d);

            // Aim the ray from the clicked spot forwards
            Ray ray = new Ray(click3d, dir);

            // Collect intersections between ray and all nodes in results list
            mainMenuState.menuNode.collideWith(ray, results);

            // See the results so we see what is going on
            for (int i = 0; i < results.size(); i++) {

                FrontEndLevelControl controller = results.getCollision(i).getGeometry().getParent().getParent().getControl(FrontEndLevelControl.class);
                if (controller != null) {

                    // (For each "hit", we know distance, impact point, geometry)
//                    float dist = results.getCollision(i).getDistance();
//                    Vector3f pt = results.getCollision(i).getContactPoint();
//                    String target = results.getCollision(i).getGeometry().getName();
//                    System.out.println("Selection #" + i + ": " + target + " at " + pt + ", " + dist + " WU away.");

                    // Deactivate current controller 
                    if (currentControl != null && !currentControl.equals(controller)) {
                        currentControl.setActive(false);
                    }

                    // Set and activate current controller
                    currentControl = controller;
                    currentControl.setActive(true);
                    break;
                }
            }

        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
        }
    }
}
