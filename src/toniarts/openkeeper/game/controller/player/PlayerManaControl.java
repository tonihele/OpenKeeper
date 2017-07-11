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
package toniarts.openkeeper.game.controller.player;

import com.jme3.util.SafeArrayList;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerManaListener;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * Controller of player mana
 *
 * @author ArchDemon
 */
public class PlayerManaControl extends Control {

    private final Keeper keeper;
    private final IMapController mapController;
    private final PlayerCreatureControl playerCreatureControl;
    private final List<PlayerManaListener> listeners = new SafeArrayList<>(PlayerManaListener.class);
    private final int manaGainBase;
    private int manaGainFromTiles = 0;
    private final static int MANA_LOSE_PER_IMP = 7;  // I don't find in Creature.java
    private int manaLoseFromCreatures = 0;

    public PlayerManaControl(Keeper keeper, IMapController mapController, PlayerCreatureControl playerCreatureControl,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.keeper = keeper;
        this.mapController = mapController;
        this.playerCreatureControl = playerCreatureControl;

        this.keeper.setMaxMana((int) gameSettings.get(MiscType.MAXIMUM_MANA_THRESHOLD).getValue());
        manaGainBase = (int) gameSettings.get(MiscType.DUNGEON_HEART_MANA_GENERATION_INCREASE_PER_SECOND).getValue();
        // FIXME where mana lose per imp ???
        //manaLosePerImp = (int) gs.getLevelVariable(MiscType.GAME_TICKS);
    }

    @Override
    protected void updateControl(float tpf) {

        // TODO change update mana only from external event
        updateManaFromTiles();
        updateManaFromCreatures();
        update();
    }

    private void updateManaFromTiles() {
        int result = 0;

        MapData mapData = mapController.getMapData();

        for (int x = 0; x < mapData.getWidth(); x++) {
            for (int y = 0; y < mapData.getHeight(); y++) {
                MapTile tile = mapData.getTile(x, y);
                if (tile.getOwnerId() == keeper.getId()) {
                    result += tile.getManaGain();
                }
            }
        }

        manaGainFromTiles = result;
    }

    private void updateManaFromCreatures() {
        this.manaLoseFromCreatures = playerCreatureControl.getImpCount() * MANA_LOSE_PER_IMP;
    }

    private void update() {

        keeper.setManaGain(manaGainBase + manaGainFromTiles);
        keeper.setManaLoose(manaLoseFromCreatures);

        addMana(keeper.getManaGain() - keeper.getManaLoose());

        updateListerners();
    }

    public void addMana(int value) {
        value = Math.max(0, keeper.getMana() + value);
        keeper.setMana(Math.min(value, keeper.getMaxMana()));
    }

    private void updateListerners() {
        for (PlayerManaListener listener : listeners) {
            listener.onManaChange(keeper.getId(), keeper.getMana(), keeper.getManaLoose(), keeper.getManaGain());
        }
    }

    public void addListener(PlayerManaListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlayerManaListener listener) {
        listeners.remove(listener);
    }
}
