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
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.CheatController;
import toniarts.openkeeper.game.controller.ICheatController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.NoopCheatController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.state.loop.GameLoopManager;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;
import toniarts.openkeeper.game.state.session.SaveGame;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The game state that actually runs the game. Has no relation to visuals.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameServerState extends AbstractAppState {

    private static final Logger logger = System.getLogger(GameServerState.class.getName());

    private Main app;

    private Thread loader;
    private AppStateManager stateManager;

    private final Object loadingObject = new Object();
    private volatile boolean gameLoaded = false;

    private final IKwdFile kwdFile;
    private final CampaignLevel levelObject;
    private final SaveGame saveGame;

    private final boolean campaign;
    private final boolean multiplayer;
    private final GameSessionServerService gameService;
    private IMapController mapController;
    private final MapListener mapListener = new MapListenerImpl();
    private final GameSessionServiceListener gameSessionListener = new GameSessionServiceListenerImpl();
    private final PlayerActionListener playerActionListener = new PlayerActionListenerImpl();
    private GameLoopManager game;
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

    /**
     * Loads a saved game
     *
     * @param level the level of the saved game
     * @param saveGame the saved game
     * @param gameService the game service, its server entities are the saved
     * ones
     */
    public GameServerState(IKwdFile level, SaveGame saveGame, GameSessionServerService gameService) {
        this(level, saveGame.getPlayers(), false, gameService, null, saveGame);
    }

    private GameServerState(IKwdFile level, List<Keeper> players, boolean campaign, GameSessionServerService gameService,
            CampaignLevel campaignLevel, SaveGame saveGame) {
        this.kwdFile = level;
        this.levelObject = campaignLevel;
        this.saveGame = saveGame;
        this.campaign = campaign;
        this.gameService = gameService;

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

        // Start loading game
        loadGame(players);
    }

    public boolean isMultiplayer() {
        return multiplayer;
    }

    /**
     * Saves the game, see {@link GameLoopManager#save(Path)}
     *
     * @param file the save file
     * @return completes when the file is written, or right away with an
     * exception if the game hasn't loaded yet
     */
    public CompletableFuture<Void> save(Path file) {
        if (!gameLoaded) {
            return CompletableFuture.failedFuture(new IllegalStateException("The game hasn't loaded yet!"));
        }
        return game.save(file);
    }

    private void loadGame(List<Keeper> players) {
        loader = new GameLoader(players);
        loader.start();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        this.app = (Main) app;
        this.stateManager = stateManager;
    }

    /**
     * If you are getting rid of the game state, use this so that all the related states are detached on the
     * same render loop. Otherwise the app might crash.
     */
    public void detach() {
        if (loader != null && loader.isAlive()) {
            loader.interrupt();
        }
        stateManager.detach(this);

        if (game != null) {
            try {
                game.stop();
            } catch (Exception ex) {
                logger.log(Level.ERROR, "Failed to close the game!", ex);
            }
        }
    }

    @Override
    public void cleanup() {

        // Detach
        detach();

        super.cleanup();
    }

    /**
     * Load the game
     */
    private final class GameLoader extends Thread {

        private final List<Keeper> players;

        public GameLoader(List<Keeper> players) {
            super("GameDataServerLoader");

            this.players = players;
        }

        @Override
        public void run() {
            // Create the central game controller
            game = saveGame != null ? new GameLoopManager(kwdFile, gameService, saveGame) : new GameLoopManager(kwdFile, gameService, players);

            gameWorldController = game.getGameController().getGameWorldController();
            mapController = gameWorldController.getMapController();
            gameWorldController.addListener(playerActionListener);
            cheatController = multiplayer ? new NoopCheatController() : new CheatController(kwdFile, game.getGameController());

            // Send the the initial game data
            gameService.sendGameData(game.getGameController().getLevelInfo().getPlayers().values());

            // Set up a listener for the map
            mapController.addListener(mapListener);

            // Set up a listener for the player changes, they are per player
            for (IPlayerController playerController : game.getGameController().getPlayerControllers().values()) {
                playerController.addListener(gameService);
            }

            // Nullify the thread object
            loader = null;

            // Mark the server end ready
            synchronized (loadingObject) {
                gameLoaded = true;
                loadingObject.notifyAll();
            }
        }
    }

    /**
     * Listen for basically clients' requests
     */
    private final class GameSessionServiceListenerImpl implements GameSessionServiceListener {

        // The actions are queued for the next tick, and the client may reuse the points it passed in (the selection
        // area does), so the queued action gets its own copies

        private static Vector2f copy(Vector2f vector) {
            return vector != null ? vector.clone() : null;
        }

        private static Point copy(Point point) {
            return point != null ? new Point(point.x, point.y) : null;
        }

        @Override
        public void onStartGame() {

            // Just make sure that the server has fully set up itself before we start the game
            if (!gameLoaded) {
                synchronized (loadingObject) {
                    if (!gameLoaded) {
                        try {
                            loadingObject.wait();
                        } catch (InterruptedException ex) {
                            logger.log(Level.ERROR, "Failed to load the game.", ex);
                        }
                    }
                }
            }

            // Start the actual game
            game.start();
        }

        @Override
        public void onSelectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
            Vector2f from = start.clone();
            Vector2f to = end.clone();
            game.runPlayerAction(() -> mapController.selectTiles(from, to, select, playerId));
        }

        @Override
        public void onBuild(Vector2f start, Vector2f end, short roomId, short playerId) {
            Vector2f from = start.clone();
            Vector2f to = end.clone();
            game.runPlayerAction(() -> gameWorldController.build(from, to, playerId, roomId));
        }

        @Override
        public void onSell(Vector2f start, Vector2f end, short playerId) {
            Vector2f from = start.clone();
            Vector2f to = end.clone();
            game.runPlayerAction(() -> gameWorldController.sell(from, to, playerId));
        }

        @Override
        public void onInteract(EntityId entity, short playerId) {
            game.runPlayerAction(() -> gameWorldController.interact(entity, playerId));
        }

        @Override
        public void onPickUp(EntityId entity, short playerId) {
            game.runPlayerAction(() -> gameWorldController.pickUp(entity, playerId));
        }

        @Override
        public void onDrop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity, short playerId) {
            Point dropTile = copy(tile);
            Vector2f dropCoordinates = copy(coordinates);
            game.runPlayerAction(() -> gameWorldController.drop(entity, dropTile, dropCoordinates, dropOnEntity, playerId));
        }

        @Override
        public void onCastKeeperSpell(short keeperSpellId, EntityId target, Point tile, Vector2f position, short playerId) {
            Point castTile = copy(tile);
            Vector2f castPosition = copy(position);
            game.runPlayerAction(() -> gameWorldController.castKeeperSpell(keeperSpellId, target, castTile, castPosition, playerId));
        }

        @Override
        public void onPlaceDoor(short doorId, Point tile, short playerId) {
            Point doorTile = copy(tile);
            game.runPlayerAction(() -> gameWorldController.placeDoor(doorId, doorTile, playerId));
        }

        @Override
        public void onPlaceTrap(short trapId, Point tile, short playerId) {
            Point trapTile = copy(tile);
            game.runPlayerAction(() -> gameWorldController.placeTrap(trapId, trapTile, playerId));
        }

        @Override
        public void onTransitionEnd(short playerId) {
            // We are not really interested in this, the status is also tracked in the local clients
        }

        @Override
        public void onPauseRequest(short playerId) {
            // TODO: We should only allow the server owner etc. to pause, otherwise, send a system message that player x wants to pause?
            game.pause();

        }

        @Override
        public void onResumeRequest(short playerId) {
            // TODO: We should only allow the server owner etc. to pause, otherwise, send a system message that player x wants to pause?
            game.resume();
        }

        @Override
        public void onExitGame(short playerId) {
            // TODO: Close the server and game only when everybody has left
            stateManager.detach(GameServerState.this);
        }

        @Override
        public void onGetGold(int amount, short playerId) {
            game.runPlayerAction(() -> gameWorldController.getGold(amount, playerId));
        }

        @Override
        public void onCheatTriggered(CheatState.CheatType cheat, short playerId) {
            game.runPlayerAction(() -> cheatController.onCheat(cheat, playerId));
        }

        @Override
        public void onSpawnCreatureCheatTriggered(short creatureId, int level, int amount, short playerId) {
            game.runPlayerAction(() -> cheatController.spawnCreature(creatureId, level, amount, playerId));
        }

        @Override
        public void onAddGoldCheatTriggered(int amount, short playerId) {
            cheatController.addGold(amount, playerId);
        }

        @Override
        public void onAddManaCheatTriggered(int amount, short playerId) {
            cheatController.addMana(amount, playerId);
        }

        @Override
        public void onSetPossessedMovement(Vector2f direction, float rotation, byte speedMode, short playerId) {
            EntityId possessed = gameWorldController.getCreaturesController().getPossessedCreature(playerId);
            if (possessed != null) {
                gameWorldController.getCreaturesController().createController(possessed).setPossessedMovement(direction, rotation, speedMode);
            }
        }

        @Override
        public void onEndPossession(short playerId) {
            game.runPlayerAction(() -> {
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
