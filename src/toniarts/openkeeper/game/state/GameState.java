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

import com.badlogic.gdx.ai.GdxAI;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.GameTimer;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.party.PartytState;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.world.WorldState;

/**
 * The GAME state!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameState extends AbstractPauseAwareState {

    private Main app;

    private AppStateManager stateManager;

    private String level;
    private KwdFile kwdFile;

    private TriggerControl triggerControl = null;
    private final Map<Short, Integer> flags = new HashMap<>(127);
    // TODO What timer class we should take ?
    private final Map<Byte, GameTimer> timers = new HashMap<>(15);

    private float gameTime = 0;
    private Float timeLimit = null;
    private TaskManager taskManager;
    private final Map<Short, Keeper> players = new HashMap<>();
    private static final Logger logger = Logger.getLogger(GameState.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     */
    public GameState(String level) {
        this.level = level;
    }

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players player particicipating in this game
     */
    public GameState(KwdFile level, List<Keeper> players) {
        this.kwdFile = level;
        for (Keeper keeper : players) {
            this.players.put(keeper.getId(), keeper);
        }
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Set up the loading screen
        SingleBarLoadingState loader = new SingleBarLoadingState() {
            @Override
            public Void onLoad() {

                try {

                    // Load the level data
                    if (level != null) {
                        kwdFile = new KwdFile(Main.getDkIIFolder(),
                                new File(ConversionUtils.getRealFileName(Main.getDkIIFolder(), AssetsConverter.MAPS_FOLDER + level + ".kwd")));
                    } else {
                        kwdFile.load();
                    }
                    setProgress(0.1f);

                    // Setup players
                    if (players.isEmpty()) {
                        for (Entry<Short, Player> entry : kwdFile.getPlayers().entrySet()) {
                            players.put(entry.getKey(), new Keeper(entry.getValue()));
                        }
                    }

                    // Create the actual level
                    WorldState worldState = new WorldState(kwdFile, assetManager, GameState.this) {
                        @Override
                        protected void updateProgress(int progress, int max) {
                            setProgress(0.1f + ((float) progress / max * 0.5f));
                        }
                    };

                    // Initialize tasks
                    // FIXME: for all players managed by this computer
                    taskManager = new TaskManager(worldState, (short) 3);

                    GameState.this.stateManager.attach(worldState);

                    GameState.this.stateManager.attach(new SoundState(false));
                    setProgress(0.60f);

                    GameState.this.stateManager.attach(new ActionPointState(false));
                    setProgress(0.70f);

                    GameState.this.stateManager.attach(new PartytState(false));
                    setProgress(0.80f);

                    // Trigger data
                    for (short i = 0; i < 128; i++) {
                        flags.put(i, 0);
                    }

                    for (byte i = 0; i < 16; i++) {
                        timers.put(i, new GameTimer());
                    }

                    int triggerId = kwdFile.getGameLevel().getTriggerId();
                    if (triggerId != 0) {
                        triggerControl = new TriggerControl(stateManager, triggerId);
                        setProgress(0.90f);
                    }

                    setProgress(1.0f);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load the game!", e);
                }

                return null;
            }

            @Override
            public void onLoadComplete() {

                // Enable player state
                GameState.this.stateManager.getState(PlayerState.class).setEnabled(true);
                GameState.this.stateManager.getState(ActionPointState.class).setEnabled(true);
                GameState.this.stateManager.getState(PartytState.class).setEnabled(true);
                GameState.this.stateManager.getState(SoundState.class).setEnabled(true);

                // Set initialized
                GameState.this.initialized = true;

                // Set the processors
                GameState.this.app.setViewProcessors();
            }
        };
        stateManager.attach(loader);
    }

    @Override
    public void cleanup() {

        // Detach
        stateManager.detach(stateManager.getState(ActionPointState.class));
        stateManager.detach(stateManager.getState(PartytState.class));
        stateManager.detach(stateManager.getState(WorldState.class));
        stateManager.detach(stateManager.getState(SoundState.class));

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }

        // Update time for AI
        GdxAI.getTimepiece().update(tpf);
        gameTime += tpf;

        if (timeLimit != null && timeLimit > 0) {
            timeLimit -= tpf;
        }

        for (GameTimer timer : timers.values()) {
            timer.update(tpf);
        }

        if (triggerControl != null) {
            triggerControl.update(tpf);
        }

        super.update(tpf);
    }

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    public KwdFile getLevelData() {
        return kwdFile;
    }

    public int getFlag(int id) {
        return flags.get((short) id);
    }

    public void setFlag(int id, int value) {
        flags.put((short) id, value);
    }

    public GameTimer getTimer(int id) {
        return timers.get((byte) id);
    }

    public float getGameTime() {
        return gameTime;
    }

    public String getLevel() {
        return level;
    }

    public Float getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(float timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setEnd(boolean win) {
        // TODO make lose and win the game
        stateManager.getState(MainMenuState.class).setEnabled(true);
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public Keeper getPlayer(short playerId) {
        return players.get(playerId);
    }

    public Collection<Keeper> getPlayers() {
        return players.values();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    public ActionPointState getActionPointState() {
        return stateManager.getState(ActionPointState.class);
    }
}
