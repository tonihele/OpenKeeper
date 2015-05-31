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
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.tools.SizeValue;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataEntry;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.cinematics.Cinematic;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.room.control.FrontEndLevelControl;

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
    private NiftyJmeDisplay niftyDisplay;
    public final static List<String> opengl = new ArrayList<>(Arrays.asList(new String[]{AppSettings.LWJGL_OPENGL1, AppSettings.LWJGL_OPENGL2, AppSettings.LWJGL_OPENGL3}));
    public final static List<Integer> samples = new ArrayList<>(Arrays.asList(new Integer[]{0, 2, 4, 6, 8, 16}));
    public final static List<Integer> anisotrophies = new ArrayList<>(Arrays.asList(new Integer[]{0, 2, 4, 8, 16}));
    private final KwdFile kwdFile;
    private final MouseEventListener mouseListener = new MouseEventListener(this);
    private Vector3f startLocation;

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
        }
        rootNode.attachChild(menuNode);

        // Init Nifty
        niftyDisplay = this.app.getNifty();

        // Load the start menu
        niftyDisplay.getNifty().getResourceBundles().put("menu", Main.getResourceBundle("Interface/Texts/Text"));
        niftyDisplay.getNifty().fromXml("Interface/MainMenu.xml", "start", this);

        // Set the camera position
        loadCameraStartLocation();
    }

    private void loadCameraStartLocation() {
        Player player = kwdFile.getPlayer((short) 3); // Keeper 1
        startLocation = new Vector3f(MapLoader.getCameraPositionOnMapPoint(player.getStartingCameraX(), player.getStartingCameraY()));

        CameraSweepData csd = (CameraSweepData) assetManager.loadAsset(AssetsConverter.PATHS_FOLDER.concat(File.separator).replaceAll(Pattern.quote("\\"), "/").concat("EnginePath250".concat(".").concat(CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION)));
        CameraSweepDataEntry entry = csd.getEntries().get(0);
        this.app.getCamera().setLocation(entry.getPosition().mult(MapLoader.TILE_WIDTH).add(startLocation));
        this.app.getCamera().setRotation(entry.getRotation());
    }

    @Override
    public void cleanup() {

        // Clear sound
        clearLevelBriefingNarration();

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
        switch (nifty.getCurrentScreen().getScreenId()) {
            case "selectCampaignLevel":
                inputManager.addRawInputListener(mouseListener);
                break;
            case "campaign":

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
                break;
            case "graphicsOptions":

                // Populate settings screen
                setGraphicsSettingsToGUI();
                break;
        }
    }

    @Override
    public void onEndScreen() {
        switch (nifty.getCurrentScreen().getScreenId()) {
            case "selectCampaignLevel":
                inputManager.removeRawInputListener(mouseListener);
                break;
            case "campaign":
                clearLevelBriefingNarration();
                break;
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

    public void options() {
        doTransitionAndGoToScreen("EnginePath256", "optionsMain");
    }

    public void cancelOptionsMain() {
        doTransitionAndGoToScreen("EnginePath257", "start");
    }

    /**
     * Called by the GUI, start the selected campaign level
     */
    public void startCampaignLevel() {

        // Detach us
        stateManager.detach(this);

        // Create the level state
        GameState gameState = new GameState("level" + selectedLevel.getLevel() + (selectedLevel.getVariation() != null ? selectedLevel.getVariation() : ""), assetManager);
        stateManager.attach(gameState);
    }

    /**
     * Does a cinematic transition and opens up a specified screen
     *
     * @param transition name of the transition (without file extension)
     * @param screen the screen name
     */
    private void doTransitionAndGoToScreen(String transition, final String screen) {

        // Remove the current screen
        nifty.gotoScreen("empty");

        // Do cinematic transition
        Cinematic c = new Cinematic(assetManager, app.getCamera(), startLocation, transition, menuNode);
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
     * Campaign level selected, transition the screen and display the briefing
     *
     * @param selectedLevel the selected level
     */
    private void selectCampaignLevel(FrontEndLevelControl selectedLevel) {
        this.selectedLevel = new Level(selectedLevel.getLevel(), selectedLevel.getVariation());
        doTransitionAndGoToScreen("EnginePath253", "campaign");
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
        return Main.getResourceBundle("Interface/Texts/LEVEL" + selectedLevel.getLevel() + (selectedLevel.getVariation() != null ? selectedLevel.getVariation() : "") + "_BRIEFING");
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
     * Stops the level briefing sound
     */
    private void clearLevelBriefingNarration() {

        // Quit playing the sound
        if (levelBriefing != null && levelBriefing.getStatus() == AudioSource.Status.Playing) {
            levelBriefing.stop();
        }
        levelBriefing = null;
    }

    /**
     * Save the graphics settings
     */
    public void applyGraphicsSettings() {

        // Get the controls settings
        boolean needToRestart = true;
        AppSettings settings = app.getSettings();
        DropDown res = screen.findNiftyControl("resolution", DropDown.class);
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        CheckBox fullscreen = screen.findNiftyControl("fullscreen", CheckBox.class);
        CheckBox vsync = screen.findNiftyControl("verticalSync", CheckBox.class);
        DropDown ogl = screen.findNiftyControl("openGl", DropDown.class);
        DropDown aa = screen.findNiftyControl("antialiasing", DropDown.class);
        DropDown af = screen.findNiftyControl("anisotropicFiltering", DropDown.class);
        CheckBox ssao = screen.findNiftyControl("ssao", CheckBox.class);
        MyDisplayMode mdm = (MyDisplayMode) res.getSelection();

        // TODO: See if we need a restart, but keep in mind that the settings are saved in the restart

        // Set the settings
        settings.setResolution(mdm.width, mdm.height);
        settings.setBitsPerPixel(mdm.bitDepth);
        settings.setFrequency((Integer) refresh.getSelection());
        settings.setFullscreen(fullscreen.isChecked());
        settings.setVSync(vsync.isChecked());
        settings.setRenderer((String) ogl.getSelection());
        settings.setSamples((Integer) aa.getSelection());
        settings.putInteger(Main.ANISOTROPY_KEY, (Integer) af.getSelection());
        settings.putBoolean(Main.SSAO_KEY, ssao.isChecked());

        // This fails and crashes on invalid settings
        if (needToRestart) {
            app.restart();
            nifty.resolutionChanged();
        }
        app.setViewProcessors();
    }

    private List<MyDisplayMode> getResolutions(GraphicsDevice device) {

        //Get from the system
        DisplayMode[] modes = device.getDisplayModes();

        List<MyDisplayMode> displayModes = new ArrayList<>(modes.length);

        //Loop them through
        for (DisplayMode dm : modes) {

            //They may already exist, then just add the possible resfresh rate
            MyDisplayMode mdm = new MyDisplayMode(dm);
            if (displayModes.contains(mdm)) {
                mdm = displayModes.get(displayModes.indexOf(mdm));
                mdm.addRefreshRate(dm);
            } else {
                displayModes.add(mdm);
            }
        }

        return displayModes;
    }

    /**
     * Sets the current setting values to the GUI
     */
    private void setGraphicsSettingsToGUI() {

        // Application settings
        AppSettings settings = app.getSettings();

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        MyDisplayMode mdm = new MyDisplayMode(settings);
        List<MyDisplayMode> resolutions = getResolutions(device);
        Collections.sort(resolutions);

        // Get values to the settings screen

        // Resolutions
        DropDown res = screen.findNiftyControl("resolution", DropDown.class);
        res.addAllItems(resolutions);
        res.selectItem(mdm);

        //Refresh rates
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        int index = Collections.binarySearch(resolutions, mdm);
        if (index >= 0) {
            refresh.addAllItems(resolutions.get(index).refreshRate);
            refresh.selectItem(settings.getFrequency());
        } else {
            refresh.addItem(mdm.refreshRate.get(0));
        }
        if (!settings.isFullscreen()) {
            refresh.disable();
        }

        //Fullscreen
        CheckBox fullscreen = screen.findNiftyControl("fullscreen", CheckBox.class);
        fullscreen.setChecked(settings.isFullscreen());
        fullscreen.setEnabled(device.isFullScreenSupported());

        //VSync
        CheckBox vsync = screen.findNiftyControl("verticalSync", CheckBox.class);
        vsync.setChecked(settings.isVSync());

        //Antialiasing
        DropDown aa = screen.findNiftyControl("antialiasing", DropDown.class);
        aa.addAllItems(samples);
        if (samples.contains(settings.getSamples())) {
            aa.selectItem(settings.getSamples());
        } else {
            aa.addItem(settings.getSamples());
            aa.selectItem(settings.getSamples());
        }

        //Anisotropic filtering
        DropDown af = screen.findNiftyControl("anisotropicFiltering", DropDown.class);
        af.addAllItems(anisotrophies);
        if (settings.containsKey(Main.ANISOTROPY_KEY) && anisotrophies.contains(settings.getInteger(Main.ANISOTROPY_KEY))) {
            af.selectItem(settings.getInteger(Main.ANISOTROPY_KEY));
        } else if (settings.containsKey(Main.ANISOTROPY_KEY)) {
            af.addItem(settings.getInteger(Main.ANISOTROPY_KEY));
            af.selectItem(settings.getInteger(Main.ANISOTROPY_KEY));
        }

        //OpenGL
        DropDown ogl = screen.findNiftyControl("openGl", DropDown.class);
        ogl.addAllItems(opengl);
        ogl.selectItem(settings.getRenderer());

        //SSAO
        CheckBox ssao = screen.findNiftyControl("ssao", CheckBox.class);
        ssao.setChecked(settings.getBoolean(Main.SSAO_KEY));
    }

    @NiftyEventSubscriber(id = "resolution")
    public void onResolutionChanged(final String id, final DropDownSelectionChangedEvent<MyDisplayMode> event) {

        //Set the refresh dropdown
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        refresh.clear();
        refresh.addAllItems(event.getSelection().refreshRate);
        refresh.selectItemByIndex(refresh.itemCount() - 1);
    }

    @NiftyEventSubscriber(id = "fullscreen")
    public void onFullscreenChanged(final String id, final CheckBoxStateChangedEvent event) {

        //Set the refresh dropdown
        DropDown refresh = screen.findNiftyControl("refreshRate", DropDown.class);
        if (event.isChecked()) {
            refresh.enable();
        } else {
            refresh.disable();
        }
    }

    private class MyDisplayMode implements Comparable<MyDisplayMode> {

        private int height;
        private int width;
        private int bitDepth;
        private List<Integer> refreshRate = new ArrayList<>(10);

        public MyDisplayMode(DisplayMode dm) {
            height = dm.getHeight();
            width = dm.getWidth();
            bitDepth = dm.getBitDepth();
            refreshRate.add(dm.getRefreshRate());
        }

        private MyDisplayMode(AppSettings settings) {
            height = settings.getHeight();
            width = settings.getWidth();
            bitDepth = settings.getBitsPerPixel();
            refreshRate.add(settings.getFrequency());
        }

        public void addRefreshRate(DisplayMode dm) {
            if (dm.getRefreshRate() != DisplayMode.REFRESH_RATE_UNKNOWN && !refreshRate.contains(dm.getRefreshRate())) {
                refreshRate.add(dm.getRefreshRate());
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MyDisplayMode other = (MyDisplayMode) obj;
            if (this.height != other.height) {
                return false;
            }
            if (this.width != other.width) {
                return false;
            }
            if (this.bitDepth != other.bitDepth) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.height;
            hash = 37 * hash + this.width;
            hash = 37 * hash + this.bitDepth;
            return hash;
        }

        @Override
        public String toString() {
            return width + " x " + height + (bitDepth != DisplayMode.BIT_DEPTH_MULTI ? " @" + bitDepth : "");
        }

        @Override
        public int compareTo(MyDisplayMode o) {
            int result = Integer.compare(bitDepth, o.bitDepth);
            if (result == 0) {
                result = Integer.compare(width, o.width);
            }
            if (result == 0) {
                result = Integer.compare(height, o.height);
            }
            return result;
        }
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
            setCampaignMapActive(evt.getX(), evt.getY());
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
                    setCampaignMapActive((int) evt.getX(), (int) evt.getY());
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
        private void setCampaignMapActive(int x, int y) {

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
