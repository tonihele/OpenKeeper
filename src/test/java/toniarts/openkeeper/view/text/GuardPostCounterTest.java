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
package toniarts.openkeeper.view.text;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;

/**
 * Tests counting the Guard Posts, i.e. the patrol routes, of a player
 */
class GuardPostCounterTest {

    private static final short GUARD_POST = 11;
    private static final short FIRE_TRAP = 3;

    @Test
    void countsOnlyTheOwnersGuardPosts() {
        EntityData entityData = new DefaultEntityData();
        addTrap(entityData, GUARD_POST, 1);
        addTrap(entityData, GUARD_POST, 1);
        addTrap(entityData, GUARD_POST, 2);
        addTrap(entityData, FIRE_TRAP, 1);

        GuardPostCounter counter = new GuardPostCounter(entityData, trapId -> trapId == GUARD_POST);
        try {
            assertEquals(2, counter.applyAsInt((short) 1));
            assertEquals(1, counter.applyAsInt((short) 2));
            assertEquals(0, counter.applyAsInt((short) 3));
        } finally {
            counter.close();
        }
    }

    @Test
    void followsGuardPostsBeingAddedAndRemoved() {
        EntityData entityData = new DefaultEntityData();
        GuardPostCounter counter = new GuardPostCounter(entityData, trapId -> trapId == GUARD_POST);
        try {
            assertEquals(0, counter.applyAsInt((short) 1));

            EntityId post = addTrap(entityData, GUARD_POST, 1);
            assertEquals(1, counter.applyAsInt((short) 1));

            entityData.removeEntity(post);
            assertEquals(0, counter.applyAsInt((short) 1));
        } finally {
            counter.close();
        }
    }

    private static EntityId addTrap(EntityData entityData, short trapId, int owner) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id, new TrapComponent(trapId), new Owner((short) owner, (short) owner));
        return id;
    }

}
