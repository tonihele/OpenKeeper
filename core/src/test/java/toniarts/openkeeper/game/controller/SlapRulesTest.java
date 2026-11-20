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
package toniarts.openkeeper.game.controller;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlapRulesTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;

    @Test
    void ownCreatureCanBeSlapped() {
        assertTrue(SlapRules.canSlapCreature(PLAYER, PLAYER, PLAYER, false));
    }

    @Test
    void freeEnemyCreatureCannotBeSlapped() {

        // Wherever it stands, also on our own land
        assertFalse(SlapRules.canSlapCreature(PLAYER, ENEMY, ENEMY, false));
        assertFalse(SlapRules.canSlapCreature(PLAYER, Player.GOOD_PLAYER_ID, Player.GOOD_PLAYER_ID, false));
    }

    @Test
    void enemyCreatureInOurPrisonCanBeSlapped() {
        assertTrue(SlapRules.canSlapCreature(PLAYER, ENEMY, PLAYER, true));
        assertTrue(SlapRules.canSlapCreature(PLAYER, Player.GOOD_PLAYER_ID, PLAYER, true));
    }

    @Test
    void nothingInSomeoneElsesPrisonCanBeSlapped() {
        assertFalse(SlapRules.canSlapCreature(PLAYER, Player.GOOD_PLAYER_ID, ENEMY, true));
        assertFalse(SlapRules.canSlapCreature(PLAYER, Player.KEEPER3_ID, ENEMY, true));

        // Not even our own creature held there
        assertFalse(SlapRules.canSlapCreature(PLAYER, PLAYER, ENEMY, true));
    }

    @Test
    void ourCreatureInSomeoneElsesPrisonCanBeSlappedByTheJailer() {
        assertTrue(SlapRules.canSlapCreature(ENEMY, PLAYER, ENEMY, true));
    }

}
