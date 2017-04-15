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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import static toniarts.openkeeper.Main.getDkIIFolder;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataEntry;
import toniarts.openkeeper.cinematics.Cinematic;
import toniarts.openkeeper.game.MapSelector;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.GeneralLevel;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.data.Settings.Setting;
import toniarts.openkeeper.game.network.chat.ChatClientService;
import toniarts.openkeeper.game.state.ConnectionState.ConnectionErrorListener;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.game.state.lobby.LobbyClientService;
import toniarts.openkeeper.game.state.lobby.LobbyService;
import toniarts.openkeeper.game.state.lobby.LobbyState;
import toniarts.openkeeper.game.state.lobby.LocalLobby;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.video.MovieState;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.room.control.FrontEndLevelControl;

/**
 * The main menu state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MainMenuState extends AbstractAppState {

    protected Main app;
    protected Node rootNode;
    protected AssetManager assetManager;
    protected AppStateManager stateManager;
    protected InputManager inputManager;
    //private ViewPort viewPort;
    private MainMenuScreenController screen;
    protected Node menuNode;
    protected GeneralLevel selectedLevel;
    protected AudioNode levelBriefing;

    private KwdFile kwdFile;
    protected final MainMenuInteraction listener;
    private Vector3f startLocation;
    protected MapSelector mapSelector;
    private final MainMenuConnectionErrorListener connectionErrorListener = new MainMenuConnectionErrorListener();

    private static final Logger logger = Logger.getLogger(MainMenuState.class.getName());

    /**
     * (c) Construct a MainMenuState, you should only have one of these. Disable
     * when not in use.
     *
     * @param enabled whether to load the menu scene now, or later when needed
     * (has its own loading screen here)
     * @param assetManager asset manager for loading the screen
     * @param app the main application
     */
    public MainMenuState(final boolean enabled, final AssetManager assetManager, final Main app) {
        listener = new MainMenuInteraction(this);
        super.setEnabled(enabled);

        if (enabled) {
            loadMenuScene(null, assetManager, app);
        }
    }

    /**
     * Loads up the main menu 3D scene
     *
     * @param loadingScreen optional loading screen
     * @param assetManager asset manager
     */
    private void loadMenuScene(final SingleBarLoadingState loadingScreen, final AssetManager assetManager, final Main app) {

        // Load the 3D Front end
        kwdFile = new KwdFile(Main.getDkIIFolder(), new File(Main.getDkIIFolder()
                + PathUtils.DKII_MAPS_FOLDER + "FrontEnd3DLevel.kwd"));
        if (loadingScreen != null) {
            loadingScreen.setProgress(0.25f);
        }
        AssetUtils.prewarmAssets(kwdFile, assetManager, app);
        // load 3D Front end sound
        SoundsLoader.load(kwdFile.getGameLevel().getSoundCategory(), false);

        // Attach the 3D Front end
        menuNode = new Node("Main menu");
        menuNode.attachChild(new MapLoader(assetManager, kwdFile, new EffectManagerState(kwdFile, assetManager), null, new ObjectLoader(kwdFile, null)) {
            @Override
            protected void updateProgress(float progress) {
                if (loadingScreen != null) {
                    loadingScreen.setProgress(0.25f + progress * 0.75f);
                }
            }
        }.load(assetManager, kwdFile));
        if (loadingScreen != null) {
            loadingScreen.setProgress(1.0f);
        }

        // Init the skirmish and multiplayer maps selector
        mapSelector = new MapSelector();
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

        screen = new MainMenuScreenController(this);
        this.app.getNifty().registerScreenController(screen);
    }

    /**
     * Load the initial main menu camera position
     */
    private void loadCameraStartLocation() {
        Player player = kwdFile.getPlayer(Player.KEEPER1_ID);
        startLocation = WorldUtils.pointToVector3f(player.getStartingCameraX(), player.getStartingCameraY());
        startLocation.addLocal(0, MapLoader.FLOOR_HEIGHT, 0);

        // Set the actual camera location
        loadCameraStartLocation("EnginePath250");
    }

    /**
     * Loads and sets up the starting camera position from the given transition
     *
     * @param transition the transition
     */
    private void loadCameraStartLocation(String transition) {
        CameraSweepData csd = AssetUtils.loadCameraSweep(assetManager, transition);
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

        shutdownMultiplayer();

        super.cleanup();
    }

    /**
     * Initialize the start menu, sets the menu scene in place & sets the
     * controls and start screen
     */
    private void initializeMainMenu() {

        // Set the processors & scene
        app.enqueue(() -> {
            MainMenuState.this.app.setViewProcessors();
            rootNode.attachChild(menuNode);

            // Start screen, do this here since another state may have just changed to empty screen -> have to do it like this, delayed
            MainMenuState.this.screen.goToScreen(MainMenuScreenController.SCREEN_START_ID);
            return null;
        });

        // Enable cursor
        app.getInputManager().setCursorVisible(true);
        if (Main.getUserSettings().getSettingBoolean(Settings.Setting.USE_CURSORS)) {
            app.getInputManager().setMouseCursor(CursorFactory.getCursor(CursorFactory.CursorType.POINTER, assetManager));
        }

        // Set the camera position
        loadCameraStartLocation();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!isInitialized()) {
            return;
        }

        if (enabled) {

            // If this is the first time, we might have to load the menu
            if (menuNode == null) {

                // Set up the loading screen
                SingleBarLoadingState loader = new SingleBarLoadingState() {
                    @Override
                    public Void onLoad() {
                        loadMenuScene(this, MainMenuState.this.assetManager, MainMenuState.this.app);
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

            screen.goToScreen(MainMenuScreenController.SCREEN_EMPTY_ID);
        }
    }

    protected void createLocalLobby() {
        LocalLobby localLobby = new LocalLobby();
        initLobby(false, null, localLobby, localLobby, false);
    }

    public void multiplayerCreate(String game, int port, String player) {

        // Create connector
        ConnectionState connectionState = new ConnectionState(null, port, player, true, game) {
            @Override
            protected void onLoggedOn(boolean loggedIn) {
                super.onLoggedOn(loggedIn);

                initLobby(true, getServerInfo(), getLobbyService(), getLobbyClientService(), true);
            }
        };
        connectionState.addConnectionErrorListener(connectionErrorListener);
        stateManager.attach(connectionState);

        // Save player name & game name
        try {
            Main.getUserSettings().setSetting(Setting.PLAYER_NAME, player);
            Main.getUserSettings().setSetting(Setting.GAME_NAME, game);
            Main.getUserSettings().save();
        } catch (IOException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to save user settings!", ex);
        }
    }

    public void multiplayerConnect(String hostAddress, String player) {
        String[] address = hostAddress.split(":");
        String host = address[0];
        Integer port = (address.length == 2) ? Integer.valueOf(address[1]) : Main.getUserSettings().getSettingInteger(Setting.MULTIPLAYER_LAST_PORT);

        // Connect, connection is lazy
        ConnectionState connectionState = new ConnectionState(host, port, player) {
            @Override
            protected void onLoggedOn(boolean loggedIn) {
                super.onLoggedOn(loggedIn);

                initLobby(true, getServerInfo(), getLobbyService(), getLobbyClientService(), true);
            }
        };
        connectionState.addConnectionErrorListener(connectionErrorListener);
        stateManager.attach(connectionState);

        try {

            // Save player name & last connection
            Main.getUserSettings().setSetting(Setting.PLAYER_NAME, player);
            Main.getUserSettings().setSetting(Setting.MULTIPLAYER_LAST_IP, hostAddress);
            Main.getUserSettings().save();

        } catch (IOException ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to save user settings!", ex);
        }
    }

    private void initLobby(boolean online, String serverInfo, LobbyService lobbyService, LobbyClientService lobbyClientService, boolean doTransition) {

        // Create and attach the lobby services
        LobbyState lobbyState = new LobbyState(online, serverInfo, lobbyService, lobbyClientService, mapSelector);
        stateManager.attach(lobbyState);

        if (doTransition) {
            app.enqueue(() -> {
                screen.goToScreen("skirmishLobby");
            });
        }
    }

    public void shutdownMultiplayer() {
        ConnectionState connectionState = stateManager.getState(ConnectionState.class);
        if (connectionState != null) {
            connectionState.addConnectionErrorListener(connectionErrorListener);
            stateManager.detach(connectionState);
        }
        LobbyState lobbyState = stateManager.getState(LobbyState.class);
        if (lobbyState != null) {
            stateManager.detach(lobbyState);
        }
    }

    public void chatTextSend(final String text) {
        ChatClientService chatService = getChatService();
        if (chatService != null) {
            chatService.sendMessage(text);
        } else {
            logger.warning("Connection not initialized!");
        }
    }

    public ChatClientService getChatService() {
        ConnectionState connectionState = getConnectionState();
        if (connectionState != null) {
            return connectionState.getService(ChatClientService.class);
        }
        return null;
    }

    public boolean isHosting() {
        LobbyState lobbyState = stateManager.getState(LobbyState.class);
        if (lobbyState != null) {
            return lobbyState.isHosting();
        }
        return false;
    }

    private ConnectionState getConnectionState() {
        return stateManager.getState(ConnectionState.class);
    }

    public LobbyState getLobbyState() {
        return stateManager.getState(LobbyState.class);
    }

    /**
     * Called by the GUI, start the selected level
     *
     * @param type where level selected. @TODO change campaign like others or
     * otherwise
     */
    public void startLevel(String type) {
        GameState gameState;
        if ("campaign".equals(type.toLowerCase())) {

            // Create the level state
            gameState = new GameState(selectedLevel);
        } else {
            logger.log(Level.WARNING, "Unknown type of Level {0}", type);
            return;
        }

        // Start the game
        setEnabled(false);
        stateManager.attach(gameState);
    }

    /**
     * Plays a movie file
     *
     * @param movieFile the movie filename that should be played. No extension!
     */
    public void playMovie(String movieFile) {
        try {
            MovieState movieState = new MovieState(getDkIIFolder() + PathUtils.DKII_MOVIES_FOLDER + movieFile + ".TGQ") {
                @Override
                protected void onPlayingEnd() {
                    inputManager.setCursorVisible(true);
                }
            };
            stateManager.attach(movieState);
            inputManager.setCursorVisible(false);
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Failed to initiate playing " + movieFile + "!", e);
        }
    }

    /**
     * Does a cinematic transition and opens up a specified screen
     *
     * @param transition name of the transition (without file extension)
     * @param screen the screen name
     * @param transitionStatic set start static location of camera after
     * transition
     */
    protected void doTransitionAndGoToScreen(final String transition, final String screen, final String transitionStatic) {

        // Remove the current screen
        this.screen.goToScreen(MainMenuScreenController.SCREEN_EMPTY_ID);

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
                MainMenuState.this.screen.goToScreen(screen);
            }
        });
        stateManager.attach(c);
        c.play();
    }

    public void restart() {
        app.restart();
    }

    public void quitToOS() {
        app.stop();
    }

    /**
     * Campaign level selected, transition the screen and display the briefing
     *
     * @param selectedLevel the selected level
     */
    protected void selectCampaignLevel(FrontEndLevelControl selectedLevel) {
        this.selectedLevel = selectedLevel.getLevel();
        screen.doTransition("253", "briefing", null);
    }

    /**
     * Stops the level briefing sound
     */
    protected void clearLevelBriefingNarration() {

        // Quit playing the sound
        if (levelBriefing != null && levelBriefing.getStatus() == AudioSource.Status.Playing) {
            levelBriefing.stop();
        }
        levelBriefing = null;
    }

    protected List<MyDisplayMode> getResolutions(GraphicsDevice device) {

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
     * Init skirmish players
     */
    protected void initSkirmishPlayers() {
        skirmishPlayers.clear();

        Keeper keeper = new Keeper(false, "Player", Player.KEEPER1_ID, app);
        skirmishPlayers.add(keeper);
        keeper = new Keeper(true, null, Player.KEEPER2_ID, app);
        skirmishPlayers.add(keeper);
    }

    public void doDebriefing(GameResult result) {
        setEnabled(true);
        if (selectedLevel != null && result != null) {
            screen.showDebriefing(result);
        } else {
            screen.goToScreen(MainMenuScreenController.SCREEN_START_ID);
        }
    }

    /**
     * See if the map thumbnail exist, otherwise create one TODO maybe move to
     * KwdFile class ???
     *
     * @param map
     * @return path to map thumbnail file
     */
    protected String getMapThumbnail(KwdFile map) {

        // See if the map thumbnail exist, otherwise create one
        String asset = AssetsConverter.MAP_THUMBNAILS_FOLDER + File.separator + ConversionUtils.stripFileName(map.getGameLevel().getName()) + ".png";
        if (assetManager.locateAsset(new TextureKey(asset)) == null) {

            // Generate
            try {
                AssetsConverter.genererateMapThumbnail(map, AssetsConverter.getAssetsFolder() + AssetsConverter.MAP_THUMBNAILS_FOLDER + File.separator);
            } catch (Exception e) {
                logger.log(java.util.logging.Level.WARNING, "Failed to generate map file out of {0}!", map);
                asset = "Textures/Unique_NoTextureName.png";
            }
        }
        return asset;
    }

    private class MainMenuConnectionErrorListener implements ConnectionErrorListener {

        @Override
        public void showError(String title, String message, Throwable e, boolean fatal) {
            app.enqueue(() -> {
                screen.showError(title, message);
            });
        }

    }
}
