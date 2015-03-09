/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
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
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.tools.SizeValue;
import java.awt.Point;
import java.io.File;
import java.util.ResourceBundle;
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
    private Level selectedLevel;
    private AudioNode levelBriefing;
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
        } else if ("campaing".equals(nifty.getCurrentScreen().getScreenId())) {

            // Set the dynamic values
            Label levelTitle = screen.findNiftyControl("levelTitle", Label.class);
            levelTitle.setText(getLevelTitle());

            Label mainObjective = screen.findNiftyControl("mainObjective", Label.class);
            mainObjective.setText(getLevelResourceBundle().getString("2"));

            Element mainObjectiveImage = screen.findElementByName("mainObjectiveImage");
            NiftyImage img = nifty.createImage("Textures/Obj_Shots/Level" + selectedLevel.getLevel() + (selectedLevel.getVariation() != null ? selectedLevel.getVariation() : "") + "-0.png", false);
            mainObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
            mainObjectiveImage.setWidth(img.getWidth());
            mainObjectiveImage.setHeight(img.getHeight());

            setupSubObjectiveLabel("subObjective1", "3");
            setupSubObjectiveLabel("subObjective2", "4");
            Label subObjective = setupSubObjectiveLabel("subObjective3", "5");

            // Fix the layout
            subObjective.getElement().getParent().layoutElements();

            Element subObjectiveImage = screen.findElementByName("subObjectiveImage");
            img = nifty.createImage("Textures/Obj_Shots/Level" + selectedLevel.getLevel() + (selectedLevel.getVariation() != null ? selectedLevel.getVariation() : "") + "-1.png", false);
            subObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
            subObjectiveImage.setWidth(img.getWidth());
            subObjectiveImage.setHeight(img.getHeight());

            // Play some tunes!!
            levelBriefing = new AudioNode(assetManager, "Sounds/speech_mentor/lev" + String.format("%02d", selectedLevel.getLevel()) + "001.mp2", false);
            levelBriefing.setLooping(false);
            levelBriefing.play();
        }
    }

    @Override
    public void onEndScreen() {
        if ("selectCampaignLevel".equals(nifty.getCurrentScreen().getScreenId())) {
            inputManager.removeRawInputListener(mouseListener);

        } else if ("campaing".equals(nifty.getCurrentScreen().getScreenId())) {

            // Quit playing the sound
            if (levelBriefing != null && levelBriefing.getStatus() == AudioSource.Status.Playing) {
                levelBriefing.stop();
            }
            levelBriefing = null;
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

    /**
     * Does a cinematic transition and opens up a specified screen
     *
     * @param transition name of the transition (without file extension)
     * @param screen the screen name
     */
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
     * Campaing level selected, transition the screen and display the briefing
     *
     * @param selectedLevel the selected level
     */
    private void selectCampaignLevel(FrontEndLevelControl selectedLevel) {
        this.selectedLevel = new Level(selectedLevel.getLevel(), selectedLevel.getVariation());
        doTransitionAndGoToScreen("EnginePath253", "campaing");
    }

    /**
     * Cancel level selection and go back to the campaign map selection
     */
    public void cancelLevelSelect() {
        this.selectedLevel = null;
        doTransitionAndGoToScreen("EnginePath254", "selectCampaignLevel");
    }

    /**
     * Get the selected level title
     *
     * @return level title
     */
    public String getLevelTitle() {
        if (selectedLevel != null) {
            ResourceBundle dict = getLevelResourceBundle();
            StringBuilder sb = new StringBuilder("\"");
            sb.append(dict.getString("0"));
            sb.append("\" - ");
            sb.append(dict.getString("1"));
            return sb.toString();
        }
        return "";
    }

    /**
     * Gets the selected level resource bundle
     *
     * @return the resource bundle
     */
    private ResourceBundle getLevelResourceBundle() {
        ResourceBundle dict = ResourceBundle.getBundle("Interface/Texts/LEVEL" + selectedLevel.getLevel() + (selectedLevel.getVariation() != null ? selectedLevel.getVariation() : "") + "_BRIEFING");
        return dict;
    }

    /**
     * Set ups a sub objective text
     *
     * @param id the element ID
     * @param textId the text ID in the resource bundle
     * @return returns the element
     */
    private Label setupSubObjectiveLabel(String id, String textId) {

        // Get the actual label and set the text
        Label label = screen.findNiftyControl(id, Label.class);
        String caption = getLevelResourceBundle().getString(textId);
        label.setText(caption.isEmpty() ? "" : "- ".concat(caption));

        // Measure the text height so that the element can be arranged to the the screen without overlapping the othe sub objectives
        TextRenderer renderer = label.getElement().getRenderer(TextRenderer.class);
        label.setHeight(new SizeValue(renderer.getTextHeight() + "px"));

        return label;
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
            setCampaingMapActive(evt.getX(), evt.getY());
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if (currentControl != null && evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                evt.setConsumed();

                // Select level
                mainMenuState.selectCampaignLevel(currentControl);
            }
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {

            // NOT TESTED AT ALL, just for shit & giggles, may work which would be super cool
            if (!evt.isScaleSpanInProgress()) {
                if (currentControl != null) {
                    evt.setConsumed();

                    // Select level
                    mainMenuState.selectCampaignLevel(currentControl);
                } else if (currentControl == null) {
                    evt.setConsumed();

                    // Treat this like "on hover"
                    setCampaingMapActive((int) evt.getX(), (int) evt.getY());
                }
            }
        }

        /**
         * Sets the map at certain point as active (i.e. selected), IF there is
         * one
         *
         * @param x x screen coordinate
         * @param y y screen coordinate
         */
        private void setCampaingMapActive(int x, int y) {

            // See if we hit a map
            CollisionResults results = new CollisionResults();

            // Convert screen click to 3D position
            Vector3f click3d = mainMenuState.app.getCamera().getWorldCoordinates(
                    new Vector2f(x, y), 0f);
            Vector3f dir = mainMenuState.app.getCamera().getWorldCoordinates(
                    new Vector2f(x, y), 1f).subtractLocal(click3d);

            // Aim the ray from the clicked spot forwards
            Ray ray = new Ray(click3d, dir);

            // Collect intersections between ray and all nodes in results list
            mainMenuState.menuNode.collideWith(ray, results);

            // See the results so we see what is going on
            for (int i = 0; i < results.size(); i++) {

                FrontEndLevelControl controller = results.getCollision(i).getGeometry().getParent().getParent().getControl(FrontEndLevelControl.class);
                if (controller != null) {

                    // Deactivate current controller
                    if (currentControl != null && !currentControl.equals(controller)) {
                        currentControl.setActive(false);
                    }

                    // Set and activate current controller
                    currentControl = controller;
                    currentControl.setActive(true);
                    return;
                }
            }

            // Deactivate current controller, nothing is selected
            if (currentControl != null) {
                currentControl.setActive(false);
                currentControl = null;
            }
        }
    }

    /**
     * Level info
     */
    private static class Level {

        private final int level;
        private final String variation;

        public Level(int level, String variation) {
            this.level = level;
            this.variation = variation;
        }

        public int getLevel() {
            return level;
        }

        public String getVariation() {
            return variation;
        }
    }
}
