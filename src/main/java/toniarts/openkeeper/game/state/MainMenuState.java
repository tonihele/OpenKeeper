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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Paths;
import java.util.Collections;
import toniarts.openkeeper.Main;
import static toniarts.openkeeper.Main.getDkIIFolder;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataEntry;
import toniarts.openkeeper.cinematics.Cinematic;
import toniarts.openkeeper.game.MapSelector;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.GeneralLevel;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.data.Settings.Setting;
import toniarts.openkeeper.game.network.chat.ChatClientService;
import toniarts.openkeeper.game.state.ConnectionState.ConnectionErrorListener;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.game.state.lobby.LobbyClientService;
import toniarts.openkeeper.game.state.lobby.LobbyService;
import toniarts.openkeeper.game.state.lobby.LobbyState;
import toniarts.openkeeper.game.state.lobby.LocalLobby;
import toniarts.openkeeper.game.state.session.LocalGameSession;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.video.MovieState;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.map.MapViewController;
import toniarts.openkeeper.view.text.TextParser;
import toniarts.openkeeper.view.text.TextParserService;
import toniarts.openkeeper.view.map.construction.FrontEndLevelControl;

/**
 * The main menu state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MainMenuState extends AbstractAppState {

    private static final Logger logger = System.getLogger(MainMenuState.class.getName());

    protected Main app;
    protected Node rootNode;
    protected AssetManager assetManager;
    protected AppStateManager stateManager;
    protected InputManager inputManager;
    private final MainMenuScreenController screen;
    protected Node menuNode;
    protected GeneralLevel selectedLevel;
    protected AudioNode levelBriefing;

    private KwdFile kwdFile;
    protected final MainMenuInteraction listener;
    private Vector3f startLocation;
    protected MapSelector mapSelector;
    private EntityData mainMenuEntityData;
    private MainMenuEntityViewState mainMenuEntityViewState;
    private GameController gameController;
    private final MainMenuConnectionErrorListener connectionErrorListener = new MainMenuConnectionErrorListener();

    /**
     * (c) Construct a MainMenuState, you should only have one of these. Disable when not in use.
     *
     * @param enabled whether to load the menu scene now, or later when needed (has its own loading screen
     * here)
     * @param assetManager asset manager for loading the screen
     * @param app the main application
     * @throws java.io.IOException may fail to load the main menu map scene
     */
    public MainMenuState(final boolean enabled, final AssetManager assetManager, final Main app)
            throws IOException {

        listener = new MainMenuInteraction(this);
        super.setEnabled(enabled);

        if (enabled) {
            loadMenuScene(null, assetManager, app);
        }

        screen = new MainMenuScreenController(this, app.getNifty());
        app.getNifty().registerScreenController(screen);
    }

    /**
     * Loads up the main menu 3D scene
     *
     * @param loadingScreen optional loading screen
     * @param assetManager asset manager
     */
    private void loadMenuScene(final SingleBarLoadingState loadingScreen, final AssetManager assetManager, final Main app) throws IOException {

        // Load the 3D Front end
        kwdFile = new KwdFile(Main.getDkIIFolder(), Paths.get(PathUtils.getRealFileName(
                Main.getDkIIFolder() + PathUtils.DKII_MAPS_FOLDER, "FrontEnd3DLevel.kwd")));
        if (loadingScreen != null) {
            loadingScreen.setProgress(0.25f);
        }
        AssetUtils.prewarmAssets(kwdFile, assetManager, app);

        // Load 3D Front end sound
        SoundsLoader.load(kwdFile.getGameLevel().getSoundCategory(), false);

        // Attach the 3D Front end
        mainMenuEntityData = new DefaultEntityData();
        menuNode = new Node("Main menu");
        gameController = new GameController(kwdFile, Collections.emptyList(), mainMenuEntityData, kwdFile.getVariables(), new MainMenuPlayerService());
        gameController.createNewGame();

        // Create the actual map
        MapViewController mapLoader = new MapViewController(assetManager, kwdFile, gameController.getGameWorldController().getMapController(), Player.KEEPER1_ID) {

            @Override
            protected void updateProgress(float progress) {
                if (loadingScreen != null) {
                    loadingScreen.setProgress(0.25f + progress * 0.75f);
                }
            }

        };
        menuNode.attachChild(mapLoader.load(assetManager, kwdFile));
        if (loadingScreen != null) {
            loadingScreen.setProgress(1.0f);
        }
        mainMenuEntityViewState = new MainMenuEntityViewState(kwdFile, assetManager, mainMenuEntityData, Player.KEEPER1_ID, new TextParserService(gameController.getGameWorldController().getMapController(), null), menuNode);
        mainMenuEntityViewState.setEnabled(false);
        app.getStateManager().attach(mainMenuEntityViewState);

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
    }

    /**
     * Load the initial main menu camera position
     */
    private void loadCameraStartLocation() {
        Player player = kwdFile.getPlayer(Player.KEEPER1_ID);
        startLocation = WorldUtils.pointToVector3f(player.getStartingCameraX(), player.getStartingCameraY());
        startLocation.addLocal(0, WorldUtils.FLOOR_HEIGHT, 0);

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
        Cinematic.applyCameraSweepEntry(app.getCamera(), startLocation, entry, app.getListener());
    }

    @Override
    public void cleanup() {
        if (mainMenuEntityViewState != null) {
            stateManager.detach(mainMenuEntityViewState);
            mainMenuEntityViewState = null;
        }

        // Clear sound
        clearLevelBriefingNarration();

        // Detach our start menu
        if (menuNode != null) {
            rootNode.detachChild(menuNode);
            menuNode = null;
        }

        shutdownMultiplayer();

        if (gameController != null) {
            gameController.stop();
            gameController = null;
        }

        super.cleanup();
    }

    /**
     * Initialize the start menu, sets the menu scene in place & sets the controls and start screen
     */
    private void initializeMainMenu() {

        // Set the processors & scene
        mainMenuEntityViewState.setEnabled(true);
        MainMenuState.this.app.setViewProcessors();
        rootNode.attachChild(menuNode);

        app.enqueue(() -> {

            // Start screen, do this here since another state may have just changed to empty screen -> have to do it like this, delayed
            MainMenuState.this.screen.goToScreen(MainMenuScreenController.SCREEN_START_ID);
            return null;
        });

        // Enable cursor
        app.getInputManager().setCursorVisible(true);
        if (Main.getUserSettings().getBoolean(Settings.Setting.USE_CURSORS)) {
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

        stateManager.getState(SoundState.class).changeBackground(SoundState.Background.AMBIENCE);
        stateManager.getState(SoundState.class).setEnabled(true);

        if (enabled) {

            // If this is the first time, we might have to load the menu
            if (menuNode == null) {

                // Set up the loading screen
                SingleBarLoadingState loader = new SingleBarLoadingState(app, "Single Loading") {

                    @Override
                    public void onLoad() {
                        try {
                            loadMenuScene(this, MainMenuState.this.assetManager, MainMenuState.this.app);
                        } catch (IOException ex) {
                            logger.log(Level.ERROR, "Failed to load the main menu scene!", ex);
                        }
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

            stateManager.getState(MainMenuEntityViewState.class).setEnabled(false);
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
            logger.log(Level.ERROR, "Failed to save user settings!", ex);
        }
    }

    public void multiplayerConnect(String hostAddress, String player) {
        String[] address = hostAddress.split(":");
        String host = address[0];
        int port = (address.length == 2) ? Integer.parseInt(address[1]) : Main.getUserSettings().getInteger(Setting.MULTIPLAYER_LAST_PORT);

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
            logger.log(Level.ERROR, "Failed to save user settings!", ex);
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
            logger.log(Level.WARNING, "Connection not initialized!");
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
     * @param type where level selected. @TODO change campaign like others or otherwise
     */
    public void startLevel(String type) {
        if ("campaign".equals(type.toLowerCase())) {

            // Create the level state
            LocalGameSession.createLocalGame(selectedLevel.getKwdFile(), true, stateManager, app);
        } else {
            logger.log(Level.WARNING, "Unknown type of Level {0}", type);
            return;
        }

        // Start the game
        setEnabled(false);
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
            logger.log(Level.WARNING, "Failed to initiate playing " + movieFile + "!", e);
        }
    }

    /**
     * Does a cinematic transition and opens up a specified screen
     *
     * @param transition name of the transition (without file extension)
     * @param screen the screen name
     * @param transitionStatic set start static location of camera after transition
     */
    protected void doTransitionAndGoToScreen(final String transition, final String screen, final String transitionStatic) {

        // Remove the current screen
        this.screen.goToScreen(MainMenuScreenController.SCREEN_EMPTY_ID);

        // Do cinematic transition
        Cinematic c = new Cinematic(assetManager, app.getCamera(), app.getListener(), startLocation, transition, menuNode, stateManager);
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

    public void doDebriefing(GameResult result) {
        setEnabled(true);
        if (selectedLevel != null && result != null) {
            screen.showDebriefing(result);
        } else {
            screen.goToScreen(MainMenuScreenController.SCREEN_START_ID);
        }
    }

    /**
     * See if the map thumbnail exist, otherwise create one TODO maybe move to KwdFile class ???
     *
     * @param map
     * @return path to map thumbnail file
     */
    protected String getMapThumbnail(KwdFile map) {

        // See if the map thumbnail exist, otherwise create one
        String asset = AssetsConverter.MAP_THUMBNAILS_FOLDER + File.separator + PathUtils.stripFileName(map.getGameLevel().getName()) + ".png";
        if (assetManager.locateAsset(new TextureKey(asset)) == null) {

            // Generate
            try {
                AssetsConverter.genererateMapThumbnail(map, AssetsConverter.getAssetsFolder() + AssetsConverter.MAP_THUMBNAILS_FOLDER + File.separator);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to generate map file out of {0}!", map);
                asset = "Textures/Unique_NoTextureName.png";
            }
        }
        return asset;
    }

    private final class MainMenuConnectionErrorListener implements ConnectionErrorListener {

        @Override
        public void showError(String title, String message, Throwable e, boolean fatal) {
            app.enqueue(() -> {
                screen.showError(title, message);
            });
        }

    }

    /**
     * Main menu version of the player entity view state
     */
    private static final class MainMenuEntityViewState extends PlayerEntityViewState {

        public MainMenuEntityViewState(KwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId, TextParser textParser, Node rootNode) {
            super(kwdFile, assetManager, entityData, playerId, textParser, rootNode);

            setId("MainMenu: " + playerId);
        }

    }

    private static final class MainMenuPlayerService implements PlayerService {

        @Override
        public void setWidescreen(boolean enable, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playSpeech(int speechId, boolean showText, boolean introduction, int pathId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isInTransition() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void doTransition(short pathId, Vector3f start, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void flashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void rotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void showMessage(int textId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void zoomViewToPoint(Vector3f point, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void zoomViewToEntity(EntityId entityId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setGamePaused(boolean paused) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void showUnitFlower(EntityId entityId, int interval, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPossession(EntityId target, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
