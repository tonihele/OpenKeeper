/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.logic;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import static org.junit.jupiter.api.Assertions.*;

class MovementSystemDoorsTest {
    @Test
    void aDoorBuiltOnAnOldEnemyRouteStopsAtItsTileForReplanning() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        var door = f.door((short) 1, 0, false, true, false);
        var creature = f.walker(4, 3, 1, DoorMovementFixtures.ENEMY, 4, 7);
        var movement = f.ai();
        f.tickAi(movement);
        f.data.setComponent(door, new DoorComponent((short) 1, false, false));
        for (int i = 0; i < 8; i++) f.tickAi(movement);
        assertTrue(f.position(creature).z < 3.5f);
        assertNull(f.data.getComponent(creature, Navigation.class));
    }

    @Test
    void lockingADoorInvalidatesItsOwnersOldRoute() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        var door = f.door((short) 1, 0, false, false, true);
        var creature = f.walker(4, 3, 1, DoorMovementFixtures.PLAYER, 4, 7);
        var movement = f.ai();
        f.data.setComponent(door, new DoorComponent((short) 1, true, false));
        f.open(door, false);
        for (int i = 0; i < 8; i++) f.tickAi(movement);
        assertTrue(f.position(creature).z < 3.5f);
        assertNull(f.data.getComponent(creature, Navigation.class));
    }

    @Test
    void friendlyTilePermissionDoesNotIntroduceASlabIntoWalking() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        f.door((short) 1, 0, false, false, false);
        var creature = f.walker(4, 3, 1, DoorMovementFixtures.PLAYER, 4, 7);
        var movement = f.ai();
        for (int i = 0; i < 16; i++) f.tickAi(movement);
        assertTrue(f.position(creature).z > 6);
    }
}
