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
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.input.InputManager;
import com.jme3.input.KeyNames;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.controls.Chat;
import de.lessvoid.nifty.controls.ChatTextSendEvent;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.ListBoxSelectionChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.effects.EffectEventId;
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import toniarts.openkeeper.Main;
import static toniarts.openkeeper.Main.getDkIIFolder;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataEntry;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.cinematics.Cinematic;
import toniarts.openkeeper.game.data.HiScores;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Level;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.data.Settings.Setting;
import toniarts.openkeeper.game.network.message.MessageChat;
import toniarts.openkeeper.game.network.NetworkClient;
import toniarts.openkeeper.game.network.NetworkServer;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.table.TableRow;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import static toniarts.openkeeper.tools.convert.AssetsConverter.MAP_THUMBNAILS_FOLDER;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevFlag;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.video.MovieState;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.effect.EffectManager;
import toniarts.openkeeper.world.room.control.FrontEndLevelControl;

/**
 * The main menu state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MainMenuState extends AbstractAppState implements ScreenController {

    protected Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    //private ViewPort viewPort;
    private Nifty nifty;
    private Screen screen;
    protected Node menuNode;
    private Level selectedLevel;
    private AudioNode levelBriefing;
    public final static List<String> opengl = new ArrayList<>(Arrays.asList(new String[]{AppSettings.LWJGL_OPENGL2, AppSettings.LWJGL_OPENGL3}));
    public final static List<Integer> samples = new ArrayList<>(Arrays.asList(new Integer[]{0, 2, 4, 6, 8, 16}));
    public final static List<Integer> anisotrophies = new ArrayList<>(Arrays.asList(new Integer[]{0, 2, 4, 8, 16}));
    private KwdFile kwdFile;
    private final MainMenuInteraction listener;
    private Vector3f startLocation;
    private static final Logger logger = Logger.getLogger(MainMenuState.class.getName());
    public static HiScores hiscores = HiScores.load();
    private static final HashMap<String, String[]> cutscenes = new HashMap<>(3);
    private List<KwdFile> skirmishMaps;
    private List<KwdFile> multiplayerMaps;
    private KwdFile selectedMap;
    private final List<Keeper> skirmishPlayers = new ArrayList<>(4);
    private boolean skirmishMapSelect = false;
    private NetworkServer server = null;
    private NetworkClient client = null;

    static {
        cutscenes.put("image", "Intro,000,001,002,003,004,005,006,007,008,009,010,011,012,013,014,015,016,017,018,Outro".split(","));
        cutscenes.put("click", "INTRO,CutSceneLevel1,CutSceneLevel2,CutSceneLevel3,CutSceneLevel4,CutSceneLevel5,CutSceneLevel6,CutSceneLevel7,CutSceneLevel8,CutSceneLevel9,CutSceneLevel10,CutSceneLevel11,CutSceneLevel12,CutSceneLevel13,CutSceneLevel14,CutSceneLevel15,CutSceneLevel16,CutSceneLevel17,CutSceneLevel18,CutSceneLevel19,Outro".split(","));
        cutscenes.put("moviename", "${menu.77},${speech.1417},${speech.1439},${speech.1435},${speech.1445},${speech.1428},${speech.1426},${speech.1430},${speech.1432},${speech.1441},${speech.1431},${speech.1433},${speech.1419},${speech.1414},${speech.1437},${speech.1416},${speech.1420},${speech.1421},${speech.1443},${speech.1422},${menu.2843}".split(","));
    }

    /**
     * (c) Construct a MainMenuState, you should only have one of these. Disable
     * when not in use.
     *
     * @param load whether to load the menu scene now, or later when needed (has
     * its own loading screen here)
     * @param assetManager asset manager for loading the screen
     */
    public MainMenuState(final boolean load, final AssetManager assetManager) {
        listener = new MainMenuInteraction(this);
        if (load) {
            loadMenuScene(null, assetManager);
        }
    }

    /**
     * Loads up the main menu 3D scene
     *
     * @param loadingScreen optional loading screen
     * @param assetManager asset manager
     */
    private void loadMenuScene(final SingleBarLoadingState loadingScreen, final AssetManager assetManager) {

        // Load the 3D Front end
        kwdFile = new KwdFile(Main.getDkIIFolder(), new File(Main.getDkIIFolder().concat(AssetsConverter.MAPS_FOLDER.concat("FrontEnd3DLevel.kwd"))));
        if (loadingScreen != null) {
            loadingScreen.setProgress(0.25f);
        }

        // Attach the 3D Front end
        menuNode = new Node("Main menu");
        menuNode.attachChild(new MapLoader(assetManager, kwdFile, new EffectManager(assetManager, kwdFile), null) {
            @Override
            protected void updateProgress(int progress, int max) {
                if (loadingScreen != null) {
                    loadingScreen.setProgress(0.25f + ((float) progress / max * 0.75f));
                }
            }
        }.load(assetManager, kwdFile));
        if (loadingScreen != null) {
            loadingScreen.setProgress(1.0f);
        }

        // Init the skirmish maps
        initMapSelection();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        //viewPort = this.app.getViewPort();

        // Set some Nifty stuff
        NiftyJmeDisplay niftyDisplay = MainMenuState.this.app.getNifty();
        niftyDisplay.getNifty().addResourceBundle("menu", Main.getResourceBundle("Interface/Texts/Text"));
        niftyDisplay.getNifty().addResourceBundle("speech", Main.getResourceBundle("Interface/Texts/Speech"));
        niftyDisplay.getNifty().addResourceBundle("mpd1", Main.getResourceBundle("Interface/Texts/LEVELMPD1_BRIEFING"));
        niftyDisplay.getNifty().addResourceBundle("mpd2", Main.getResourceBundle("Interface/Texts/LEVELMPD2_BRIEFING"));
        niftyDisplay.getNifty().addResourceBundle("mpd3", Main.getResourceBundle("Interface/Texts/LEVELMPD3_BRIEFING"));
        niftyDisplay.getNifty().addResourceBundle("mpd4", Main.getResourceBundle("Interface/Texts/LEVELMPD4_BRIEFING"));
        niftyDisplay.getNifty().addResourceBundle("mpd5", Main.getResourceBundle("Interface/Texts/LEVELMPD5_BRIEFING"));
        niftyDisplay.getNifty().addResourceBundle("mpd6", Main.getResourceBundle("Interface/Texts/LEVELMPD6_BRIEFING"));
        niftyDisplay.getNifty().addResourceBundle("mpd7", Main.getResourceBundle("Interface/Texts/LEVELMPD7_BRIEFING"));
    }

    /**
     * Load the initial main menu camera position
     */
    private void loadCameraStartLocation() {
        Player player = kwdFile.getPlayer((short) 3); // Keeper 1
        startLocation = new Vector3f(MapLoader.getCameraPositionOnMapPoint(player.getStartingCameraX(), player.getStartingCameraY()));

        // Set the actual camera location
        loadCameraStartLocation("EnginePath250");
    }

    /**
     * Loads and sets up the starting camera position from the given transition
     *
     * @param transition the transition
     */
    private void loadCameraStartLocation(String transition) {
        CameraSweepData csd = (CameraSweepData) assetManager.loadAsset(AssetsConverter.PATHS_FOLDER.concat(File.separator).replaceAll(Pattern.quote("\\"), "/").concat(transition.concat(".").concat(CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION)));
        CameraSweepDataEntry entry = csd.getEntries().get(0);
        Cinematic.applyCameraSweepEntry(app.getCamera(), startLocation, entry);
    }

    @Override
    public void cleanup() {

        // Clear sound
        clearLevelBriefingNarration();

        // Detach our start menu
        if (menuNode != null) {
            rootNode.detachChild(menuNode);
            menuNode = null;
        }

        if (server != null) {
            server.close();
        }

        if (client != null) {
            client.close();
        }

        super.cleanup();
    }

    /**
     * Initialize the start menu, sets the menu scene in place & sets the
     * controls and start screen
     */
    private void initializeMainMenu() {

        // Set the processors & scene
        app.enqueue(new Callable() {
            @Override
            public Object call() throws Exception {
                MainMenuState.this.app.setViewProcessors();
                rootNode.attachChild(menuNode);

                // Start screen, do this here since another state may have just changed to empty screen -> have to do it like this, delayed
                MainMenuState.this.app.getNifty().getNifty().gotoScreen("start");
                return null;
            }
        });

        // Enable cursor
        app.getInputManager().setCursorVisible(true);
        if (app.getUserSettings().getSettingBoolean(Settings.Setting.USE_CURSORS)) {
            app.getInputManager().setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));
        }

        // Set the camera position
        loadCameraStartLocation();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {

            // If this is the first time, we might have to load the menu
            if (menuNode == null) {

                // Set up the loading screen
                SingleBarLoadingState loader = new SingleBarLoadingState() {
                    @Override
                    public Void onLoad() {
                        loadMenuScene(this, MainMenuState.this.assetManager);
                        return null;
                    }

                    @Override
                    public void onLoadComplete() {
                        initializeMainMenu();
                    }
                };
                stateManager.attach(loader);
            } else {
                initializeMainMenu();
            }
        } else {

            if (menuNode != null && rootNode != null) {

                // Detach our start menu
                rootNode.detachChild(menuNode);
            }

            if (nifty != null) {
                nifty.gotoScreen("empty");
            }
        }
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    @Override
    public void onStartScreen() {
        bind(nifty, nifty.getCurrentScreen());
        switch (nifty.getCurrentScreen().getScreenId()) {
            case "singlePlayer":
                selectedMap = null;
                break;

            case "selectCampaignLevel":
                inputManager.addRawInputListener(listener);
                break;
            case "campaign":

                // Set the dynamic values
                Label levelTitle = screen.findNiftyControl("levelTitle", Label.class);
                levelTitle.setText(getLevelTitle());
                Label mainObjective = screen.findNiftyControl("mainObjective", Label.class);
                mainObjective.setText(getLevelResourceBundle().getString("2"));
                Element mainObjectiveImage = screen.findElementById("mainObjectiveImage");
                NiftyImage img = nifty.createImage("Textures/Obj_Shots/" + selectedLevel.getFullName() + "-0.png", false);
                mainObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
                mainObjectiveImage.setWidth(img.getWidth());
                mainObjectiveImage.setHeight(img.getHeight());
                String subText1 = getLevelResourceBundle().getString("3");
                String subText2 = getLevelResourceBundle().getString("4");
                String subText3 = getLevelResourceBundle().getString("5");
                Element subObjectivePanel = screen.findElementById("subObjectivePanel");

                subObjectivePanel.hide();
                if (!(subText1.isEmpty() && subText2.isEmpty() && subText3.isEmpty())) {
                    // We have subobjectives
                    subObjectivePanel.show();
                    setupSubObjectiveLabel("subObjective1", subText1);
                    setupSubObjectiveLabel("subObjective2", subText2);
                    Label subObjective = setupSubObjectiveLabel("subObjective3", subText3);
                    // Fix the layout
                    subObjective.getElement().getParent().layoutElements();
                    Element subObjectiveImage = screen.findElementById("subObjectiveImage");
                    subObjectiveImage.hide();

                    if (selectedLevel.getType().equals(Level.LevelType.Level)) {
                        subObjectiveImage.show();
                        img = nifty.createImage("Textures/Obj_Shots/" + selectedLevel.getFullName() + "-1.png", false);
                        subObjectiveImage.getRenderer(ImageRenderer.class).setImage(img);
                        subObjectiveImage.setWidth(img.getWidth());
                        subObjectiveImage.setHeight(img.getHeight());

                        // Play some tunes!!
                        levelBriefing = new AudioNode(assetManager, "Sounds/speech_mentor/lev" + String.format("%02d", selectedLevel.getLevel()) + "001.mp2", false);
                        levelBriefing.setLooping(false);
                        levelBriefing.setDirectional(false);
                        levelBriefing.setPositional(false);
                        levelBriefing.play();
                    }
                }
                break;
            case "hiscores":
                generateHiscoreList();
                break;

            case "optionsGraphics":

                // Populate settings screen
                setGraphicsSettingsToGUI();
                break;
            case "optionsControl":
                setControlSettingsToGUI();
                break;
            case "movies":
                generateMovieList();
                break;
            case "skirmish":
                // Init the screen
                skirmishMapSelect = true;
                if (selectedMap == null) {
                    selectRandomMap();
                }
                populateSkirmishPlayerTable();
                break;

            case "multiplayerCreate":
                skirmishMapSelect = false;
                if (selectedMap == null) {
                    selectRandomMap();
                }
                                
                if (client != null) {
                    ListBox<TableRow> players = screen.findNiftyControl("playersTable", ListBox.class);
                    if (players != null) {
                        players.addItem(new TableRow(players.itemCount(), client.getPlayer()));
                    }
                    
                    client.setChat(MainMenuState.this.screen.findNiftyControl("multiplayerChat", Chat.class));
                    
                    Label title = screen.findNiftyControl("multiplayerTitle", Label.class);
                    if (title != null) {
                        title.setText(client.getClient().getGameName());
                    }
                    
                    if (client.getRole() == NetworkClient.Role.SLAVE) {
                        // TODO disable map change, add, kick players and other stuf
                        Element element = screen.findElementById("skirmishGameControl");
                        if (element != null) {
                            element.hide();
                        }
                        element = screen.findElementById("skirmishPlayerControl");
                        if (element != null) {
                            element.hide();
                        }
                    }
                }
                break;

            case "multiplayerLocal":
                selectedMap = null;
                // multiplayerRefresh();
                break;

            case "skirmishMapSelect": {

                // Populate the maps
                if (skirmishMapSelect) {
                    populateMapSelection(skirmishMaps, selectedMap);
                } else {
                    populateMapSelection(multiplayerMaps, selectedMap);
                }
            }
        }
    }

    public void multiplayerCreate() throws IOException {

        TextField player = screen.findNiftyControl("playerName", TextField.class);
        TextField game = screen.findNiftyControl("gameName", TextField.class);
        TextField port = screen.findNiftyControl("gamePort", TextField.class);
        
        try {
            server = new NetworkServer(game.getRealText(), Integer.valueOf(port.getRealText()));            
            server.start();
            logger.info("Server created");
        } catch (IOException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
            goToScreen("multiplayerLocal");
        }
        
        client = new NetworkClient(player.getRealText());                
        
        try {
            
            client.start(server.getHost(), server.getPort());
        } catch (IOException ex) {
            server.close();
            logger.log(java.util.logging.Level.SEVERE, null, ex);
            goToScreen("multiplayerLocal");
        }
        
        goToScreen("multiplayerCreate");
    }

    public void multiplayerRefresh() {
        /*
        TextField port = screen.findNiftyControl("gamePort", TextField.class);
        int serverPort = Integer.valueOf(port.getRealText());

        ListBox<TableRow> games = screen.findNiftyControl("multiplayerGamesTable", ListBox.class);
        if (games == null) {
            logger.warning("Element multiplayerGamesTable not found");
            return;
        }
        games.clear();

        LocalServerSearch searcher = new LocalServerSearch(serverPort) {
            int i = 0;

            @Override
            public void onFound(NetworkServer server) {
                TableRow row = new TableRow(i++, server.getName(), server.getHost(), server.getPort() + "");
                games.addItem(row);
            }
        };
        searcher.start();
        */
    }

    public void multiplayerConnect() {
        /*
        ListBox<TableRow> games = screen.findNiftyControl("multiplayerGamesTable", ListBox.class);
        if (games == null) {
            logger.warning("Element multiplayerGamesTable not found");
            return;
        }
        TableRow row = games.getFocusItem();
        String host = row.getData().get(1);
        String port = row.getData().get(2);
        */
        TextField player = screen.findNiftyControl("playerName", TextField.class);
        TextField hostAdderss = screen.findNiftyControl("hostAddress", TextField.class);
        if (player == null || player.getRealText().isEmpty() 
                || hostAdderss == null || hostAdderss.getRealText().isEmpty()) {
            return;
        }
        
        String[] address = hostAdderss.getRealText().split(":");
        String host = address[0];
        Integer port = (address.length == 2) ? Integer.valueOf(address[1]) : 7575;
        
        client = new NetworkClient(player.getRealText(), NetworkClient.Role.SLAVE);
        try {
            
            client.start(host, port);
            goToScreen("multiplayerCreate");

        } catch (IOException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
            //goToScreen("multiplayerLocal");
        }        
    }

    @NiftyEventSubscriber(id="multiplayerChat")
    public void onChatTextSend(final String id, final ChatTextSendEvent event) {
        if (client == null) {
            logger.warning("Network client not initialized");
            return;
        }        
        
        // event.getChatControl().addPlayer("ID " + client.getId(), null);
        // event.getChatControl().receivedChatLine(event.getText(), null);
        client.getClient().send(new MessageChat(client.getPlayer() + ": " + event.getText()));
    } 

    @Override
    public void onEndScreen() {
        switch (nifty.getCurrentScreen().getScreenId()) {
            case "selectCampaignLevel":
                inputManager.removeRawInputListener(listener);
                break;
            case "campaign":
                clearLevelBriefingNarration();
                break;
            case "multiplayerCreate":
                if (client != null) {
                    client.close();
                    client = null;
                }

                if (server != null) {                    
                    server.close();                    
                    server = null;
                }
                break;
        }
    }

    private void generateHiscoreList() {
        Element hiscoreList = screen.findElementById("hiscoreList");

        if (hiscoreList != null) {
            for (Element oldElement : hiscoreList.getChildren()) {
                nifty.removeElement(screen, oldElement);
            }

            ControlBuilder hiscoreDesc = new ControlBuilder("hiscoreHead", "hiscoreRow");
            hiscoreDesc.parameter("rank", "${menu.80}");
            hiscoreDesc.parameter("score", "${menu.82}");
            hiscoreDesc.parameter("level", "${menu.2042}");
            hiscoreDesc.parameter("user", "${menu.83}");
            hiscoreDesc.style("nifty-hiscore-head");
            hiscoreDesc.build(nifty, screen, hiscoreList);

            int i = 0;
            for (HiScores.HiScoresEntry hiscore : MainMenuState.hiscores.getEntries()) {
                ControlBuilder hiscoreControl = new ControlBuilder("hiscore" + i++, "hiscoreRow");
                hiscoreControl.parameter("rank", i + "");
                hiscoreControl.parameter("score", hiscore.getScore() + "");
                hiscoreControl.parameter("level", hiscore.getLevel());
                hiscoreControl.parameter("user", hiscore.getName());
                hiscoreControl.build(nifty, screen, hiscoreList);
            }
        }

    }

    public void goToScreen(String nextScreen) {
        nifty.gotoScreen(nextScreen);  // Switch to another screen
    }

    /**
     * Select a my pet dungeon level
     *
     * @param Number the level number as a string
     */
    public void selectMPDLevel(String Number) {
        this.selectedLevel = new Level(Level.LevelType.MPD, Integer.parseInt(Number), null);
        goToScreen("campaign");
    }

    /**
     * Called by the GUI, start the selected level
     * @param type where level selected. @TODO change campaign like others or otherwise
     */
    public void startLevel(String type) {
        if ("campaign".equals(type.toLowerCase())) {
            // Disable us
            setEnabled(false);

            // Create the level state
            String level = String.format("%s%s%s", selectedLevel.getType(), selectedLevel.getLevel(), selectedLevel.getVariation());
            GameState gameState = new GameState(level);
            stateManager.attach(gameState);

        } else if (selectedMap != null) {

            // Disable us
            setEnabled(false);

            // Create the level state. FIXME maybe some bugs
            GameState gameState = new GameState(selectedMap, skirmishPlayers);
            stateManager.attach(gameState);
        }
    }

    /**
     * Called by the gui to restart the autoscroll effect
     */
    public void restartCredits() {
        Element credits = screen.findElementById("creditList");
        if (credits != null) {
            credits.resetEffects();
            if (!credits.isEffectActive(EffectEventId.onActive)) {
                credits.startEffect(EffectEventId.onActive);
            } else {
                // Screen got changed
                credits.stopEffect(EffectEventId.onActive);
            }
        }
    }

    /**
     * Plays a movie file
     *
     * @param movieFile the movie filename that should be played. No extension!
     */
    public void playMovie(String movieFile) {
        try {
            MovieState movieState = new MovieState(getDkIIFolder().concat("Data".concat(File.separator).concat("Movies").concat(File.separator).concat(movieFile + ".TGQ"))) {
                @Override
                protected void onPlayingEnd() {
                }
            };
            stateManager.attach(movieState);
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Failed to initiate playing " + movieFile + "!", e);
        }
    }

    /**
     * Generates the movie list
     */
    private void generateMovieList() {
        // TODO: We should only do that if the progress has changed and at the start of the game
        Element movies = screen.findElementById("movieList");
        if (movies != null) {
            for (Element oldElement : movies.getChildren()) {
                nifty.removeElement(screen, oldElement);
            }

            String[] item = MainMenuState.cutscenes.get("moviename");
            String image;
            String action;
            for (int i = 0; i < item.length; i++) {
                if (isCutsceneViewable(i)) {
                    image = MainMenuState.cutscenes.get("image")[i];
                    action = "playMovie(" + MainMenuState.cutscenes.get("click")[i] + ")";
                } else {
                    image = "Unavailable";
                    action = "goToScreen(cutsceneLocked)";
                }
                ControlBuilder control = new ControlBuilder("movie" + i, "movieButton");
                control.parameter("image", "Textures/Mov_Shots/M-" + image + "-0.png");
                control.parameter("click", action);
                control.parameter("moviename", item[i]);
                control.build(nifty, screen, movies);
            }
        }
    }

    /**
     * Stub for checking if a cutscene is unlocked
     *
     * @param level the level id that should be checked. Should be a value
     * between 0-20
     * @return
     */
    private boolean isCutsceneViewable(int level) {
        // TODO: implement the logic behind it
        return true;
    }

    /**
     * Go to screen with cinematic transition
     *
     * @param transition the transition code
     * @param screen the screen to go to
     * @param transitionStatic the transition for the finishing position. Not
     * all the transitions return perfectly so this is a workaround
     */
    public void doTransition(String transition, String screen, String transitionStatic) {
        transition = "EnginePath" + transition;

        doTransitionAndGoToScreen(transition, screen, (transitionStatic == null || "null".equals(transitionStatic) ? null : "EnginePath" + transitionStatic));
    }

    /**
     * Does a cinematic transition and opens up a specified screen
     *
     * @param transition name of the transition (without file extension)
     * @param screen the screen name
     */
    private void doTransitionAndGoToScreen(final String transition, final String screen, final String transitionStatic) {

        // Remove the current screen
        nifty.gotoScreen("empty");

        // Do cinematic transition
        Cinematic c = new Cinematic(assetManager, app.getCamera(), startLocation, transition, menuNode, stateManager);
        c.addListener(new CinematicEventListener() {
            @Override
            public void onPlay(CinematicEvent cinematic) {
            }

            @Override
            public void onPause(CinematicEvent cinematic) {
            }

            @Override
            public void onStop(CinematicEvent cinematic) {
                if (transitionStatic != null) {
                    loadCameraStartLocation(transitionStatic);
                }
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
    protected void selectCampaignLevel(FrontEndLevelControl selectedLevel) {
        this.selectedLevel = selectedLevel.getLevel();
        doTransition("253", "campaign", null);
    }

    /**
     * Cancel level selection and go back to the campaign map selection
     */
    public void cancelLevelSelect() {
        if (this.selectedLevel.getType().equals(Level.LevelType.MPD)) {
            goToScreen("myPetDungeon");
        } else {
            doTransition("254", "selectCampaignLevel", null);
        }
        this.selectedLevel = null;
    }

    /**
     * Get the selected level title
     *
     * @return level title
     */
    public String getLevelTitle() {
        if (selectedLevel != null) {
            ResourceBundle dict = getLevelResourceBundle();
            StringBuilder sb = new StringBuilder();
            String name = dict.getString("0");
            if (!name.equals("")) {
                sb.append("\"");
                sb.append(name);
                sb.append("\" - ");
            }
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
        String briefingName;
        switch (selectedLevel.getType()) {
            case MPD:
                briefingName = selectedLevel.getFullName();
                break;
            case Secret:
                briefingName = "S" + selectedLevel.getLevel();
                break;
            default:
                briefingName = selectedLevel.getLevel() + selectedLevel.getVariation().toUpperCase();
                break;
        }
        return Main.getResourceBundle("Interface/Texts/LEVEL" + briefingName + "_BRIEFING");
    }

    /**
     * Set ups a sub objective text
     *
     * @param id the element ID
     * @param textId the text ID in the resource bundle
     * @return returns the element
     */
    private Label setupSubObjectiveLabel(String id, String caption) {

        // Get the actual label and set the text
        Label label = screen.findNiftyControl(id, Label.class);
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
        Settings settings = app.getUserSettings();
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
        settings.getAppSettings().setResolution(mdm.width, mdm.height);
        settings.getAppSettings().setBitsPerPixel(mdm.bitDepth);
        settings.getAppSettings().setFrequency((Integer) refresh.getSelection());
        settings.getAppSettings().setFullscreen(fullscreen.isChecked());
        settings.getAppSettings().setVSync(vsync.isChecked());
        settings.getAppSettings().setRenderer((String) ogl.getSelection());
        settings.getAppSettings().setSamples((Integer) aa.getSelection());
        settings.setSetting(Settings.Setting.ANISOTROPY, af.getSelection());
        settings.setSetting(Settings.Setting.SSAO, ssao.isChecked());

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
        AppSettings settings = app.getUserSettings().getAppSettings();

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
        if (app.getUserSettings().containsSetting(Settings.Setting.ANISOTROPY) && anisotrophies.contains(app.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY))) {
            af.selectItem(app.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY));
        } else if (app.getUserSettings().containsSetting(Settings.Setting.ANISOTROPY)) {
            af.addItem(app.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY));
            af.selectItem(app.getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY));
        }

        //OpenGL
        DropDown ogl = screen.findNiftyControl("openGl", DropDown.class);
        ogl.addAllItems(opengl);
        ogl.selectItem(settings.getRenderer());

        //SSAO
        CheckBox ssao = screen.findNiftyControl("ssao", CheckBox.class);
        ssao.setChecked(app.getUserSettings().getSettingBoolean(Settings.Setting.SSAO));
    }

    private void setControlSettingsToGUI() {
        ListBox<TableRow> listBox = screen.findNiftyControl("keyboardSetup", ListBox.class);
        int i = 0;
        int selected = 0;
        KeyNames kNames = new KeyNames();
        listBox.clear();
        List<Setting> settings = Settings.Setting.getSettings(Settings.SettingCategory.CONTROLS);

        for (Setting setting : settings) {
            /*
             if (map.equals(selectedMap)) {
             selected = i;
             }
             */
            String keys = "";
            if (setting.getSpecialKey() != null) {
                keys = (kNames.getName(setting.getSpecialKey()) + " + ").replace("Left ", "").replace("Right ", "");
            }
            keys += kNames.getName((int) setting.getDefaultValue()).replace("Left ", "").replace("Right ", "");
            TableRow row = new TableRow(i++, String.format("${menu.%s}", setting.getTranslationKey()), keys);
            listBox.addItem(row);
        }
        listBox.selectItemByIndex(selected);

        screen.findNiftyControl("mouseSensitivity", Slider.class).setValue((float) Settings.Setting.MOUSE_SENSITIVITY.getDefaultValue());
        screen.findNiftyControl("gameSpeed", Slider.class).setValue((float) Settings.Setting.GAME_SPEED.getDefaultValue());
        screen.findNiftyControl("scrollSpeed", Slider.class).setValue((float) Settings.Setting.SCROLL_SPEED.getDefaultValue());
        screen.findNiftyControl("invertMouse", CheckBox.class).setChecked((boolean) Settings.Setting.MOUSE_INVERT.getDefaultValue());
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

    private void initMapSelection() {

        // Get the skirmish maps
        File f = new File(Main.getDkIIFolder().concat(AssetsConverter.MAPS_FOLDER));
        File[] files = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".kwd");
            }
        });

        // Read them
        multiplayerMaps = new ArrayList<>(files.length);
        skirmishMaps = new ArrayList<>(files.length);
        for (File file : files) {
            KwdFile kwd = new KwdFile(Main.getDkIIFolder(), file, false);
            if (kwd.getGameLevel().getLvlFlags().contains(LevFlag.IS_SKIRMISH_LEVEL)) {
                skirmishMaps.add(kwd);
            }
            if (kwd.getGameLevel().getLvlFlags().contains(LevFlag.IS_MULTIPLAYER_LEVEL)) {
                multiplayerMaps.add(kwd);
            }
        }

        // Sort them
        Comparator c = new Comparator<KwdFile>() {
            @Override
            public int compare(KwdFile o1, KwdFile o2) {
                return o1.getGameLevel().getName().compareToIgnoreCase(o2.getGameLevel().getName());
            }
        };
        Collections.sort(skirmishMaps, c);
        Collections.sort(multiplayerMaps, c);

        // Init skirmish players
        Keeper keeper = new Keeper(false, "Player", Keeper.KEEPER1_ID);
        skirmishPlayers.add(keeper);
        keeper = new Keeper(true, null, Keeper.KEEPER2_ID);
        skirmishPlayers.add(keeper);
    }

    private void setSkirmishMapDataToGUI() {

        // The map title
        Label label = screen.findNiftyControl("mapNameTitle", Label.class);
        label.setText(selectedMap == null ? "No maps found from " + AssetsConverter.MAPS_FOLDER : selectedMap.getGameLevel().getName());
        NiftyUtils.resetContraints(label);

        if (selectedMap != null) {

            // Player count
            label = screen.findNiftyControl("playerCount", Label.class);
            label.setText(": " + selectedMap.getGameLevel().getPlayerCount());
            NiftyUtils.resetContraints(label);

            // Map image
            Element mapImage = screen.findElementById("mapImage");
            NiftyImage img = getMapThumbnail(selectedMap);
            mapImage.getRenderer(ImageRenderer.class).setImage(img);
            mapImage.setConstraintWidth(new SizeValue(img.getWidth() + "px"));
            mapImage.setConstraintHeight(new SizeValue(img.getHeight() + "px"));

            // We can't have more players than the map supports
            if (skirmishPlayers.size() > selectedMap.getGameLevel().getPlayerCount()) {
                skirmishPlayers.subList(selectedMap.getGameLevel().getPlayerCount(), skirmishPlayers.size()).clear();
            }
        }

        // Re-populate
        screen.layoutLayers();
    }

    public void selectRandomMap() {
        KwdFile map;
        List<KwdFile> maps;

        maps = skirmishMapSelect ?  skirmishMaps : multiplayerMaps;

        if (maps.isEmpty()) {
            return;
        } else if (maps.size() == 1) {
            map = maps.get(0);
        } else {
            do {
                map = maps.get(FastMath.nextRandomInt(0, maps.size() - 1));
            } while (map.equals(selectedMap));
        }

        selectedMap = map;
        setSkirmishMapDataToGUI();  
    }

    private void populateSkirmishPlayerTable() {
        ListBox<TableRow> listBox = screen.findNiftyControl("playersTable", ListBox.class);
        listBox.clear();
        int i = 0;
        for (Keeper keeper : skirmishPlayers) {
            listBox.addItem(new TableRow(i, keeper.toString(), "", "", "", keeper.isReady() + ""));
            i++;
        }
    }

    /**
     * Go to map selection
     *
     * @param mapFor skirmish/multiplayer
     */
    public void selectMap(String mapFor) {
        if ("skirmish".equals(mapFor)) {
            skirmishMapSelect = true;
        } else {
            skirmishMapSelect = false;
        }
        nifty.gotoScreen("skirmishMapSelect");
    }

    public void cancelMapSelection() {
        if (skirmishMapSelect) {
            nifty.gotoScreen("skirmish");
        } else {
            nifty.gotoScreen("multiplayerCreate");
        }
        skirmishMapSelect = false;
    }

    public void mapSelected() {
        ListBox<TableRow> listBox = screen.findNiftyControl("mapsTable", ListBox.class);
        int selectedMapIndex = listBox.getSelectedIndices().get(0);
        if (skirmishMapSelect) {
            selectedMap = skirmishMaps.get(selectedMapIndex);
            nifty.gotoScreen("skirmish");
        } else {
            selectedMap = multiplayerMaps.get(selectedMapIndex);
            nifty.gotoScreen("multiplayerCreate");
        }
        skirmishMapSelect = false;
    }

    @NiftyEventSubscriber(id = "mapsTable")
    public void onListBoxSelectionChanged(final String id, final ListBoxSelectionChangedEvent<TableRow> event) {

        if (event.getSelectionIndices().isEmpty()) {
            return;
        }

        // Put the map info in
        KwdFile selectedMap;
        if (skirmishMapSelect) {
            selectedMap = skirmishMaps.get(event.getSelectionIndices().get(0));
        } else {
            selectedMap = multiplayerMaps.get(event.getSelectionIndices().get(0));
        }

        // The map title
        Label label = screen.findNiftyControl("mapNameTitle", Label.class);
        label.setText(selectedMap == null ? "No maps found from " + AssetsConverter.MAPS_FOLDER : selectedMap.getGameLevel().getName());
        NiftyUtils.resetContraints(label);

        if (selectedMap != null) {

            // Player count
            label = screen.findNiftyControl("playerCount", Label.class);
            label.setText(": " + selectedMap.getGameLevel().getPlayerCount());
            NiftyUtils.resetContraints(label);

            // Map image
            // TODO: static generator to MapLoader etc. place, I don't really want to use the BMPs
            Element mapImage = screen.findElementById("mapImage");
            NiftyImage img = getMapThumbnail(selectedMap);
            mapImage.getRenderer(ImageRenderer.class).setImage(img);
            mapImage.setConstraintWidth(new SizeValue(img.getWidth() + "px"));
            mapImage.setConstraintHeight(new SizeValue(img.getHeight() + "px"));

            // Map size
            label = screen.findNiftyControl("mapSize", Label.class);
            label.setText(selectedMap.getMap().getWidth() + " x " + selectedMap.getMap().getHeight());
            NiftyUtils.resetContraints(label);
        }

        // Re-populate
        screen.layoutLayers();
    }

    /**
     * Populate the map selection with given maps
     *
     * @param kwds map selection
     * @param selectedMap the selected map
     */
    private void populateMapSelection(List<KwdFile> kwds, KwdFile selectedMap) {
        ListBox<TableRow> listBox = screen.findNiftyControl("mapsTable", ListBox.class);
        int i = 0;
        int selected = 0;
        listBox.clear();
        for (KwdFile kwd : kwds) {
            if (kwd.equals(selectedMap)) {
                selected = i;
            }
            listBox.addItem(new TableRow(i, kwd.getGameLevel().getName(),
                    String.valueOf(kwd.getGameLevel().getPlayerCount()),
                    kwd.getMap().getWidth() + " x " + kwd.getMap().getHeight()));
            i++;
        }
        listBox.selectItemByIndex(selected);
    }

    private NiftyImage getMapThumbnail(KwdFile map) {

        // See if the map thumbnail exist, otherwise create one
        String asset = "Textures/Thumbnails/" + ConversionUtils.stripFileName(map.getGameLevel().getName()) + ".png";
        if (assetManager.locateAsset(new TextureKey(asset)) == null) {

            // Generate
            try {
                AssetsConverter.genererateMapThumbnail(map, AssetsConverter.getAssetsFolder().concat(MAP_THUMBNAILS_FOLDER).concat(File.separator));
            } catch (Exception e) {
                logger.log(java.util.logging.Level.WARNING, "Failed to generate map file out of {0}!", kwdFile);
                asset = "Textures/Unique_NoTextureName.png";
            }
        }
        return nifty.createImage(asset, true);
    }
}
