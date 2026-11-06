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
package toniarts.openkeeper.view.control;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Turncoat;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The colour a creature's flower is drawn in
 */
class CreatureFlowerControlTest {

    private static final short OWNER = Player.KEEPER1_ID;
    private static final short CASTER = Player.KEEPER2_ID;

    @Test
    void creatureWithoutTurncoatShowsItsOwner() {
        assertEquals(OWNER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, null, 0));
        assertEquals(OWNER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, null, 333));
    }

    @Test
    void turncoatAlternatesBetweenOwnerAndCasterEvery333Milliseconds() {
        Turncoat turncoat = new Turncoat(CASTER, 100);

        assertEquals(OWNER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, turncoat, 0));
        assertEquals(OWNER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, turncoat, 332));
        assertEquals(CASTER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, turncoat, 333));
        assertEquals(CASTER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, turncoat, 665));
        assertEquals(OWNER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, turncoat, 666));
        assertEquals(CASTER, CreatureFlowerControl.getFlowerColorPlayerId(OWNER, turncoat, 1000));
    }

    @Test
    void neutralCreatureKeepsTheNeutralColors() {
        Turncoat turncoat = new Turncoat(CASTER, 100);

        assertEquals(Player.NEUTRAL_PLAYER_ID,
                CreatureFlowerControl.getFlowerColorPlayerId(Player.NEUTRAL_PLAYER_ID, turncoat, 333));
    }

}
