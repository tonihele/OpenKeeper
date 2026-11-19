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
package toniarts.openkeeper.game.state;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.ClientEntityData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoomCyclingTest {
    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final short LAIR = 1;
    private static final short TREASURY = 2;
    private DefaultEntityData data;
    private RoomCycling cycling;

    @BeforeEach
    void setUp() {
        data = new DefaultEntityData();
        // This facade rejects synchronous component lookups, as the real client does.
        cycling = new RoomCycling(new ClientEntityData(data), PLAYER);
    }

    @AfterEach
    void tearDown() {
        cycling.close();
        data.close();
    }

    @Test
    void wrapsThroughOwnedRoomsOfTheSelectedType() {
        room(LAIR, PLAYER, new Point(1, 1));
        room(LAIR, ENEMY, new Point(2, 2));
        room(TREASURY, PLAYER, new Point(3, 3));
        room(LAIR, PLAYER, new Point(4, 4));
        assertEquals(new Point(1, 1), cycling.next(LAIR));
        assertEquals(new Point(4, 4), cycling.next(LAIR));
        assertEquals(new Point(1, 1), cycling.next(LAIR));
        assertEquals(new Point(3, 3), cycling.next(TREASURY));
        assertEquals(new Point(1, 1), cycling.next(LAIR));
    }

    @Test
    void followsRemovalDestructionAndCaptureBetweenClicks() {
        EntityId first = room(LAIR, PLAYER, new Point(1, 1));
        EntityId second = room(LAIR, PLAYER, new Point(2, 2));
        EntityId enemy = room(LAIR, ENEMY, new Point(3, 3));
        assertEquals(new Point(1, 1), cycling.next(LAIR));
        data.removeEntity(second);
        assertEquals(new Point(1, 1), cycling.next(LAIR));
        data.setComponent(enemy, new Owner(PLAYER, PLAYER));
        assertEquals(new Point(3, 3), cycling.next(LAIR));
        data.setComponent(first, new RoomComponent(LAIR, true, new Point(1, 1)));
        data.setComponent(enemy, new Owner(ENEMY, ENEMY));
        assertNull(cycling.next(LAIR));
        room(LAIR, PLAYER, new Point(4, 4));
        assertEquals(new Point(4, 4), cycling.next(LAIR));
    }

    @Test
    void keepsTheCursorOnRoomIdentityWhenOtherRoomsChange() {
        EntityId first = room(LAIR, PLAYER, new Point(1, 1));
        room(LAIR, PLAYER, new Point(2, 2));
        EntityId third = room(LAIR, PLAYER, new Point(3, 3));
        cycling.next(LAIR);
        assertEquals(new Point(2, 2), cycling.next(LAIR));
        data.removeEntity(first);
        data.setComponent(third, new RoomComponent(LAIR, false, new Point(7, 7)));
        assertEquals(new Point(7, 7), cycling.next(LAIR));
    }

    @Test
    void ignoresMissingLocationsAndDoesNothingAfterCleanup() {
        room(LAIR, PLAYER, null);
        assertNull(cycling.next(LAIR));
        room(LAIR, PLAYER, new Point(1, 1));
        cycling.close();
        cycling.close();
        assertNull(cycling.next(LAIR));
    }

    private EntityId room(short type, short owner, Point location) {
        EntityId entity = data.createEntity();
        data.setComponents(entity, new RoomComponent(type, false, location), new Owner(owner, owner));
        return entity;
    }
}
