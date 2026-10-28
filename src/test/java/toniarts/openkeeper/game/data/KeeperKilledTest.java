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
package toniarts.openkeeper.game.data;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PLAYER_KILLED trigger asks whether a keeper has been killed, by anyone
 * (0) or by a given player
 */
class KeeperKilledTest {

    private static final short ANYONE = 0;

    @Test
    void keeperStillInTheGameIsNotKilled() {
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);

        assertFalse(keeper.isKilledBy(ANYONE));
        assertFalse(keeper.isKilledBy(Player.GOOD_PLAYER_ID));
    }

    @Test
    void keeperKilledByTheHeroesIsKilledByThemAndByAnyone() {
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);

        keeper.setDestroyed(Player.GOOD_PLAYER_ID);

        assertTrue(keeper.isDestroyed());
        assertTrue(keeper.isKilledBy(ANYONE));
        assertTrue(keeper.isKilledBy(Player.GOOD_PLAYER_ID));
        assertFalse(keeper.isKilledBy(Player.KEEPER2_ID));
    }

    @Test
    void keeperKilledByNobodyKnownIsKilledByAnyoneOnly() {
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);

        keeper.setDestroyed(ANYONE);

        assertTrue(keeper.isKilledBy(ANYONE));
        assertFalse(keeper.isKilledBy(Player.KEEPER2_ID));
    }

    @Test
    void resultOfAKeeperCanBeSetOnlyOnce() {
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);

        assertTrue(keeper.claimResult());
        assertFalse(keeper.claimResult());
    }

}
