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
package toniarts.openkeeper.game;

import de.lessvoid.nifty.controls.Label;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldHandler;

/**
 * Ingame player information
 * TODO: use level Variables to get mana lose and gain information
 * @author ArchDemon
 */
public class PlayerManaControl {

    public enum Type {
        CURRENT, GET, LOSE;
    }

    private short playerId;
    private WorldHandler worldHandler;
    private Map<Type, Label> listeners = new HashMap();

    private int manaCurrent;
    private int manaMax;
    private int manaGet;  // mana get per second
    private int manaGetBase = 30;  // I think this dungeon heart
    private int manaGetFromTiles;

    private int manaLose;  // mana lose per second
    private int manaLosePerImp = 7;  // I don`t find in Creature.java
    private int manaLoseFromCreatures;

    public PlayerManaControl(short playerId, WorldHandler worldHandler) {
        this.playerId = playerId;
        this.worldHandler = worldHandler;

        this.manaGetFromTiles = 0;
        this.updateManaFromTiles();
        this.manaLoseFromCreatures = 0;
        this.updateManaFromCreatures();
        this.manaMax = 200000;
    }

    public final void updateManaFromTiles() {
        int result = 0;

        for (int x = 0; x < worldHandler.getLevelData().getWidth(); x++) {
            for (int y = 0; y < worldHandler.getLevelData().getHeight(); y++) {
                TileData tile = worldHandler.getMapLoader().getTile(x, y);
                if (tile.getPlayerId() == this.playerId) {
                    result += worldHandler.getLevelData().getTerrain(tile.getTerrainId()).getManaGain();
                }
            }
        }

        this.manaGetFromTiles = result;
    }

    public final void updateManaFromCreatures() {
        int result = 0;

        for (Thing thing : worldHandler.getLevelData().getThings()) {
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

    public void updateManaGet() {
        manaGet = manaGetBase + manaGetFromTiles;
        if (listeners.containsKey(Type.GET)) {
            listeners.get(Type.GET).setText(String.format("+ %s", manaGet));
        }
    }

    public void updateManaLose() {
        manaLose = manaLoseFromCreatures;
        if (listeners.containsKey(Type.LOSE)) {
            listeners.get(Type.LOSE).setText(String.format("- %s", manaLose));
        }
    }

    public void update() {
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
        if (!listeners.containsKey(type)) {
            listeners.put(type, label);
        }
    }

    public void removeListeners() {
        listeners.clear();
    }
}