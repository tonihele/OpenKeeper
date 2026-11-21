/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.listener;

import com.jme3.network.service.rmi.Asynchronous;

/**
 * Listen to the changes in the player's statistics shown on the Player
 * Information window
 */
public interface PlayerStatsListener {

    /**
     * The keeper's kills, losses or research changed
     *
     * @param keeperId the keeper
     * @param enemyCreaturesKilled the enemy creatures, not heroes, the keeper
     * has killed
     * @param heroesDestroyed the heroes the keeper has killed
     * @param ownCreaturesKilled the keeper's own creatures that have died
     * @param researchCompleted the research items the keeper has finished
     */
    @Asynchronous
    public void onStatsChange(short keeperId, int enemyCreaturesKilled, int heroesDestroyed, int ownCreaturesKilled,
            int researchCompleted);

}
