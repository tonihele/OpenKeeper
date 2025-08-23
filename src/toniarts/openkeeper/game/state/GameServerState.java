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
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.state.loop.GameLoopManager;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Utils;

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
    private final toniarts.openkeeper.game.data.Level levelObject;

    private final boolean campaign;
    private final boolean multiplayer;
    private final GameSessionServerService gameService;
    private IMapController mapController;
    private final MapListener mapListener = new MapListenerImpl();
    private final GameSessionServiceListener gameSessionListener = new GameSessionServiceListenerImpl();
    private final PlayerActionListener playerActionListener = new PlayerActionListenerImpl();
    private GameLoopManager game;
    private IGameWorldController gameWorldController;

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players players participating in this game
     * @param campaign whether this is a campaign level or not
     * @param gameService the game service
     */
    public GameServerState(IKwdFile level, List<Keeper> players, boolean campaign, GameSessionServerService gameService) {
        this.kwdFile = level;
        this.levelObject = null;
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
            game = new GameLoopManager(kwdFile, gameService, players);

            gameWorldController = game.getGameController().getGameWorldController();
            mapController = gameWorldController.getMapController();
            gameWorldController.addListener(playerActionListener);

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
            mapController.selectTiles(start, end, select, playerId);
        }

        @Override
        public void onBuild(Vector2f start, Vector2f end, short roomId, short playerId) {
            gameWorldController.build(start, end, playerId, roomId);
        }

        @Override
        public void onSell(Vector2f start, Vector2f end, short playerId) {
            gameWorldController.sell(start, end, playerId);
        }

        @Override
        public void onInteract(EntityId entity, short playerId) {
            gameWorldController.interact(entity, playerId);
        }

        @Override
        public void onPickUp(EntityId entity, short playerId) {
            gameWorldController.pickUp(entity, playerId);
        }

        @Override
        public void onDrop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity, short playerId) {
            gameWorldController.drop(entity, tile, coordinates, dropOnEntity, playerId);
        }

        @Override
        public void onCastKeeperSpell(short keeperSpellId, EntityId target, Point tile, Vector2f position, short playerId) {
            gameWorldController.castKeeperSpell(keeperSpellId, target, tile, position, playerId);
        }

        @Override
        public void onPlaceDoor(short doorId, Point tile, short playerId) {
            gameWorldController.placeDoor(doorId, tile, playerId);
        }

        @Override
        public void onPlaceTrap(short trapId, Point tile, short playerId) {
            gameWorldController.placeTrap(trapId, tile, playerId);
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
            gameWorldController.getGold(amount, playerId);
        }

        @Override
        public void onCheatTriggered(CheatState.CheatType cheat, short playerId) {
            if (isMultiplayer()) {
                return; // No! Bad!
            }

            // See the cheat
            switch (cheat) {
                case LEVEL_MAX: {
                    gameWorldController.getCreaturesController().levelUpCreatures(playerId, Utils.MAX_CREATURE_LEVEL);
                    break;
                }
                case MANA: {
                    game.getGameController().getPlayerController(playerId).getManaControl().addMana(100000);
                    break;
                }
                case MONEY: {
                    gameWorldController.addGold(playerId, 100000);
                    break;
                }
                case REMOVE_FOW: {
                    // TODO:
                    break;
                }
                case UNLOCK_ROOMS: {
                    PlayerRoomControl playerRoomControl = game.getGameController()
                            .getPlayerController(playerId).getRoomControl();
                    for (Room room : kwdFile.getRooms()) {
                        playerRoomControl.setTypeAvailable(room, true);
                    }
                    break;
                }
                case UNLOCK_DOORS_TRAPS: {
                    PlayerDoorControl playerDoorControl = game.getGameController()
                            .getPlayerController(playerId).getDoorControl();
                    for (Door door : kwdFile.getDoors()) {
                        playerDoorControl.setTypeAvailable(door, true);
                    }

                    PlayerTrapControl playerTrapControl = game.getGameController()
                            .getPlayerController(playerId).getTrapControl();
                    for (Trap trap : kwdFile.getTraps()) {
                        playerTrapControl.setTypeAvailable(trap, true);
                    }
                    break;
                }
                case UNLOCK_SPELLS: {
                    PlayerSpellControl playerSpellControl = game.getGameController()
                            .getPlayerController(playerId).getSpellControl();
                    for (KeeperSpell keeperSpell : kwdFile.getKeeperSpells()) {
                        playerSpellControl.setTypeAvailable(keeperSpell, true);
                        playerSpellControl.setSpellDiscovered(keeperSpell, true);
                    }
                    break;
                }
                case WIN_LEVEL: {
                    game.getGameController().endGame(playerId, true);
                    break;
                }
                default:
                    logger.log(Level.INFO, "Cheat {0} not implemented!", cheat);
            }
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
