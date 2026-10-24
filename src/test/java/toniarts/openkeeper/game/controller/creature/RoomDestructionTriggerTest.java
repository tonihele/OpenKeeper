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
package toniarts.openkeeper.game.controller.creature;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DKII.EXE checks on every creature update (0x488930) whether a hero with the
 * DESTROY_ROOMS flag has just stepped onto a new tile while he is free to take
 * on work (0x45A6D0), and only then looks at the room under him
 */
class RoomDestructionTriggerTest {

    private static final Point START = new Point(2, 2);
    private static final Point NEXT = new Point(3, 2);

    private final RoomDestructionTrigger trigger = new RoomDestructionTrigger();

    @Test
    void checksEachNewTileTheHeroStepsOn() {
        assertNull(walk(START, CreatureState.WORK));

        assertEquals(NEXT, walk(NEXT, CreatureState.WORK));
        assertNull(walk(NEXT, CreatureState.WORK));
        assertEquals(START, walk(START, CreatureState.WORK));
    }

    @Test
    void checksWhileIdleFollowingOrWorking() {
        walk(START, CreatureState.IDLE);
        assertEquals(NEXT, walk(NEXT, CreatureState.IDLE));
        assertEquals(START, walk(START, CreatureState.FOLLOW));
    }

    @Test
    void doesNotCheckWhileBusyWithSomethingElse() {
        walk(START, CreatureState.FIGHT);
        assertNull(walk(NEXT, CreatureState.FIGHT));
        assertNull(walk(START, CreatureState.FLEE));
        assertNull(walk(NEXT, CreatureState.PICKED_UP));

        // The tile is noted all the same, the hero has to step on a new one
        assertNull(walk(NEXT, CreatureState.WORK));
    }

    @Test
    void onlyHeroesWithTheFlagDestroyRooms() {
        trigger.onTheWay(START, CreatureState.WORK, false, Player.GOOD_PLAYER_ID);
        assertNull(trigger.onTheWay(NEXT, CreatureState.WORK, false, Player.GOOD_PLAYER_ID));
        assertNull(trigger.onTheWay(START, CreatureState.WORK, true, Player.KEEPER1_ID));
    }

    private Point walk(Point tile, CreatureState state) {
        return trigger.onTheWay(tile, state, true, Player.GOOD_PLAYER_ID);
    }

}
