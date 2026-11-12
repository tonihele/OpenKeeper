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
import toniarts.openkeeper.view.ClientEntityData;

/**
 * Tests counting the Guard Posts, i.e. the patrol routes, of one player. The
 * counter gets the client's entity data, which refuses entity lookups, so the
 * tests fail if it ever asks the server for a single entity.
 */
class GuardPostCounterTest {

    private static final short GUARD_POST = 11;
    private static final short FIRE_TRAP = 3;

    @Test
    void countsOnlyThePlayersGuardPosts() {
        EntityData entityData = new DefaultEntityData();
        addTrap(entityData, GUARD_POST, 1);
        addTrap(entityData, GUARD_POST, 1);
        addTrap(entityData, GUARD_POST, 2);
        addTrap(entityData, FIRE_TRAP, 1);

        GuardPostCounter counter = new GuardPostCounter(new ClientEntityData(entityData), (short) 1, trapId -> trapId == GUARD_POST);
        try {
            counter.update(0);
            assertEquals(2, counter.getAsInt());
        } finally {
            counter.cleanup();
        }
    }

    @Test
    void followsGuardPostsBeingAddedAndRemoved() {
        EntityData entityData = new DefaultEntityData();
        GuardPostCounter counter = new GuardPostCounter(new ClientEntityData(entityData), (short) 1, trapId -> trapId == GUARD_POST);
        try {
            assertEquals(0, counter.getAsInt());

            EntityId post = addTrap(entityData, GUARD_POST, 1);
            assertEquals(0, counter.getAsInt(), "The count only follows the set when it is updated");
            counter.update(0);
            assertEquals(1, counter.getAsInt());

            entityData.removeEntity(post);
            counter.update(0);
            assertEquals(0, counter.getAsInt());
        } finally {
            counter.cleanup();
        }
    }

    private static EntityId addTrap(EntityData entityData, short trapId, int owner) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id, new TrapComponent(trapId), new Owner((short) owner, (short) owner));
        return id;
    }

}
