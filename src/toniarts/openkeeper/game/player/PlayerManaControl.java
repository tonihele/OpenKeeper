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
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import de.lessvoid.nifty.controls.Label;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapData;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;

/**
 * Ingame player information
 * TODO: use level Variables to get mana lose and gain information
 * @author ArchDemon
 */
public class PlayerManaControl extends AbstractControl {

    public enum Type {
        CURRENT, GET, LOSE;
    }
    private float tick = 0;
    private short playerId;
    private AppStateManager stateManager;
    private Map<Type, Label> listeners = new HashMap();

    private int manaCurrent;
    private int manaMax = 200000;
    private int manaGet;  // mana get per second
    private int manaGetBase = 30;  // I think this dungeon heart
    private int manaGetFromTiles = 0;

    private int manaLose;  // mana lose per second
    private int manaLosePerImp = 7;  // I don`t find in Creature.java
    private int manaLoseFromCreatures = 0;

    public PlayerManaControl(short playerId, AppStateManager stateManager) {
        this.playerId = playerId;
        this.stateManager = stateManager;

        //this.updateManaFromTiles();
        //this.updateManaFromCreatures();
    }

    @Override
    protected void controlUpdate(float tpf) {
        tick += tpf;
        if (tick >= 1) {
            updateManaFromTiles();
            updateManaFromCreatures();
            update();
            tick -= 1;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        /*
         if (listeners.containsKey(Type.CURRENT)) {
         listeners.get(Type.CURRENT).setText(String.format("%s", manaCurrent));
         }
         if (listeners.containsKey(Type.GET)) {
         listeners.get(Type.GET).setText(String.format("+ %s", manaGet));
         }
         if (listeners.containsKey(Type.LOSE)) {
         listeners.get(Type.LOSE).setText(String.format("- %s", manaLose));

         }
         */
    }

    private void updateManaFromTiles() {
        int result = 0;

        MapData mapData = stateManager.getState(WorldState.class).getMapData();
        KwdFile kwdFile = stateManager.getState(GameState.class).getLevelData();

        for (int x = 0; x < mapData.getWidth(); x++) {
            for (int y = 0; y < mapData.getHeight(); y++) {
                TileData tile = mapData.getTile(x, y);
                if (tile.getPlayerId() == this.playerId) {
                    result += kwdFile.getTerrain(tile.getTerrainId()).getManaGain();
                }
            }
        }

        this.manaGetFromTiles = result;
    }

    private void updateManaFromCreatures() {
        int result = 0;

        for (Thing thing : stateManager.getState(GameState.class).getLevelData().getThings()) {
            if (!(thing instanceof Thing.KeeperCreature)) {
                continue;
            }

            Thing.KeeperCreature creature = ((Thing.KeeperCreature)thing);
            if (creature.getPlayerId() == this.playerId && creature.getCreatureId() == 1) {
                result++;
            }
        }

        this.manaLoseFromCreatures = result * this.manaLosePerImp;
    }

    private void updateManaGet() {
        manaGet = manaGetBase + manaGetFromTiles;
        if (listeners.containsKey(Type.GET)) {
            listeners.get(Type.GET).setText(String.format("+ %s", manaGet));
        }
    }

    private void updateManaLose() {
        manaLose = manaLoseFromCreatures;
        if (listeners.containsKey(Type.LOSE)) {
            listeners.get(Type.LOSE).setText(String.format("- %s", manaLose));
        }
    }

    private void update() {
        this.updateManaGet();
        this.updateManaLose();

        this.manaCurrent += this.manaGet - this.manaLose;

        if (this.manaCurrent > this.manaMax) {
            this.manaCurrent = this.manaMax;
        }

        if (listeners.containsKey(Type.CURRENT)) {
            listeners.get(Type.CURRENT).setText(String.format("%s", manaCurrent));
        }
    }

    public int getMana() {
        return this.manaCurrent;
    }

    public int getManaGain() {
        return this.manaGet;
    }

    public int getManaLose() {
        return this.manaLose;
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