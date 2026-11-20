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
package toniarts.openkeeper.game.task.worker;

import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The owner of a room is told the moment an enemy Imp starts taking the room
 * over (DKII.EXE 0x466080, message 0x22 with the room type: speech 158 for the
 * Lair). The message queue holds it for 30 seconds
 */
class ClaimRoomTaskTest {

    private final RoomTileWorld world = new RoomTileWorld();
    private final ICreatureController imp = RoomTileWorld.creature(Player.KEEPER2_ID);
    private final ClaimRoomTask task = new ClaimRoomTask(null, world.mapController, world.gameWorldController,
            RoomTileWorld.TILE, Player.KEEPER2_ID);

    @Test
    void theOwnerIsToldWhenTheImpStartsOnTheRoom() {
        task.assign(imp, false);

        task.executeTask(imp, 0.25f);

        assertEquals(List.of(158), world.mentorMessages);
    }

    @Test
    void theImpsThatFollowDoNotRepeatIt() {
        ICreatureController secondImp = RoomTileWorld.creature(Player.KEEPER2_ID);
        task.assign(imp, false);
        task.assign(secondImp, false);

        task.executeTask(imp, 0.25f);
        task.executeTask(imp, 1.25f);
        task.executeTask(imp, 2.5f);
        task.executeTask(secondImp, 0.25f);

        assertEquals(List.of(158), world.mentorMessages);
    }

}
