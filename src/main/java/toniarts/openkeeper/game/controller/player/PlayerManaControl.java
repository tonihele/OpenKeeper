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
import java.util.Map;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerManaListener;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * Controller of player mana
 *
 * @author ArchDemon
 */
public final class PlayerManaControl {

    private final Keeper keeper;
    private final SafeArrayList<PlayerManaListener> listeners = new SafeArrayList<>(PlayerManaListener.class);
    private int manaSpent;

    public PlayerManaControl(Keeper keeper,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.keeper = keeper;

        this.keeper.setMaxMana((int) gameSettings.get(MiscType.MAXIMUM_MANA_THRESHOLD).getValue());
    }

    /**
     * Updates gain and loose and adds/removes mana accordingly to the player
     *
     * @param gain mana gain
     * @param loose mana loose
     */
    public void updateMana(int gain, int loose) {

        // The mana spent at once this second is gone already, it only shows with the loss
        keeper.setManaGain(gain);
        keeper.setManaLoose(loose + manaSpent);
        manaSpent = 0;

        addMana(gain - loose);

        updateListerners();
    }

    /**
     * Spends mana at once, like a spell that is cast. DKII.EXE adds it to the
     * keeper's loss of the second it is spent in (0x4C17A0), so it shows with
     * the other losses
     *
     * @param amount the mana spent
     */
    public void spendMana(int amount) {
        addMana(-amount);
        manaSpent += amount;
    }

    public void addMana(int value) {
        value = Math.max(0, keeper.getMana() + value);
        keeper.setMana(Math.min(value, keeper.getMaxMana()));
    }

    private void updateListerners() {
        for (PlayerManaListener listener : listeners.getArray()) {
            listener.onManaChange(keeper.getId(), keeper.getMana(), keeper.getManaLoose(), keeper.getManaGain());
        }
    }

    public void addListener(PlayerManaListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlayerManaListener listener) {
        listeners.remove(listener);
    }

    public boolean hasEnoughMana(int manaDrain) {
        return keeper.getMana() - manaDrain + keeper.getManaGain() - keeper.getManaLoose() > 0;
    }
}
