/*
 * Copyright (C) 2014-2017 OpenKeeper
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
import com.jme3.app.state.AppStateManager;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.event.EventBus;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalCreatureLimit;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.PlayerController;
import toniarts.openkeeper.game.data.CampaignLevel;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.event.ClientEvents;
import toniarts.openkeeper.game.event.GeneratedEffect;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.game.state.loading.IPlayerLoadingProgress;
import toniarts.openkeeper.game.state.loading.LoadingScope;
import toniarts.openkeeper.game.state.loading.LoadingState;
import toniarts.openkeeper.game.state.loading.MultiplayerLoadingState;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.PlayerMapViewState;
import toniarts.openkeeper.view.SystemMessageState;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.minimap.FullscreenMapState;
import toniarts.openkeeper.view.minimap.MinimapPanelState;
import toniarts.openkeeper.view.text.GuardPostCounter;
import toniarts.openkeeper.view.text.TextParser;
import toniarts.openkeeper.view.text.TextParserService;

import javax.annotation.Nullable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The game client state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameClientState extends AbstractPauseAwareState {

    private static final Logger logger = System.getLogger(GameClientState.class.getName());

    private final Main app;

    private AppStateManager stateManager;

    private final IKwdFile kwdFile;

    private final Map<Short, Keeper> players = new TreeMap<>();
    private final Map<Short, IPlayerController> playerControllers = new TreeMap<>();
    private final Object loadingObject = new Object();
    private volatile boolean gameStarted = false;

    private final Short playerId;
    private final boolean multiplayer;
    private final List<ClientInfo> clientInfos;
    private final Map<Short, String> networkPlayerNames;
    private IPlayerLoadingProgress loadingState;
    private final GameSessionClientService gameClientService;
    private final GameSessionListenerImpl gameSessionListener = new GameSessionListenerImpl();
    private IMapInformation mapInformation;
    private IFogOfWarInformation fogOfWarInformation;
    private PlayerState playerState;
    @Nullable
    private final CampaignLevel campaignLevel;

    private PlayerMapViewState playerMapViewState;
    private AmbienceState ambienceState;
    private RoomSoundState roomSoundState;
    private volatile EntityId possessedCreature;
    private PlayerEntityViewState playerModelViewState;
    private MinimapPanelState minimapPanelState;
    private FullscreenMapState fullscreenMapState;
    private MusicLevelState musicLevelState;
    private EffectManagerState effectManagerState;
    private TextParser textParser;
    private GuardPostCounter guardPostCounter;
    private volatile EntitySet portalLimits;
    private final LoadingScope loading;
    private final CompletionStage<Void> screenLoadingStopped;
    private volatile CompletionStage<Boolean> prewarming = CompletableFuture.completedFuture(false);
    private final CompletableFuture<Void> loadingStopped = new CompletableFuture<>();
    private boolean initializationStarted;
    private boolean closing;
    private boolean cleaned;
    private boolean readyPending;
    private final ArrayDeque<CameraTransition> pendingCameraTransitions = new ArrayDeque<>();
    private record CameraTransition(short pathId, Vector3f start) { }

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param playerId our player ID
     * @param players players participating in this game
     * @param gameClientService client services
     * @param app the main application
     * @param campaignLevel the campaign level, or {@code null} for non-campaign
     */
    public GameClientState(IKwdFile level, Short playerId, List<ClientInfo> players, GameSessionClientService gameClientService, Main app, @Nullable CampaignLevel campaignLevel) {
        this.kwdFile = level;
        this.gameClientService = gameClientService;
        this.playerId = playerId;
        this.app = (Main) app;
        this.stateManager = app.getStateManager();
        loading = new LoadingScope(task -> app.enqueue(task),
                failure -> app.handleError("Failed to load the client", failure));
        this.campaignLevel = campaignLevel;
        this.clientInfos = List.copyOf(players);

        // Set multiplayer
        int humanPlayers = 0;
        for (ClientInfo clientInfo : players) {
            if (!clientInfo.getKeeper().isAi()) {
                humanPlayers++;
                if (humanPlayers > 1) {
                    break;
                }
            }
        }
        multiplayer = (humanPlayers > 1);

        // Cache the network names, only used in multiplayer
        Map<Short, String> names = new HashMap<>();
        if (multiplayer) {
            for (ClientInfo clientInfo : players) {
                if (clientInfo.getKeeper() != null && clientInfo.getName() != null && !clientInfo.getName().isBlank()) {
                    names.putIfAbsent(clientInfo.getKeeper().getId(), clientInfo.getName());
                }
            }
        }
        this.networkPlayerNames = Map.copyOf(names);

        // Create the loading state
        loadingState = createLoadingState(app);
        screenLoadingStopped = ((LoadingState) loadingState).whenLoadingStopped();

        // Add the listener
        gameClientService.addGameSessionListener(gameSessionListener);

        // Tell that we are ready to start receiving game data
        gameClientService.markReady();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        initializationStarted = true;
        if (loading.isClosed()) return;
        this.stateManager = stateManager;

        // Attach the loading state finally
        if (!gameStarted) {
            synchronized (loadingObject) {
                if (!gameStarted) {
                    stateManager.attach(loadingState);
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    private void detachRelatedAppStates() {
        if (ambienceState != null) {
            detachOwned(ambienceState);
            ambienceState = null;
        }
        if (roomSoundState != null) {
            detachOwned(roomSoundState);
            roomSoundState = null;
        }
        detachOwned(minimapPanelState);
        detachOwned(fullscreenMapState);
        detachOwned(effectManagerState);
        detachOwned(guardPostCounter);
        detachOwned(playerModelViewState);
        detachOwned(playerMapViewState);
        detachOwned(playerState);
        if (musicLevelState != null) {
            detachOwned(musicLevelState);
        }
    }

    private void detachOwned(com.jme3.app.state.AppState state) {
        if (state != null && !stateManager.detach(state)) state.stateDetached(stateManager);
    }

    /**
     * If you are getting rid of the game state, use this so that all the
     * related states are detached on the same render loop. Otherwise the app
     * might crash.
     */
    public void detach() {
        closeClient();
        stateManager.detach(this);
    }

    @Override public void stateDetached(AppStateManager stateManager) {
        closeClient();
        // jME skips cleanup for states detached from its initialization queue.
        if (!initializationStarted) cleanup();
    }

    private void closeClient() {
        if (closing) return;
        closing = true;
        readyPending = false;
        pendingCameraTransitions.clear();
        loading.close();
        gameClientService.removeGameSessionListener(gameSessionListener);

        // If we are stuck loading
        synchronized (loadingObject) {
            loadingObject.notifyAll();
        }

        CompletionStage<Void> mapStopped = playerMapViewState == null
                ? CompletableFuture.completedFuture(null) : playerMapViewState.whenLoadingStopped();
        detachOwned(loadingState);
        loadingState = null;
        detachRelatedAppStates();

        if (portalLimits != null) { portalLimits.release(); portalLimits = null; }
        loading.whenStopped().thenCompose(unused -> prewarming.handle((ready, failure) -> null))
                .thenCombine(mapStopped, (a, b) -> null).thenCombine(screenLoadingStopped, (a, b) -> null)
                .whenComplete((unused, failure) -> {
                    if (failure == null) loadingStopped.complete(null);
                    else loadingStopped.completeExceptionally(failure);
                });

        playerState = null;
        playerMapViewState = null;
        playerModelViewState = null;
        minimapPanelState = null;
        fullscreenMapState = null;
        musicLevelState = null;
        effectManagerState = null;
    }

    /** Actual decoding/prewarm retirement, without waiting on the render thread or forcing GPU completion. */
    public CompletionStage<Void> whenLoadingStopped() { return loadingStopped.minimalCompletionStage(); }

    @Override
    public void update(float tpf) {
        if (loading.isClosed()) return;
        // enqueue() drains recursively in the same frame. Initialization must be observed here,
        // after AppStateManager.initializePending(), before preloading the live scene or starting play.
        if (readyPending && playerState.isInitialized() && playerModelViewState.isInitialized()
                && playerMapViewState.isInitialized() && minimapPanelState.isInitialized()
                && fullscreenMapState.isInitialized() && effectManagerState.isInitialized()
                && guardPostCounter.isInitialized()) {
            readyPending = false;
            app.getRenderManager().preloadScene(app.getRootNode());
            gameClientService.loadComplete();
        }
        if (!pendingCameraTransitions.isEmpty()) {
            PlayerCameraState camera = stateManager.getState(PlayerCameraState.class);
            if (camera != null && camera.isInitialized()) {
                CameraTransition transition = pendingCameraTransitions.removeFirst();
                gameSessionListener.startTransition(transition.pathId(), transition.start());
            }
        }
        if (portalLimits != null && portalLimits.applyChanges()) {
            copyPortalLimits(portalLimits.getAddedEntities());
            copyPortalLimits(portalLimits.getChangedEntities());
        }
    }

    /**
     * Gather the keeper stats the server keeps in the entities to our keepers
     */
    private void copyPortalLimits(Collection<Entity> entities) {
        for (Entity entity : entities) {
            Keeper keeper = getPlayer(entity.get(Owner.class).ownerId);
            if (keeper != null) {
                PortalCreatureLimit limit = entity.get(PortalCreatureLimit.class);
                keeper.setPortalCreatures(limit.attracted, limit.capacity);
            }
        }
    }

    @Override
    public void cleanup() {
        if (cleaned) return;
        cleaned = true;
        closeClient();

        // Signal our exit & close all the connections
        gameClientService.exitGame();
        ConnectionState cs = stateManager.getState(ConnectionState.class);
        if (cs != null) {
            cs.disconnect();
        }

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    /**
     * The name to show for a player: the network name in multiplayer,
     * otherwise "Keeper n"
     */
    public String getPlayerName(Keeper keeper) {
        return networkPlayerNames.getOrDefault(keeper.getId(), "Keeper " + (keeper.getId() - Player.KEEPER1_ID + 1));
    }

    public boolean isMultiplayer() {
        return multiplayer;
    }

    private IPlayerLoadingProgress createLoadingState(Main app) {

        // Create the appropriate loaging screen
        IPlayerLoadingProgress loader;
        if (isMultiplayer()) {
            loader = new MultiplayerLoadingState(app, "Multiplayer") {

                @Override
                public void onLoad() {
                    onGameLoad();
                }

                @Override
                public void onLoadComplete() {
                    onGameLoadComplete();
                }
            };
        } else {
            loader = new SingleBarLoadingState(app, "Singleplayer") {

                @Override
                public void onLoad() {
                    onGameLoad();
                }

                @Override
                public void onLoadComplete() {
                    onGameLoadComplete();
                }
            };
        }
        return loader;
    }

    private void onGameLoadComplete() {
        if (loading.isClosed()) return;
        loadingState = null;

        // Set initialized
        GameClientState.this.initialized = true;

        // Set the processors
        GameClientState.this.app.setViewProcessors();
    }

    private void onGameLoad() {
        try {

            // The game is actually loaded elsewhere but for the visuals we need this
            synchronized (loadingObject) {
                while (!gameStarted && !loading.isClosed()) loadingObject.wait();
            }

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    public IKwdFile getLevelData() {
        return kwdFile;
    }

    public Keeper getPlayer(short playerId) {
        return players.get(playerId);
    }

    public Keeper getPlayer() {
        return players.get(playerId);
    }

    public Collection<Keeper> getPlayers() {
        return players.values();
    }

    public IPlayerController getPlayerController(short playerId) {
        return playerControllers.get(playerId);
    }

    /**
     * Get level variable value
     *
     * @param variable the variable type
     * @return variable value
     */
    public float getLevelVariable(Variable.MiscVariable.MiscType variable) {
        // TODO: player is able to change these, so need a wrapper and store these to GameState
        return kwdFile.getVariables().get(variable).getValue();
    }

    /**
     * Gets a text parser that can fill up the parameters in translations
     *
     * @return a text parser
     */
    public TextParser getTextParser() {
        return textParser;
    }

    private final class GameSessionListenerImpl implements GameSessionListener {

        @Override
        public void onGameDataLoaded(Collection<Keeper> players) {
            List<Keeper> keepers = List.copyOf(players);
            loading.start("GameDataClientLoader", () -> {
                prewarming = AssetUtils.prewarmAssets(kwdFile, app.getAssetManager(), app, () -> !loading.isClosed());
                loading.publish(() -> initializeGameData(keepers));
            });
        }

        private void initializeGameData(Collection<Keeper> players) {
            for (Keeper keeper : players) {
                keeper.setPlayer(kwdFile.getPlayer(keeper.getId()));
                GameClientState.this.players.put(keeper.getId(), keeper);
                GameClientState.this.playerControllers.put(keeper.getId(), new PlayerController(kwdFile, keeper, kwdFile.getImp(), gameClientService.getEntityData(), kwdFile.getVariables()));
            }
            EntitySet limits = gameClientService.getEntityData().getEntities(Owner.class, PortalCreatureLimit.class);
            copyPortalLimits(limits);
            portalLimits = limits;

            playerMapViewState = new PlayerMapViewState(app, kwdFile, app.getAssetManager(), players, gameClientService.getEntityData(), playerId,
                    true, // Built, but hidden until the viewer possesses a creature
                    () -> loading.publish(this::initializePlayerViews), (entityId) -> {
                        loading.publish(() -> {
                            SystemMessageState systemMessageState = stateManager.getState(SystemMessageState.class);
                            if (systemMessageState != null) {
                                systemMessageState.addMessage(SystemMessageState.MessageType.FIGHT, "Enemy creature sighted!");
                            }
                        });
                    }) {

                private float lastProgress = 0;

                @Override
                protected void updateProgress(float progress) {
                    if (loading.isClosed()) return;
                    if (progress - lastProgress >= 0.01f || progress >= 1) {
                        onLoadStatusUpdate(progress, playerId);
                        gameClientService.loadStatus(progress);
                        lastProgress = progress;
                    }
                }
            };
            mapInformation = playerMapViewState.getMapInformation();
            fogOfWarInformation = playerMapViewState.getFogOfWarInformation();
            stateManager.attach(playerMapViewState);
        }

        private void initializePlayerViews() {
            if (playerState != null) return;
            // The detached map decoder has returned. Scene/UI construction and thing lights are render-owned.
            playerState = new PlayerState(playerId, kwdFile, gameClientService.getEntityData(), false, app, campaignLevel);
            stateManager.attach(playerState);
            textParser = new TextParserService(mapInformation, playerMapViewState.getRoomsInformation());
            playerModelViewState = new PlayerEntityViewState(kwdFile, app.getAssetManager(), gameClientService.getEntityData(), playerId, textParser, app.getRootNode(), mapInformation.getMapData(), fogOfWarInformation);
            stateManager.attach(playerModelViewState);
            guardPostCounter = new GuardPostCounter(gameClientService.getEntityData(), playerId,
                    trapId -> kwdFile.getTrapById(trapId).getFlags().contains(Trap.TrapFlag.GUARD_POST));
            stateManager.attach(guardPostCounter);
            textParser.getRoomTextParser().setPatrolRouteCounter(owner -> guardPostCounter.getAsInt());
            playerMapViewState.addFogOfWarTilesDirtyListener(playerModelViewState::onTilesDirty);
            minimapPanelState = new MinimapPanelState(app, mapInformation, fogOfWarInformation, playerMapViewState.getRoomsInformation(),
                    gameClientService.getEntityData(), GameClientState.this.players.get(playerId),
                    getLevelVariable(Variable.MiscVariable.MiscType.DUNGEON_HEART_REPORTING_DISTANCE_TILES));
            stateManager.attach(minimapPanelState);
            effectManagerState = new EffectManagerState(kwdFile, app.getAssetManager());
            stateManager.attach(effectManagerState);
            fullscreenMapState = new FullscreenMapState(app, mapInformation, fogOfWarInformation, playerMapViewState.getRoomsInformation(),
                    gameClientService.getEntityData(), kwdFile, GameClientState.this.players.values(), GameClientState.this::getPlayerName);
            stateManager.attach(fullscreenMapState);

            readyPending = true;

        }

        @Override
        public void onLoadComplete(short keeperId) {
            loading.publish(() -> { if (loadingState != null) loadingState.setProgress(1f, keeperId); });
        }

        @Override
        public void onLoadStatusUpdate(float progress, short keeperId) {
            loading.publish(() -> { if (loadingState != null) loadingState.setProgress(progress, keeperId); });
        }

        @Override
        public void onGameStarted() {
            loading.publish(() -> {
                SoundState soundState = stateManager.getState(SoundState.class);
                soundState.setKwdFile(kwdFile);
                soundState.setEnabled(true);
                soundState.startLevelBackground();
                musicLevelState = new MusicLevelState(gameClientService.getEntityData(), players.get(playerId), kwdFile);
                stateManager.attach(musicLevelState);
                playerState.setEnabled(true);

                // The background sounds of the level
                if (playerMapViewState != null) {
                    ambienceState = new AmbienceState(kwdFile, playerMapViewState,
                            stateManager.getState(SoundState.class).getMusicLevel()::getIntensity);
                    stateManager.attach(ambienceState);
                }

                roomSoundState = new RoomSoundState(gameClientService.getEntityData(), kwdFile);
                stateManager.attach(roomSoundState);

                // Release the lock and enter to the game phase
                synchronized (loadingObject) {
                    gameStarted = true;
                    loadingObject.notifyAll();
                }
            });
        }

        @Override
        public void onTilesChange(List<Point> updatedTiles) {
            List<Point> owned = List.copyOf(updatedTiles);
            loading.publish(() -> {
                PlayerMapViewState mapViewState = playerMapViewState;
                if (mapViewState != null) {
                    mapViewState.onTilesChange(owned);
                }
            });
        }

        @Override
        public void onTilesReveal(List<Point> points, boolean explore, short keeperId) {
            List<Point> owned = List.copyOf(points);
            loading.publish(() -> {
                PlayerMapViewState mapViewState = playerMapViewState;
                if (mapViewState != null) {
                    mapViewState.onTilesReveal(owned, explore, keeperId);
                }
            });
        }

        @Override
        public void onFogOfWarDisabled(short keeperId) {
            loading.publish(() -> {
                PlayerMapViewState mapViewState = playerMapViewState;
                if (mapViewState != null) {
                    mapViewState.onFogOfWarDisabled(keeperId);
                }
            });
        }

        @Override
        public void onFogOfWarReset(short keeperId) {
            loading.publish(() -> {
                PlayerMapViewState mapViewState = playerMapViewState;
                if (mapViewState != null) {
                    mapViewState.onFogOfWarReset(keeperId);
                }
            });
        }

        @Override
        public void onGoldChange(short keeperId, int gold) {
            loading.publish(() -> {
                Keeper keeper = getPlayer(keeperId);
                if (keeper != null) keeper.setGold(gold);
                if (playerState != null && playerState.getPlayerId() == keeperId) playerState.onGoldChange(keeperId, gold);
            });
        }

        @Override
        public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
            loading.publish(() -> {
                Keeper keeper = getPlayer(keeperId);
                if (keeper != null) {
                    keeper.setMana(mana); keeper.setManaGain(manaGain); keeper.setManaLoose(manaLoose);
                }
                if (playerState != null && playerState.getPlayerId() == keeperId)
                    playerState.onManaChange(keeperId, mana, manaLoose, manaGain);
            });
        }

        @Override
        public void onBuild(short keeperId, List<Point> tiles) {
            List<Point> owned = List.copyOf(tiles);
            loading.publish(() -> {
                PlayerMapViewState mapViewState = playerMapViewState;
                if (mapViewState != null) {
                    mapViewState.onBuild(keeperId, owned);
                }
            });
        }

        @Override
        public void onSold(short keeperId, List<Point> tiles) {
            List<Point> owned = List.copyOf(tiles);
            loading.publish(() -> {
                PlayerMapViewState mapViewState = playerMapViewState;
                if (mapViewState != null) {
                    mapViewState.onSold(keeperId, owned);
                }
            });
        }

        @Override
        public void onGamePaused() {
            loading.publish(() -> {
                playerState.onPaused(true);
                if (playerModelViewState != null) playerModelViewState.setGamePaused(true);
            });
        }

        @Override
        public void onGameResumed() {
            loading.publish(() -> {
                playerState.onPaused(false);
                if (playerModelViewState != null) playerModelViewState.setGamePaused(false);
            });
        }

        @Override
        public void onSetWidescreen(boolean enable) {
            loading.publish(() -> {
                playerState.setWideScreen(enable);
            });
        }

        @Override
        public void onGameEnded(boolean win) {
            loading.publish(() -> playerState.endGame(win));
        }

        @Override
        public void onEffect(int effectId, Vector3f position, float yaw) {
            var effect = new GeneratedEffect(effectId, position, yaw);
            loading.publish(() -> EventBus.publish(ClientEvents.GENERATED_EFFECT, effect));
        }

        @Override
        public void onEvent(GameEventType type, GameEvent.ThingFlag subjectKind, int subjectId, GameEvent.ThingFlag targetKind, int targetId, EntityId entity, Point position) {
            loading.publish(() -> {
                GameEvent event = new GameEvent(type, subjectKind, subjectId, targetKind, targetId, entity, position);
                logger.log(Level.DEBUG, "Event {0} at {1}", event.type, event.position);
                EventBus.publish(ClientEvents.GAME_EVENT, event);
            });
        }

        @Override
        public void onPlaySpeech(int speechId, boolean showText, boolean introduction, int pathId) {
            loading.publish(() -> {
                // TODO: Refactor these, we don't maybe want this logic here, borderline visuals
                stateManager.getState(SoundState.class).attachLevelSpeech(speechId, () -> loading.publish(() -> {
                    SystemMessageState.addMessageIfAttached(stateManager, SystemMessageState.MessageType.INFO, String.format("${level.%d}", speechId - 1));
                    if (showText) {
                        playerState.setText(speechId, introduction, pathId);
                    }
                }));
            });
        }

        @Override
        public void onDoTransition(short pathId, Vector3f start) {

            // Called from the game logic thread, the camera and the scene belong to the render thread
            Vector3f startPoint = start.clone();
            loading.publish(() -> startTransition(pathId, startPoint));
        }

        /**
         * Starts the camera transition on the render thread. If the camera
         * state is not initialized yet, update() retries after a real state-manager boundary.
         */
        private void startTransition(short pathId, Vector3f start) {
            PlayerCameraState cameraState = stateManager.getState(PlayerCameraState.class);
            if (cameraState == null || !cameraState.isInitialized()) {
                pendingCameraTransitions.addLast(new CameraTransition(pathId, start));
                return;
            }

            // TODO: Refactor
            cameraState.doTransition(pathId, start, new CinematicEventListener() {
                @Override
                public void onPlay(CinematicEvent cinematic) {

                }

                @Override
                public void onPause(CinematicEvent cinematic) {

                }

                @Override
                public void onStop(CinematicEvent cinematic) {
                    if (!loading.isClosed()) gameClientService.transitionEnd();
                }
            });
        }

        @Override
        public void onFlashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time) {
            loading.publish(() -> {
                playerState.flashButton(buttonType, targetId, targetButtonType, enabled, time);
            });
        }

        @Override
        public void onRotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time) {
            Vector3f owned = point.clone();
            loading.publish(() -> playerState.rotateViewAroundPoint(owned, relative, angle, time));
        }

        @Override
        public void onShowMessage(int textId) {
            loading.publish(() -> {
                playerState.showMessage(textId);
            });
        }

        @Override
        public void onZoomViewToPoint(Vector3f point) {
            Vector3f owned = point.clone();
            loading.publish(() -> playerState.zoomToPoint(owned));
        }

        @Override
        public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
            List<Point> owned = List.copyOf(points);
            loading.publish(() -> {
                PlayerMapViewState mapViewState = playerMapViewState;
                if (mapViewState != null) {
                    mapViewState.onTileFlash(owned, enabled, keeperId);
                }
            });
        }

        @Override
        public void onZoomViewToEntity(EntityId entityId) {
            loading.publish(() -> {
                playerState.zoomToEntity(entityId, true);
            });
        }

        @Override
        public void onShowUnitFlower(EntityId entityId, int interval) {
            loading.publish(() -> {
                playerModelViewState.showUnitFlower(entityId, interval);
            });
        }

        @Override
        public void onEntityAdded(short keeperId, ResearchableEntity researchableEntity) {
            loading.publish(() -> {
                setResearchableEntity(keeperId, researchableEntity, () -> {
                    playerState.onEntityAdded(keeperId, researchableEntity);
                    });
            });
        }

        private void setResearchableEntity(short keeperId, ResearchableEntity researchableEntity, Runnable notifier) {
            Keeper keeper = getPlayer(keeperId);
            setResearchableEntity(researchableEntity, getResearchableEntitiesList(keeper, researchableEntity));

            if (notifier != null && playerState != null && playerState.getPlayerId() == keeperId) {
                notifier.run();
            }
        }

        @Override
        public void onEntityRemoved(short keeperId, ResearchableEntity researchableEntity) {
            loading.publish(() -> {
                setResearchableEntity(keeperId, researchableEntity, () -> {
                    playerState.onEntityRemoved(keeperId, researchableEntity);
                    });
            });
        }

        @Override
        public void onResearchStatusChanged(short keeperId, ResearchableEntity researchableEntity) {
            loading.publish(() -> {
                setResearchableEntity(keeperId, researchableEntity, () -> {
                    playerState.onResearchStatusChanged(keeperId, researchableEntity);
                    });
            });
        }

        private List<ResearchableEntity> getResearchableEntitiesList(Keeper keeper, ResearchableEntity researchableEntity) {
            List<ResearchableEntity> researchableEntities = null;
            switch (researchableEntity.getResearchableType()) {
                case SPELL: {
                    researchableEntities = keeper.getAvailableSpells();
                    break;
                }
                case DOOR: {
                    researchableEntities = keeper.getAvailableDoors();
                    break;
                }
                case ROOM: {
                    researchableEntities = keeper.getAvailableRooms();
                    break;
                }
                case TRAP: {
                    researchableEntities = keeper.getAvailableTraps();
                    break;
                }
            }

            return researchableEntities;
        }

        private <T extends ResearchableEntity> void setResearchableEntity(T researchableEntity, List<T> researchableEntities) {
            int index = Collections.binarySearch(researchableEntities, researchableEntity, (ResearchableEntity o1, ResearchableEntity o2) -> {
                return getResearchableEntityType(kwdFile, o1.getResearchableType(), o1.getId()).compareTo(getResearchableEntityType(kwdFile, o2.getResearchableType(), o2.getId()));
            });
            if (index < 0) {
                researchableEntities.add(~index, researchableEntity);
            } else if (index >= 0) {
                researchableEntities.set(index, researchableEntity);
            }
        }

        private Comparable getResearchableEntityType(IKwdFile kwdFile, ResearchableType researchableType, short typeId) {
            switch (researchableType) {
                case DOOR: {
                    return kwdFile.getDoorById(typeId);
                }
                case ROOM: {
                    return kwdFile.getRoomById(typeId);
                }
                case SPELL: {
                    return kwdFile.getKeeperSpellById(typeId);
                }
                case TRAP: {
                    return kwdFile.getTrapById(typeId);
                }
            }

            return null;
        }

        @Override
        public void setPossession(EntityId target) {
            loading.publish(() -> {
                // Session callbacks are applied on the owning render thread, including fog/audio state.
                playerState.setPossession(target);
                setPossessedCreature(target);
                if (ambienceState != null) {
                    ambienceState.setPossessing(target != null);
                }
            });
        }
    }

    /**
     * Gets the entity view state of this game. Look this up instead of asking
     * the state manager by class, the main menu's entity view state is a
     * {@link PlayerEntityViewState} too and may be found first.
     *
     * @return the game's entity view state, {@code null} if not loaded
     */
    public PlayerEntityViewState getEntityViewState() {
        return playerModelViewState;
    }

    public IMapInformation getMapClientService() {
        return mapInformation;
    }

    public IFogOfWarInformation getFogOfWarInformation() {
        return fogOfWarInformation;
    }

    /**
     * Get the rooms information, e.g. the number of rooms a player has
     *
     * @return the rooms information
     */
    public IRoomsInformation<?> getRoomsInformation() {
        return playerMapViewState.getRoomsInformation();
    }

    public MinimapPanelState getMinimapPanelState() {
        return minimapPanelState;
    }

    /**
     * Marks (or unmarks) still-unexplored tiles as locally "looking tagged"
     * while the viewer drags a tag/dig selection over them - see
     * {@link IFogOfWarInformation#isPendingTagged(Point)}.
     */
    public void markPendingTaggedTiles(List<Point> points, boolean tagged) {
        if (playerMapViewState != null) {
            playerMapViewState.markPendingTaggedTiles(points, tagged);
        }
    }

    /**
     * Tells the local fog-of-war module which creature (if any) the viewer is
     * currently possessing, so it can bypass fog for the first-person view
     * and keep exploring the possessed creature's tile
     *
     * @param entityId the possessed creature, or {@code null} if possession has ended
     */
    public void setPossessedCreature(EntityId entityId) {
        possessedCreature = entityId;
        if (playerMapViewState != null) {
            playerMapViewState.setPossessedCreature(entityId);
        }
    }

    /**
     * @return the creature the viewer is currently possessing, or
     * {@code null} if not possessing
     */
    public EntityId getPossessedCreature() {
        return possessedCreature;
    }

    /**
     * Shows or hides the map ceiling, which is only wanted while possessing a
     * creature
     *
     * @param visible show the ceiling
     */
    public void setCeilingVisible(boolean visible) {
        if (playerMapViewState != null) {
            playerMapViewState.setCeilingVisible(visible);
        }
    }

    public GameSessionClientService getGameClientService() {
        return gameClientService;
    }

}
