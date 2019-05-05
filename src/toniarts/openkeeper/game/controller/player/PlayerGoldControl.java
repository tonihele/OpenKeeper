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
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerGoldListener;

/**
 *
 * @author ArchDemon
 */
public class PlayerGoldControl {

    private int goldMax = 0;
    private final Keeper keeper;
    private final SafeArrayList<PlayerGoldListener> listeners = new SafeArrayList<>(PlayerGoldListener.class);

    public PlayerGoldControl(Keeper keeper) {
        this.keeper = keeper;
    }

    public void addGold(int value) {
        if (value < 0) {
            return;
        }

        keeper.setGold(Math.min(keeper.getGold() + value, goldMax));
        keeper.setGoldMined(keeper.getGoldMined() + value); // Nope, need to get this some other way
        updateListeners();
    }

    public boolean subGold(int value) {
        if (keeper.getGold() >= value && value > 0) {
            keeper.setGold(keeper.getGold() - value);
            updateListeners();
            return true;
        }
        return false;
    }

    public void addListener(PlayerGoldListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlayerGoldListener listener) {
        listeners.remove(listener);
    }

    private void updateListeners() {
        for (PlayerGoldListener listener : listeners.getArray()) {
            listener.onGoldChange(keeper.getId(), keeper.getGold());
        }
    }

    public void setGoldMax(int goldMax) {
        this.goldMax = goldMax;
    }

    public int getGoldMax() {
        return goldMax;
    }

    public boolean isFullCapacity() {
        return keeper.getGold() >= goldMax;
    }
}
