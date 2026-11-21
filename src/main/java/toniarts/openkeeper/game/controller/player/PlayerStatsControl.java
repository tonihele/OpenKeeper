/*
 * Copyright (C) 2014-2016 OpenKeeper
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
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerStatsListener;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * A class to hold player's miscellaneous statistics
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerStatsControl {

    private int payDays;
    private final Map<Creature, Integer> slapsMap = new HashMap<>();
    private final Map<Creature, Integer> pickUpsMap = new HashMap<>();
    private final Map<Creature, Integer> dropsMap = new HashMap<>();
    private final Keeper keeper;
    private final SafeArrayList<PlayerStatsListener> listeners = new SafeArrayList<>(PlayerStatsListener.class);

    public PlayerStatsControl(Keeper keeper) {
        this.keeper = keeper;
    }

    /**
     * The player has killed a creature, DK II counts the heroes apart from the
     * other creatures (DKII.EXE 0x487C70, CPlayer +0x14A and +0x146)
     *
     * @param creature the creature that was killed
     */
    public void creatureKilled(Creature creature) {
        if (creature.getFlags().contains(Creature.CreatureFlag.IS_GOOD)) {
            keeper.setHeroesDestroyed(keeper.getHeroesDestroyed() + 1);
        } else {
            keeper.setEnemyCreaturesKilled(keeper.getEnemyCreaturesKilled() + 1);
        }
        updateListeners();
    }

    /**
     * One of the player's own creatures has died (DKII.EXE 0x487C70, CPlayer
     * +0x173)
     */
    public void ownCreatureKilled() {
        keeper.setOwnCreaturesKilled(keeper.getOwnCreaturesKilled() + 1);
        updateListeners();
    }

    /**
     * The player has finished a research item, its book has found room in a
     * Library (DKII.EXE 0x4C2DC0, CPlayer +0x17B)
     */
    public void researchCompleted() {
        keeper.setResearchCompleted(keeper.getResearchCompleted() + 1);
        updateListeners();
    }

    public void addListener(PlayerStatsListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlayerStatsListener listener) {
        listeners.remove(listener);
    }

    private void updateListeners() {
        for (PlayerStatsListener listener : listeners.getArray()) {
            listener.onStatsChange(keeper.getId(), keeper.getEnemyCreaturesKilled(), keeper.getHeroesDestroyed(),
                    keeper.getOwnCreaturesKilled(), keeper.getResearchCompleted());
        }
    }

    public boolean hasSlapped() {
        return !slapsMap.isEmpty();
    }

    public boolean hasSlapped(Creature creature) {
        return slapsMap.containsKey(creature);
    }

    /**
     * Counts a pay day of the player
     */
    public void payDay() {
        payDays++;
    }

    /**
     * Get how many pay days the player has had
     *
     * @return the pay days
     */
    public int getPayDays() {
        return payDays;
    }

    public void creatureSlapped(Creature creature) {
        increaseMapCount(creature, slapsMap);
    }

    private void increaseMapCount(Creature creature, Map<Creature, Integer> map) {
        Integer count = map.get(creature);
        if (count == null) {
            count = 0;
        }
        count++;
        map.put(creature, count);
    }

    public boolean hasPickedUp() {
        return !pickUpsMap.isEmpty();
    }

    public boolean hasPickedUp(Creature creature) {
        return pickUpsMap.containsKey(creature);
    }

    public void creaturePickedUp(Creature creature) {
        increaseMapCount(creature, pickUpsMap);
    }

    public boolean hasDropped() {
        return !dropsMap.isEmpty();
    }

    public boolean hasDropped(Creature creature) {
        return dropsMap.containsKey(creature);
    }

    public void creatureDropped(Creature creature) {
        increaseMapCount(creature, dropsMap);
    }

}
