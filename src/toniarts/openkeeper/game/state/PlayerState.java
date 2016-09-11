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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.console.ConsoleState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.player.PlayerCreatureControl;
import toniarts.openkeeper.game.player.PlayerGoldControl;
import toniarts.openkeeper.game.player.PlayerRoomControl;
import toniarts.openkeeper.game.player.PlayerStatsControl;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState;
import toniarts.openkeeper.view.PossessionCameraState;
import toniarts.openkeeper.view.PossessionInteractionState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * The player state! GUI, camera, etc. Player interactions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerState extends AbstractAppState {

    protected Main app;

    protected AssetManager assetManager;
    protected AppStateManager stateManager;

    private short playerId;

    private boolean paused = false;

    private List<AbstractPauseAwareState> appStates = new ArrayList<>();
    private List<AbstractPauseAwareState> storedAppStates;
    protected PlayerInteractionState interactionState;
    private PossessionInteractionState possessionState;
    protected PlayerCameraState cameraState;
    private PossessionCameraState possessionCameraState;

    private boolean transitionEnd = true;
    private PlayerScreenController screen;

    private static final Logger logger = Logger.getLogger(PlayerState.class.getName());

    public PlayerState(int playerId) {
        this.playerId = (short) playerId;
    }

    public PlayerState(int playerId, boolean enabled) {
        this(playerId);
        super.setEnabled(enabled);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        assetManager = this.app.getAssetManager();
        this.stateManager = stateManager;

        screen = new PlayerScreenController(this);
        this.app.getNifty().registerScreenController(screen);
    }

    @Override
    public void cleanup() {

        // Detach states
        for (AbstractAppState state : appStates) {
            stateManager.detach(state);
        }

        super.cleanup();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!isInitialized()) {
            return;
        }

        if (enabled) {

            // Get the game state
            final GameState gameState = stateManager.getState(GameState.class);
            screen.initHud(gameState.getLevelData().getGameLevel().getTextTableId().getLevelDictFile());

            // Cursor
            app.getInputManager().setCursorVisible(true);

            // Set the pause state
            paused = false;
            screen.setPause(paused);

            stateManager.attach(new ConsoleState());
            // Create app states
            Player player = gameState.getLevelData().getPlayer(playerId);

            possessionState = new PossessionInteractionState(true) {

                @Override
                protected void onExit() {
                    super.onExit();
                    // Detach states
                    for (AbstractAppState state : storedAppStates) {
                        stateManager.detach(state);
                    }

                    // Load the state
                    for (AbstractAppState state : appStates) {
                        state.setEnabled(true);
                    }

                    screen.goToScreen(PlayerScreenController.HUD_SCREEN_ID);
                }

                @Override
                protected void onActionChange(PossessionInteractionState.Action action) {
                    PlayerState.this.screen.updatePossessionSelectedItem(action);
                }
            };

            cameraState = new PlayerCameraState(player);

            interactionState = new PlayerInteractionState(player, gameState, screen.getGuiConstraint(), screen.getTooltip()) {
                @Override
                protected void onInteractionStateChange(InteractionState interactionState) {
                    PlayerState.this.screen.updateSelectedItem(interactionState);
                }

                @Override
                protected void onPossession(Thing.KeeperCreature creature) {
                    // Detach states
                    for (AbstractAppState state : appStates) {
                        state.setEnabled(false);
                    }
                    storedAppStates = new ArrayList<>();
                    storedAppStates.add(possessionState);
                    // TODO not Thing.KeeperCreature need wrapper around KeeperCreature
                    possessionState.setTarget(creature);
                    possessionCameraState = new PossessionCameraState(creature, gameState.getLevelData());
                    storedAppStates.add(possessionCameraState);
                    // Load the state
                    for (AbstractAppState state : storedAppStates) {
                        stateManager.attach(state);
                    }
                    screen.goToScreen(PlayerScreenController.POSSESSION_SCREEN_ID);
                }
            };
            appStates.add(interactionState);
            appStates.add(cameraState);
            // FIXME add to appStates or directly to stateManager
            appStates.add(new SystemMessageState(screen.getMessages()));
            // Load the state
            for (AbstractAppState state : appStates) {
                stateManager.attach(state);
            }
        } else {

            // Detach states
            for (AbstractAppState state : appStates) {
                stateManager.detach(state);
            }
            stateManager.detach(stateManager.getState(ConsoleState.class));

            appStates.clear();
            screen.goToScreen(PlayerScreenController.SCREEN_EMPTY_ID);
        }
    }

    public PlayerScreenController getScreen() {
        return screen;
    }

    /**
     * Get this player
     *
     * @return this player
     */
    public Keeper getPlayer() {
        GameState gs = stateManager.getState(GameState.class);
        if (gs != null) {
            return gs.getPlayer(playerId);
        }
        return null;
    }

    public PlayerStatsControl getStatsControl() {
        Keeper keeper = getPlayer();
        if (keeper != null) {
            return keeper.getStatsControl();
        }
        return null;
    }

    public PlayerGoldControl getGoldControl() {
        Keeper keeper = getPlayer();
        if (keeper != null) {
            return keeper.getGoldControl();
        }
        return null;
    }

    public PlayerCreatureControl getCreatureControl() {
        Keeper keeper = getPlayer();
        if (keeper != null) {
            return keeper.getCreatureControl();
        }
        return null;
    }

    public PlayerRoomControl getRoomControl() {
        Keeper keeper = getPlayer();
        if (keeper != null) {
            return keeper.getRoomControl();
        }
        return null;
    }

    public void setTransitionEnd(boolean value) {
        transitionEnd = value;
    }

    public boolean isTransitionEnd() {
        return transitionEnd;
    }

    public void setWideScreen(boolean enable) {
        if (interactionState.isInitialized()) {
            interactionState.setEnabled(!enable);
        }

        if (enable) {
            screen.goToScreen(PlayerScreenController.CINEMATIC_SCREEN_ID);
        } else {
            screen.goToScreen(PlayerScreenController.HUD_SCREEN_ID);
        }
    }

    public void setText(int textId, boolean introduction, int pathId) {
        screen.setCinematicText(textId, introduction, pathId);
    }

    public void flashButton(int id, TriggerAction.MakeType type, boolean enabled, int time) {
        // TODO make flash button
    }

    protected List<Room> getAvailableRoomsToBuild() {
        return getRoomControl().getTypesAvailable();
    }

    protected List<KeeperSpell> getAvailableKeeperSpells() {
        GameState gameState = stateManager.getState(GameState.class);
        List<KeeperSpell> spells = gameState.getLevelData().getKeeperSpells();
        return spells;
    }

    protected List<Door> getAvailableDoors() {
        GameState gameState = stateManager.getState(GameState.class);
        List<Door> doors = gameState.getLevelData().getDoors();
        return doors;
    }

    protected List<Trap> getAvailableTraps() {
        GameState gameState = stateManager.getState(GameState.class);
        List<Trap> traps = new ArrayList<>();
        for (Trap trap : gameState.getLevelData().getTraps()) {
            if (trap.getGuiIcon() == null) {
                continue;
            }
            traps.add(trap);
        }
        return traps;
    }

    public void setPaused(boolean paused) {
        // Pause state
        this.paused = paused;

        // Pause / unpause
        stateManager.getState(GameState.class).setEnabled(!paused);

        for (AbstractPauseAwareState state : appStates) {
            if (state.isPauseable()) {
                state.setEnabled(!paused);
            }
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void quitToMainMenu() {

        // Disable us, detach game and enable start
        stateManager.getState(GameState.class).detach();
        setEnabled(false);
        stateManager.getState(MainMenuState.class).setEnabled(true);
    }

    public void quitToOS() {
        app.stop();
    }

    public void zoomToCreature(String creatureId) {
        GameState gs = stateManager.getState(GameState.class);
        CreatureControl creature = getCreatureControl().getNextCreature(gs.getLevelData().getCreature(Short.parseShort(creatureId)));
        cameraState.setCameraLookAt(creature.getSpatial());
    }

    public short getPlayerId() {
        return playerId;
    }

    protected Creature getPossessionCreature() {
        return possessionState.getTargetCreature();
    }
    
    protected InteractionState getInteractionState() {
        return interactionState.getInteractionState();
    }
}
