/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopCameraCyclerTest {
    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final short TYPE = 1;
    private DefaultEntityData data;
    private WorkshopCameraCycler cycling;

    @BeforeEach
    void setUp() {
        data = new DefaultEntityData();
        // Rejects direct lookups and all client-side entity mutations.
        cycling = new WorkshopCameraCycler(new ClientEntityData(data), PLAYER);
    }

    @AfterEach
    void tearDown() {
        cycling.close();
        data.close();
    }

    @Test
    void cyclesBuiltDoorsAndBlueprintsByTypeAndWraps() {
        door(TYPE, PLAYER, false, 1);
        door(TYPE, ENEMY, false, 2);
        door((short) 2, PLAYER, false, 3);
        door(TYPE, PLAYER, true, 4);
        assertEquals(position(1), cycling.nextDoor(TYPE));
        assertEquals(position(4), cycling.nextDoor(TYPE));
        assertEquals(position(1), cycling.nextDoor(TYPE));
        assertEquals(position(3), cycling.nextDoor((short) 2));
        assertEquals(position(4), cycling.nextDoor(TYPE));
    }

    @Test
    void cyclesTrapsAndBlueprintsSeparatelyFromDoorsIncludingBarricades() {
        EntityId barricade = door(TYPE, PLAYER, true, 1);
        // Barricades have trap gameplay data as well as their door identity.
        data.setComponent(barricade, new TrapComponent(TYPE));
        trap(TYPE, PLAYER, false, 2);
        trap(TYPE, ENEMY, true, 3);
        trap(TYPE, PLAYER, true, 4);
        trap((short) 2, PLAYER, false, 5);
        assertEquals(position(1), cycling.nextDoor(TYPE));
        assertEquals(position(2), cycling.nextTrap(TYPE));
        assertEquals(position(4), cycling.nextTrap(TYPE));
        assertEquals(position(2), cycling.nextTrap(TYPE));
        assertEquals(position(5), cycling.nextTrap((short) 2));
        assertEquals(position(1), cycling.nextDoor(TYPE));
    }

    @Test
    void followsSalesRemovalCaptureAndPositionChangesBetweenClicks() {
        EntityId first = door(TYPE, PLAYER, false, 1);
        EntityId second = door(TYPE, PLAYER, true, 2);
        EntityId third = door(TYPE, PLAYER, false, 3);
        EntityId enemy = door(TYPE, ENEMY, false, 4);
        assertEquals(position(1), cycling.nextDoor(TYPE));
        assertEquals(position(2), cycling.nextDoor(TYPE));
        data.removeEntity(first); // Removing an earlier entry must not skip the next identity.
        data.removeEntity(second); // Includes selling the most recently visited instance.
        data.setComponent(third, new Position(0, position(7)));
        assertEquals(position(7), cycling.nextDoor(TYPE));
        data.setComponent(third, new Owner(ENEMY, PLAYER)); // Actual owner decides, not controlId.
        assertNull(cycling.nextDoor(TYPE));
        data.setComponent(enemy, new Owner(PLAYER, PLAYER));
        assertEquals(position(4), cycling.nextDoor(TYPE));
    }

    @Test
    void skipsRemovedTrapViewsAndUsesLatestBlueprintAndOwnerData() {
        EntityId first = trap(TYPE, PLAYER, true, 1);
        EntityId second = trap(TYPE, PLAYER, false, 2);
        assertEquals(position(1), cycling.nextTrap(TYPE));
        data.removeComponent(second, TrapViewState.class);
        data.setComponent(first, new TrapViewState(TYPE, false)); // Blueprint completes in place.
        assertEquals(position(1), cycling.nextTrap(TYPE));
        data.setComponent(first, new Owner(ENEMY, ENEMY));
        assertNull(cycling.nextTrap(TYPE));
        data.setComponent(first, new Owner(PLAYER, PLAYER));
        assertEquals(position(1), cycling.nextTrap(TYPE));
    }

    @Test
    void emptyOrIncompleteSetsDoNothingAndReturnedPositionsAreCopies() {
        assertNull(cycling.nextDoor(TYPE));
        assertNull(cycling.nextTrap(TYPE));
        var incomplete = data.createEntity();
        data.setComponents(incomplete, new DoorViewState(TYPE, false, true, false), new Owner(PLAYER, PLAYER));
        assertNull(cycling.nextDoor(TYPE));
        EntityId id = trap(TYPE, PLAYER, true, 3);
        cycling.nextTrap(TYPE).set(99, 99, 99);
        assertEquals(position(3), data.getComponent(id, Position.class).position);
        assertEquals(position(3), cycling.nextTrap(TYPE));
    }

    private EntityId door(short type, short owner, boolean blueprint, int x) {
        EntityId id = data.createEntity();
        data.setComponents(id, new DoorViewState(type, false, blueprint, false),
                new Owner(owner, owner), new Position(0, position(x)));
        return id;
    }

    private EntityId trap(short type, short owner, boolean blueprint, int x) {
        EntityId id = data.createEntity();
        data.setComponents(id, new TrapViewState(type, blueprint),
                new Owner(owner, owner), new Position(0, position(x)));
        return id;
    }

    private static Vector3f position(int x) {
        return new Vector3f(x, 1, x);
    }
}
