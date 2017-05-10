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
package toniarts.openkeeper.game.player;

import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.controls.Label;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.world.MapData;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;

/**
 * Controller of player mana
 *
 * @author ArchDemon
 */
public class PlayerManaControl extends Control {

    public enum Type {

        CURRENT, GAIN, LOSE;
    }
    private float tick = 0;
    private final short playerId;
    private final AppStateManager stateManager;
    private final Map<Type, Label> listeners = new HashMap<>();
    private int manaCurrent;
    private final int manaMax;
    private int manaGain;  // mana get per second
    private final int manaGainBase;
    private int manaGainFromTiles = 0;
    private int manaLose;  // mana lose per second
    private final static int MANA_LOSE_PER_IMP = 7;  // I don`t find in Creature.java
    private int manaLoseFromCreatures = 0;

    public PlayerManaControl(short playerId, AppStateManager stateManager) {
        this.playerId = playerId;
        this.stateManager = stateManager;

        GameState gs = this.stateManager.getState(GameState.class);

        manaMax = (int) gs.getLevelVariable(MiscType.MAXIMUM_MANA_THRESHOLD);
        manaGainBase = (int) gs.getLevelVariable(MiscType.DUNGEON_HEART_MANA_GENERATION_INCREASE_PER_SECOND);
        // FIXME where mana lose per imp ???
        //manaLosePerImp = (int) gs.getLevelVariable(MiscType.GAME_TICKS);
    }

    @Override
    protected void updateControl(float tpf) {
        tick += tpf;
        if (tick >= 1) {
            // TODO change update mana only from external event
            updateManaFromTiles();
            updateManaFromCreatures();
            update();
            tick -= 1;
        }
    }

    private void updateManaFromTiles() {
        int result = 0;

        MapData mapData = stateManager.getState(WorldState.class).getMapData();

        for (int x = 0; x < mapData.getWidth(); x++) {
            for (int y = 0; y < mapData.getHeight(); y++) {
                TileData tile = mapData.getTile(x, y);
                if (tile.getPlayerId() == this.playerId) {
                    result += tile.getTerrain().getManaGain();
                }
            }
        }

        manaGainFromTiles = result;
    }

    private void updateManaFromCreatures() {
        GameState gs = stateManager.getState(GameState.class);
        int result = gs.getPlayer(playerId).getCreatureControl().getImpCount();

        this.manaLoseFromCreatures = result * MANA_LOSE_PER_IMP;
    }

    private void update() {
        manaGain = manaGainBase + manaGainFromTiles;
        manaLose = manaLoseFromCreatures;

        addMana(this.manaGain - this.manaLose);

        updateListerners();
    }

    public void addMana(int value) {
        value = Math.max(0, this.manaCurrent + value);
        this.manaCurrent = Math.min(value, this.manaMax);
    }

    public int getMana() {
        return this.manaCurrent;
    }

    public int getManaMax() {
        return this.manaMax;
    }

    public int getManaGain() {
        return this.manaGain;
    }

    public int getManaLose() {
        return this.manaLose;
    }

    private void updateListerners() {
        stateManager.getApplication().enqueue(() -> {

            for (Map.Entry<Type, Label> entry : listeners.entrySet()) {
                String text = "";
                Label label = entry.getValue();
                switch (entry.getKey()) {
                    case CURRENT:
                        text = String.format("%s", manaCurrent);
                        break;
                    case GAIN:
                        text = String.format("%s", manaGain);
                        break;
                    case LOSE:
                        text = String.format("- %s", manaLose);
                        break;
                }
                label.setText(text);
            }

            return null;
        });
    }

    public void addListener(Label label, Type type) {
        if (!listeners.containsKey(type) && label != null) {
            listeners.put(type, label);
        }
    }

    public void removeListeners() {
        listeners.clear();
    }
}
