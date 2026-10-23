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
    private final int maxManaGain;
    private final SafeArrayList<PlayerManaListener> listeners = new SafeArrayList<>(PlayerManaListener.class);

    public PlayerManaControl(Keeper keeper,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.keeper = keeper;

        this.keeper.setMaxMana((int) gameSettings.get(MiscType.MAXIMUM_MANA_THRESHOLD).getValue());
        this.maxManaGain = (int) gameSettings.get(MiscType.MAXIMUM_MANA_GAIN_PER_SECOND).getValue();
    }

    /**
     * Updates gain and loose and adds/removes mana accordingly to the player.
     * The gain is limited to the maximum mana gain per second, raised by the
     * owned tiles (mana vaults), and the loose is paid after the limit
     * (DKII.EXE 0x4C10E0; a tile changing owner moves its raise in 0x4559D0).
     * The mana spent at once since the last update leaves the loose here, like
     * DKII.EXE clears it before the update (0x4C10E0)
     *
     * @param gain mana gain
     * @param loose mana loose
     * @param maxGainIncrease how much the owned mana sources raise the gain
     * limit
     */
    public void updateMana(int gain, int loose, int maxGainIncrease) {

        int cappedGain = Math.min(gain, maxManaGain + maxGainIncrease);

        keeper.setManaGain(cappedGain);
        keeper.setManaLoose(loose);

        addMana(cappedGain - loose);

        updateListerners();
    }

    /**
     * Spends mana at once, like a spell that is cast or a trap that fires.
     * DKII.EXE adds it to the keeper's loss of the second it is spent in
     * (0x4C17A0), so it shows with the other losses until the next update
     *
     * @param amount the mana spent
     */
    public void spendMana(int amount) {
        addMana(-amount);
        keeper.setManaLoose(keeper.getManaLoose() + amount);

        updateListerners();
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
