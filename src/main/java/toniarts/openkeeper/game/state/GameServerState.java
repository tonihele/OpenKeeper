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
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.data.CampaignLevel;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.CheatController;
import toniarts.openkeeper.game.controller.ICheatController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.NoopCheatController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.SecretLevels;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.listener.SpecialListener;
import toniarts.openkeeper.game.state.loop.GameLoopManager;
import toniarts.openkeeper.game.state.loop.IGameLoopManager;
import toniarts.openkeeper.game.state.loading.LoadingScope;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The game state that actually runs the game. Has no relation to visuals.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameServerState extends AbstractAppState {

    private static final Logger logger = System.getLogger(GameServerState.class.getName());

    private Main app;

    private volatile AppStateManager stateManager;
    private final LoadingScope loading;
    private final Supplier<? extends IGameLoopManager> gameFactory;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicBoolean startRequested = new AtomicBoolean();
    private final CompletableFuture<Void> stopped = new CompletableFuture<>();
    private final ReentrantReadWriteLock commands = new ReentrantReadWriteLock();
    private final List<IPlayerController> listeningPlayers = new ArrayList<>();
    private boolean cleaned, gameStarted;
    private volatile Throwable closeFailure;
    private volatile boolean gameLoaded = false;

    private final IKwdFile kwdFile;
    private final CampaignLevel levelObject;

    private final boolean campaign;
    private final boolean multiplayer;
    private final GameSessionServerService gameService;
    private IMapController mapController;
    private final MapListener mapListener = new MapListenerImpl();
    private final GameSessionServiceListener gameSessionListener = new GameSessionServiceListenerImpl();
    private final PlayerActionListener playerActionListener = new PlayerActionListenerImpl();
    private final SpecialListener specialListener = new SpecialListenerImpl();
    private volatile IGameLoopManager game;
    private IGameWorldController gameWorldController;
    private ICheatController cheatController;

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players players participating in this game
     * @param campaign whether this is a campaign level or not
     * @param gameService the game service
     * @param campaignLevel the campaign level, or {@code null} for non-campaign
     */
    public GameServerState(IKwdFile level, List<Keeper> players, boolean campaign, GameSessionServerService gameService, @Nullable toniarts.openkeeper.game.data.CampaignLevel campaignLevel) {
        this(level, players, campaign, gameService, campaignLevel, null);
    }

    GameServerState(IKwdFile level, List<Keeper> players, boolean campaign, GameSessionServerService gameService,
            @Nullable CampaignLevel campaignLevel, Supplier<? extends IGameLoopManager> factory) {
        this.kwdFile = level;
        this.levelObject = campaignLevel;
        this.campaign = campaign;
        this.gameService = gameService;
        List<Keeper> loadPlayers = players == null ? null : List.copyOf(players);
        gameFactory = factory == null ? () -> new GameLoopManager(level, gameService, loadPlayers) : factory;
        loading = new LoadingScope(task -> stateManager.getApplication().enqueue(task),
                failure -> ((Main) stateManager.getApplication()).handleError("Failed to load the game server", failure));

        // Set multiplayer
        int humanPlayers = 0;
        if (players != null) {
            for (Keeper player : players) {
                if (!player.isAi()) {
                    humanPlayers++;
                    if (humanPlayers > 1) {
                        break;
                    }
                }
            }
        } else {
            humanPlayers = 1;
        }
        multiplayer = (humanPlayers > 1);

        // Add the listener
        gameService.addGameSessionServiceListener(gameSessionListener);

    }

    public boolean isMultiplayer() {
        return multiplayer;
    }

    @Override public void stateAttached(AppStateManager manager) {
        super.stateAttached(manager);
        stateManager = manager;
        if (closing.get()) {
            manager.getApplication().enqueue(() -> manager.detach(this));
            return;
        }
        loading.start("GameDataServerLoader", () -> {
            game = gameFactory.get();
            loading.publish(this::publishGame);
        });
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        if (closing.get()) return;
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;
        startIfReady();
    }

    /**
     * If you are getting rid of the game state, use this so that all the related states are detached on the
     * same render loop. Otherwise the app might crash.
     */
    public void detach() {
        closeServer();
        if (stateManager != null) stateManager.detach(this);
    }

    @Override public void stateDetached(AppStateManager manager) {
        closeServer();
        if (!isInitialized()) cleanup();
    }

    @Override
    public void cleanup() {
        if (cleaned) return;
        cleaned = true;
        closeServer();
        super.cleanup();
    }

    private void publishGame() {
        gameWorldController = game.getGameController().getGameWorldController();
        mapController = gameWorldController.getMapController();
        gameWorldController.addListener(playerActionListener);
        gameWorldController.addSpecialListener(specialListener);
        cheatController = multiplayer ? new NoopCheatController() : new CheatController(kwdFile, game.getGameController(), gameService.getServerEntityData());
        mapController.addListener(mapListener);
        for (IPlayerController playerController : game.getGameController().getPlayerControllers().values()) {
            listeningPlayers.add(playerController);
            playerController.addListener(gameService);
        }
        gameLoaded = true;
        gameService.sendGameData(game.getGameController().getLevelInfo().getPlayers().values());
        startIfReady();
    }

    private void startIfReady() {
        if (isInitialized() && gameLoaded && startRequested.get() && !gameStarted && !closing.get()) {
            gameStarted = true;
            game.start();
        }
    }

    private void withGame(Runnable command) {
        if (closing.get() || !gameLoaded) return;
        var lock = commands.readLock(); lock.lock();
        try { if (!closing.get() && gameLoaded) command.run(); }
        finally { lock.unlock(); }
    }

    private void closeServer() {
        if (!closing.compareAndSet(false, true)) return;
        setEnabled(false);
        try { gameService.removeGameSessionServiceListener(gameSessionListener); }
        catch (RuntimeException | Error failure) { closeFailure = failure; }
        finally {
            loading.close();
            loading.whenStopped().thenRun(() -> Thread.ofVirtual().name("GameServerRetirement").start(this::retireGame));
        }
    }

    private void retireGame() {
        // Wait for admitted commands and loop joins here, never on the render thread.
        var lock = commands.writeLock(); lock.lock();
        Throwable failure = closeFailure;
        try {
            var actions = new ArrayList<Runnable>();
            if (game != null) actions.add(game::stop);
            if (mapController != null) actions.add(() -> mapController.removeListener(mapListener));
            if (gameWorldController != null) {
                actions.add(() -> gameWorldController.removeListener(playerActionListener));
                actions.add(() -> gameWorldController.removeSpecialListener(specialListener));
            }
            for (var player : listeningPlayers) actions.add(() -> player.removeListener(gameService));
            for (Runnable action : actions) {
                try { action.run(); }
                catch (Throwable error) {
                    if (failure == null) failure = error;
                    else if (failure != error) failure.addSuppressed(error);
                }
            }
            listeningPlayers.clear();
        } catch (Throwable error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        } finally { lock.unlock(); }
        if (failure == null) stopped.complete(null);
        else {
            stopped.completeExceptionally(failure);
            logger.log(Level.ERROR, "Failed to retire the game server", failure);
        }
    }

    /** Actual loader, admitted-command, simulation-thread and listener retirement. No GPU completion claim. */
    public CompletionStage<Void> whenStopped() { return stopped.minimalCompletionStage(); }

    /**
     * Listen for basically clients' requests
     */
    private final class GameSessionServiceListenerImpl implements GameSessionServiceListener {

        @Override
        public void onStartGame() {
            startRequested.set(true);
            if (stateManager != null) loading.publish(GameServerState.this::startIfReady);
        }

        @Override
        public void onSelectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
            withGame(() -> mapController.selectTiles(start, end, select, playerId));
        }

        @Override
        public void onBuild(Vector2f start, Vector2f end, short roomId, short playerId) {
            withGame(() -> gameWorldController.build(start, end, playerId, roomId));
        }

        @Override
        public void onSell(Vector2f start, Vector2f end, short playerId) {
            withGame(() -> gameWorldController.sell(start, end, playerId));
        }

        @Override
        public void onInteract(EntityId entity, short playerId) {
            withGame(() -> gameWorldController.interact(entity, playerId));
        }

        @Override
        public void onPickUp(EntityId entity, short playerId) {
            withGame(() -> gameWorldController.pickUp(entity, playerId));
        }

        @Override
        public void onDrop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity, short playerId) {
            withGame(() -> gameWorldController.drop(entity, tile, coordinates, dropOnEntity, playerId));
        }

        @Override
        public void onCastKeeperSpell(short keeperSpellId, EntityId target, Point tile, Vector2f position, short playerId) {
            withGame(() -> gameWorldController.castKeeperSpell(keeperSpellId, target, tile, position, playerId));
        }

        @Override
        public void onPlaceDoor(short doorId, Point tile, short playerId) {
            withGame(() -> gameWorldController.placeDoor(doorId, tile, playerId));
        }

        @Override
        public void onPlaceTrap(short trapId, Point tile, short playerId) {
            withGame(() -> gameWorldController.placeTrap(trapId, tile, playerId));
        }

        @Override
        public void onTransitionEnd(short playerId) {
            // We are not really interested in this, the status is also tracked in the local clients
        }

        @Override
        public void onPauseRequest(short playerId) {
            // TODO: We should only allow the server owner etc. to pause, otherwise, send a system message that player x wants to pause?
            withGame(() -> game.pause());

        }

        @Override
        public void onResumeRequest(short playerId) {
            // TODO: We should only allow the server owner etc. to pause, otherwise, send a system message that player x wants to pause?
            withGame(() -> game.resume());
        }

        @Override
        public void onExitGame(short playerId) {
            // TODO: Close the server and game only when everybody has left
            if (closing.get()) return;
            if (stateManager == null) closeServer();
            else stateManager.getApplication().enqueue(GameServerState.this::detach);
        }

        @Override
        public void onGetGold(int amount, short playerId) {
            withGame(() -> gameWorldController.getGold(amount, playerId));
        }

        @Override
        public void onCancelCallToArms(short playerId) {
            withGame(() -> gameWorldController.getObjectsController().cancelCallToArms(playerId));
        }

        @Override
        public void onCheatTriggered(CheatState.CheatType cheat, short playerId) {
            withGame(() -> cheatController.onCheat(cheat, playerId));
        }

        @Override
        public void onSpawnCreatureCheatTriggered(short creatureId, int level, int amount, short playerId) {
            withGame(() -> cheatController.spawnCreature(creatureId, level, amount, playerId));
        }

        @Override
        public void onAddGoldCheatTriggered(int amount, short playerId) {
            withGame(() -> cheatController.addGold(amount, playerId));
        }

        @Override
        public void onAddManaCheatTriggered(int amount, short playerId) {
            withGame(() -> cheatController.addMana(amount, playerId));
        }

        @Override
        public void onSetPossessedMovement(Vector2f direction, float rotation, byte speedMode, short playerId) {
            withGame(() -> {
                EntityId possessed = gameWorldController.getCreaturesController().getPossessedCreature(playerId);
                if (possessed != null) {
                    gameWorldController.getCreaturesController().createController(possessed).setPossessedMovement(direction, rotation, speedMode);
                }
            });
        }

        @Override
        public void onSetPossessedAttack(boolean attacking, short playerId) {
            withGame(() -> {
                EntityId possessed = gameWorldController.getCreaturesController().getPossessedCreature(playerId);
                if (possessed != null) {
                    gameWorldController.getCreaturesController().createController(possessed).setPossessedAttack(attacking);
                }
            });
        }

        @Override
        public void onEndPossession(short playerId) {
            withGame(() -> {
                EntityId possessed = gameWorldController.getCreaturesController().getPossessedCreature(playerId);
                if (possessed != null) {
                    gameWorldController.getCreaturesController().createController(possessed).setPossession(false);
                }
            });
        }
    }

    /**
     * Listen for the map changes
     */
    private final class MapListenerImpl implements MapListener {

        @Override
        public void onTilesChange(List<Point> updatedTiles) {
            gameService.updateTiles(updatedTiles);
        }

        @Override
        public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
            gameService.flashTiles(points, enabled, keeperId);
        }

        @Override
        public void onTilesReveal(List<Point> points, boolean explore, short keeperId) {
            gameService.revealTiles(points, explore, keeperId);
        }

        @Override
        public void onFogOfWarDisabled(short keeperId) {
            gameService.disableFogOfWar(keeperId);
        }

        @Override
        public void onFogOfWarReset(short keeperId) {
            gameService.resetFogOfWar(keeperId);
        }
    }

    /**
     * Locate Hidden Land discovers the secret level of a campaign level. This
     * is stored right away in the user's campaign progress, as in the original.
     */
    private final class SpecialListenerImpl implements SpecialListener {

        @Override
        public void onSpecialActivated(short playerId, short objectId) {
            if (levelObject == null || objectId != ObjectsController.OBJECT_SPECIAL_LOCATE_HIDDEN_LAND_ID) {
                return;
            }

            Settings settings = Main.getUserSettings();
            if (SecretLevels.discover(levelObject, settings::getSecredLevelStatus, settings::setSecredLevelStatus).isPresent()) {
                try {
                    settings.save();
                } catch (IOException ex) {
                    logger.log(Level.ERROR, "Failed to save the discovered secret level!", ex);
                }
            }
        }

    }

    /**
     * Listen for the player actions
     */
    private final class PlayerActionListenerImpl implements PlayerActionListener {

        @Override
        public void onBuild(short keeperId, List<Point> tiles) {
            gameService.onBuild(keeperId, tiles);
        }

        @Override
        public void onSold(short keeperId, List<Point> tiles) {
            gameService.onSold(keeperId, tiles);
        }

    }

}
