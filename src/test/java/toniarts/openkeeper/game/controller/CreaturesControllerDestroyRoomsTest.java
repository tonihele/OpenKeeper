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

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DESTROY_ROOMS flag of a hero, on its own or in a hero party, is kept on
 * the creature (DKII.EXE 0x48E090 copies bit 0 of the second flags)
 */
class CreaturesControllerDestroyRoomsTest {

    @Test
    void heroWithTheFlagDestroysRooms() {
        assertTrue(CreaturesController.isRoomDestroyer(DestroyRoomsFixtures.destroyRoomsHero(
                EnumSet.of(Thing.Creature.CreatureFlag2.DESTROY_ROOMS))));
        assertTrue(CreaturesController.isRoomDestroyer(DestroyRoomsFixtures.destroyRoomsHero(
                EnumSet.of(Thing.Creature.CreatureFlag2.DESTROY_ROOMS, Thing.Creature.CreatureFlag2.DIES_INSTANTLY))));
    }

    @Test
    void heroWithoutTheFlagDoesNot() {
        assertFalse(CreaturesController.isRoomDestroyer(DestroyRoomsFixtures.destroyRoomsHero(
                EnumSet.of(Thing.Creature.CreatureFlag2.DIES_INSTANTLY))));
        assertFalse(CreaturesController.isRoomDestroyer(DestroyRoomsFixtures.destroyRoomsHero(null)));
    }

}
